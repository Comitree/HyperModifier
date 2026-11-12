# 标题：1.6.0 HMC 接口解析失败及展开通知不下沉

现象：启用“全屏岛展开时下沉通知并隐藏指纹”后无效。HMC v0.9.55 可复现接口解析失败。

原因与修复：
1. `HyperMusicCoverDexSymbols` 仍按 static int/boolean 查找指纹设置；新版 HMC 改为 Settings 对象内的 volatile 实例值。需要解析 sget-object → iget，并通过只读访问器读取当前值，同时兼容 postInvalidate 及新版 ClockRoom 分支。
2. 1.6.0 将扩展安装延后到首帧之后，可能错过通知 combine 的首次收集，报“未捕获首次通知 Flow 收集”。仅将原通知 Flow Hook 提前到 startEarlySystemUiHooks，独立防重复安装，其余 Hook 继续延迟。

通知计算、展开条件和指纹恢复逻辑无需修改。

验证：649 项单测通过；官方 HMC 0.8.1/0.9.0/0.9.50/0.9.55 APK 解析通过；用户已确认修复包实机正常。
