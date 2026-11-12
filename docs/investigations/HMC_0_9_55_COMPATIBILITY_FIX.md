# HyperModifier 1.5.3 与 HMC 0.9.55 兼容修复

## 结论

故障位于 **HyperModifier 的 HMC 私有接口适配层**。HMC 的设置存储和时钟内部实现已经变化，HyperModifier 1.5.3 的 DEX 解析器仍假定旧结构。现有证据没有表明 HMC 自身的指纹或通知功能注入失败。

分析基线：HyperModifier `v1.5.3`（`5cd969a5eb1b0746b672bb508973693a67d21faa`），HMC `v0.9.55`（`72357346991bc9748b0fb4b7b29f07922f328ed1`）。官方 APK 的 SHA-256 已与 GitHub release asset digest 核对。

用户日志重复出现：

```text
WARN com.android.systemui HMC 新版接口解析:
IOException: HMC fingerprint avoidance: expected one match, found 0
```

用原版生产解析器读取官方 HMC v0.9.50、v0.9.55 APK，均复现相同异常，位置为原版 `HyperMusicCoverDexSymbols.resolve()` 第 79 行。

## 失败链路

1. 原解析器在 `Main.lambda$static$…` 中，用 `notification bound mode=` 日志定位通知策略读取，随后只查找 **static int** 字段。
2. [HMC v0.9.55 Main](https://github.com/zyl6932/HyperMusicCover/blob/v0.9.55/app/src/main/java/com/os4/musiccover/Main.java) 改为 `Settings.FP_AVOID.get()`；[Settings](https://github.com/zyl6932/HyperMusicCover/blob/v0.9.55/app/src/main/java/com/os4/musiccover/Settings.java) 将实时值存于设置对象的 **volatile 实例字段**。正式版 R8 内联 getter，实际路径是 `sget-object Luj2;->B:Lij2;` 后 `iget Lij2;->i:I`，没有旧 static int，故匹配数为 0。
3. `HyperMusicCoverClockHooks.prepareSymbols()` 捕获异常后设置 `bindingFailed = true`，本次 SystemUI 生命周期内不再完成绑定，也不会进入 `finishBind()` 调用扩展绑定。
4. `HyperMusicCoverExtensions.adapter` 未创建，`bound()` 为 false，`refreshNow()` 直接返回。通知 Flow 的 Hook 虽然已安装，但其扩展分支被 `bound()` 门槛挡住；指纹绘制 Hook 则要等 `bind()` 才安装。

因此日志里的“模块注入”“HMC 展开通知下沉：Hook 已安装”只说明前置 Hook 安装成功，不能证明 HMC 接口已绑定或功能实际生效。

## 被首个异常遮住的后续变化

- `HIDE_FP` 同样迁入 `Settings.Bool`，旧日志锚点 `fingerprint setting read early: hide=` 不再是可用的定位依据。
- [Fingerprint.applyHideFp](https://github.com/zyl6932/HyperMusicCover/blob/v0.9.55/app/src/main/java/com/os4/musiccover/fingerprint/Fingerprint.java) 使用 `View.postInvalidate()`，旧解析只认 `invalidate()`。
- [ClockRoom.roomForRows](https://github.com/zyl6932/HyperMusicCover/blob/v0.9.55/app/src/main/java/com/os4/musiccover/clock/ClockRoom.java) 增加 `spreadClockY(float)`，与 `roomForRowsNow(float)` 签名相同。只修指纹读取后，正式 APK 继续报 `HMC roomForRowsNow: expected one match, found 2`。
- `roomForRows` 还写入两个展开端点，旧“恰好三个浮点状态字段”假设不再成立。R8 提取的 `resetClockRoom` 仍按 raw、shown、soft 写入三个缓动字段，可据此区分，避免将展开端点误当成缓动状态。

## 修改

- DEX 读取增加实例字段指令；从实际调用中的静态设置对象及其对应实例值解析 `FP_AVOID` / `HIDE_FP`。不硬编码 R8 混淆类名。
- 用 FOD 的 `mDozing`、`mKeyguardAuthen` 语义锚点定位认证范围判断，再定位读取设置、重绘指纹视图的方法，同时接受同步/异步重绘。
- 设置访问缓存 `Field` 元数据，读取当时的设置对象及 volatile 值，不缓存用户设置值，不改写 HMC 设置或状态文件。保留旧 static 字段路径。
- 时钟候选通过是否写入 `sRoomUnobstructed` 区分；从重置方法识别新增展开端点之外的三个缓动字段。
- 仍要求唯一匹配及正确类型、静态/实例属性。设置实例值要求 volatile、非 final；有歧义时拒绝绑定。

## 验证

| 官方 HMC APK | 原版解析器 | 修复后解析器 | SHA-256 |
| --- | --- | --- | --- |
| v0.8.1 | 通过，13 项 | 通过，13 项 | `88f4e14a35cbda80125ac0037a9502766d3707436e043b50d158900b6501145b` |
| v0.9.0 | 通过，13 项 | 通过，13 项 | `fa3b425f8843533a4336f7d9cf78ceaff4746b13c823a65ca79c814a2e919a5d` |
| v0.9.50 | 指纹避让匹配 0 | 通过，15 项 | `65b8e19763514752223d76f09c509051ef32433f215aecfeffc9f7b24d69d944` |
| v0.9.55 | 指纹避让匹配 0 | 通过，15 项 | `85249b045f8720800547cc78f17fc89ad1170a3e906f88e5d341c3d166f0ca52` |

新增两项为设置对象的持有字段；原有 13 项逻辑角色保留。v0.9.55 解析结果：

```text
fpAvoidEntry = Luj2;->B:Lij2;          fpAvoid = Lij2;->i:I
hideFpEntry  = Luj2;->A:Lcj2;          hideFp  = Lcj2;->i:Z
applyHide    = Lyf0;->b()V             fodForKeyguard = Lyf0;->e(Ljava/lang/Object;)Z
roomForRows  = Lf32;->A0(F)F          roomForRowsNow = Lf32;->B0(F)F
roomRaw      = Lf32;->n:F            roomShown = Lf32;->m:F
roomSoft     = Lf32;->l:F            roomEasing = Lf32;->q:Z
reassertClockRoom = Lf32;->w0()V
```

JDK 21 / Java 17 目标、Android 37 的 android.jar、JUnit 4.13.2 下，9 个 HMC 测试类合计 **53 项通过**。覆盖旧版与拆分类适配、新版设置实时变化和只读性、拒绝默认值/错误持有对象、全屏/列表/通知中心场景以及通知输入恢复。官方 APK 的静态解析与这些 JVM 单测不等于 ART Hook 或真机画面验证。

复核正式 APK：`python tools/verify-hmc-release.py /path/to/HyperMusicCover.apk`。

对 v0.9.55 的哈希和全部 15 项实际成员进行精确回归检查（已通过）：

```sh
python tools/verify-hmc-release.py /path/to/HyperMusicCover-v0.9.55.apk --expectations tools/tests/hmc-v0.9.55-symbols.properties
```

完整工程也已执行 `:app:testDebugUnitTest --tests 'com.aritxonly.myhypermodifier.HyperMusicCover*' :app:assembleRelease`：**BUILD SUCCESSFUL**，Gradle 测试报告同样为 53 项通过、0 失败、0 跳过；arm64 原生库、R8 和 Release lintVital 均通过。Windows 使用工作区内的独立 ASCII 构建目录，451 个 `app/src` 文件与修复工作区逐一比对一致。

测试 APK：`HyperModifier-v1.5.3-hmcfix.1-test.apk`，保留应用 ID、versionName `1.5.3`、versionCode `41`，采用本地 RSA 测试签名。`apksigner verify`、16 KB ZIP 对齐检查通过；包含 LSPosed 入口及 arm64 原生库。

APK SHA-256：`6e578964a443b14b7640f201b12836e2a5b3bf5441709212e6d6cfd42f7aa826`。

该签名与官方不同，正常安装流程不能直接覆盖官方版。应先导出 HyperModifier 配置，再卸载原 HyperModifier、安装测试版、恢复配置并在 LSPosed 重新启用及确认作用域。无需更换官方 HMC v0.9.55。

## 性能范围

代码审查确认 DEX 解析仍只在原有后台线程执行一次，新版额外扫描方法以定位指纹语义锚点；未增加主线程 APK 读取、轮询、IPC、渲染层或动画回调。热路径只多一次缓存 Field 对设置对象的读取，并实时读取其 volatile 值。桌面解析耗时不代表手机耗时，未作真机帧率或耗电改善声明。

## 真机验收与扩展回归清单

1. 保存 HyperModifier 配置后安装修复测试包，在 LSPosed 中启用并重启系统界面/手机，使原有 `bindingFailed` 清除及通知 Flow 重新收集。
2. HMC 指纹避让设为“跟随系统”；关闭两模块各自的常驻“隐藏指纹”和全局通知下沉，仅启用待测的全屏自动功能。
3. 查看新时间戳日志：`HMC 新版接口解析` 不再报错，`HyperMusicCover 扩展` 完成绑定；不能只看前置通知 Hook 的安装记录。
4. 全屏岛展开时通知下沉、指纹图标和动画隐藏；收起时恢复。验证音乐岛及其他 HMC 全屏岛。
5. 验证全部卡片 LIST 展开、通知中心、控制中心、锁屏编辑、解锁的恢复；验证 AOD、快速反复展开收起。
6. 应用内支付/应用锁指纹不能被隐藏。HMC 手动指纹/避让设置实时修改仍应生效，两个模块各自的手动隐藏优先级保持原有行为。

用户已对 `HyperModifier-v1.5.3-hmcfix.1-test.apk` 反馈“已恢复，展开和收起都正常”，确认本次目标功能的真机验收通过。该反馈不代表上述 AOD、应用内认证等扩展场景已逐项测试，也不是 v1.6.0 的独立真机测试记录。
