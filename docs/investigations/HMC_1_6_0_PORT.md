# v1.6.0：合入 HMC 新版接口兼容修复

上游基线：`v1.6.0`，提交 `c733d97fc29b0e6f46a3675dce571381c0f7aa4e`。
旧修复基线：`v1.5.3`，提交 `5cd969a5eb1b0746b672bb508973693a67d21faa`。

## 上游差异结论

逐文件比较 Git blob 后确认，下列故障链路相关文件在两个正式版本间完全相同：

- `HyperMusicCoverDexSymbols.java`：DEX 解析器仍假定指纹设置是 static int / boolean。
- `HyperMusicCoverSymbols.java`、`HyperMusicCoverAdapter.java`：仍只支持旧的静态设置字段。
- `HyperMusicCoverClockHooks.java`：解析失败后仍设置 `bindingFailed`，阻断后续绑定。
- `HyperMusicCoverNotificationFlow.java`、`HyperMusicCoverExtensionPolicy.java`：通知 Flow 与展开策略未改动。

因此 v1.6.0 没有修复 `HMC fingerprint avoidance: expected one match, found 0`。

HMC 周边代码有改动：上游删除 `HyperMusicCoverClockExit`，移除 `ClockFramePolicy.ExitPose` 和解锁退出姿态保持；`HyperMusicCoverFontHeight` 与 `HyperMusicCoverExtensions` 同步移除退出阶段的字体状态保持。另外新增锁屏快捷入口的 HMC 材质调用桥接。这些不是新版设置字段解析的修复。

## 合并范围

从旧修复移植 DEX 实例字段解析、实时只读设置访问、`postInvalidate()` 识别，以及新版时钟避让分支/状态字段识别。具体原因和算法见 [HMC 兼容修复分析](HMC_0_9_55_COMPATIBILITY_FIX.md)。

补丁在 v1.6.0 上无冲突应用。`HyperMusicCoverExtensions` 相对 v1.6.0 只改变两处指纹设置读取；上游删除的时钟退出类未恢复，新的锁屏快捷入口材质桥接未修改。应用版本保持 `1.6.0` / `10600`。

新增测试覆盖设置对象实时变更、拒绝默认值和错误持有对象，并保留固定 SHA-256 的 HMC v0.9.55 全部 15 项成员回归检查。

## 性能检查

移植没有新增运行路径：DEX 解析仍在原有后台线程执行一次；热路径复用 Field 元数据，读取当前设置对象及其 volatile 值。没有新增主线程磁盘操作、IPC、轮询或渲染层。保留 v1.6.0 移除退出姿态保持 Hook 的调整。未进行新版真机性能对照，不宣称帧率或耗电改善。

## 真机证据范围

用户对旧 `1.5.3-hmcfix.1` 测试包反馈“已恢复，展开和收起都正常”，已验证原功能修复。该反馈不能当作新 `1.6.0-hmcfix.1` 构建或所有扩展场景的独立真机验证。

## 本轮验证结果（2026-11-12）

- HMC 官方 v0.8.1、v0.9.0、v0.9.50、v0.9.55 APK 均通过生产解析器检查。
- v0.9.55 APK 的 SHA-256 和全部 15 项成员与固定回归基准一致。
- `:app:testDebugUnitTest`：645 项通过，0 失败、0 错误、0 跳过。
- `:app:assembleRelease`：成功；包含 arm64 原生编译、R8 与 lintVital。
- 安装包签名验证、16 KB ZIP 对齐、LSPosed 入口及 arm64 原生库检查通过。
- `git diff --check` 通过。

Windows 测试环境采用独立的工作区测试缓存，避免 Robolectric 向无写权限的 `C:\` 写锁文件；测试专用 JVM 设置没有提交到项目。`media_compact_legacy.xml` 恢复为上游 Git blob 的原始 LF 字节，使哈希迁移测试输入不被 `core.autocrlf` 改写；该文件没有源码差异，没有通过修改测试断言绕过失败。

标准构建命令：

```sh
./gradlew :app:testDebugUnitTest :app:assembleRelease
python tools/verify-hmc-release.py /path/to/HyperMusicCover-v0.9.55.apk --expectations tools/tests/hmc-v0.9.55-symbols.properties
```

本地测试包 `HyperModifier-v1.6.0-hmcfix.1-test.apk` 的 SHA-256：
`aea2f3840d35398e1383bce6b548683b83bf34f705b9cbcd9efb461e2ccf5b7b`。
沿用旧修复包的本地测试签名，可覆盖旧修复包升级；不能直接覆盖不同签名的官方 APK。新版独立真机效果尚未反馈。
