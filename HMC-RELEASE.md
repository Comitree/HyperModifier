# HMC 修复版发布说明

2026-11-12，Comitree fork 的非官方修改版，基于 AritxOnly/HyperModifier v1.6.0，继续按 GPLv3 发布，保留原版权、LICENSE 与第三方声明。

修改：适配新版 HMC 设置对象及内部成员；提前安装原通知 Flow 监听，保持通知和指纹策略不变。用户已确认实机修复完成；649 项测试通过。

构建：JDK 21、Android SDK 37、NDK 28.2.13676358、CMake 3.22.1、Gradle 9.3.1；运行 ./gradlew :app:testDebugUnitTest :app:assembleRelease，使用 Android build-tools zipalign 与 apksigner 以自己的密钥签名。测试环境需可下载 Robolectric 依赖。

发布 APK 为已实机验证的 hmcfix.2 原文件重命名，无重新编译：SHA-256 de32e381630cfa940f5fa17ebf978a6e70d469a6c3761151ab8cc0ba2b4009b3。

使用独立测试签名，可覆盖此前同签名修复包；与官方签名不同。完整对应源码通过本仓库发布标签提供，包含构建文件及第三方许可说明。
