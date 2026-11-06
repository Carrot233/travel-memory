# 旅途记忆 Android

This is a standalone, offline-first Android app for 旅途记忆. It does not call the Next.js website, `/api` routes, or any server. There is no server-URL setting. Photos, routes, videos, and stories are stored on the device with Room and app-private files, and they remain after the app is force-stopped.

构建时 Gradle 会下载依赖，这只发生在开发机器上。照片、路线、视频和故事仍保存在本机。给路线添加景点时，如果配置了高德 Web 服务 Key，可以用地名搜索；没有 Key 时改为手填经纬度。搜索不使用定位权限。

## 打开和运行

要求：

- Android Studio 当前稳定版（可打开 Android Gradle Plugin 9.5 的工程）
- JDK 17 或更高
- Android SDK 37

用 Android Studio **打开 `android/` 目录**（不是仓库根目录），等 Gradle 同步完成后运行 `app`。

命令行：

```bash
cd android
./gradlew :app:assembleDebug
```

调试 APK 在 `android/app/build/outputs/apk/debug/app-debug.apk`。`minSdk` 是 26。应用名称是「旅途记忆」，包名是 `com.carrot233.travelmemory`。

`local.properties` 由 Android Studio 生成，不要提交。

## 本地数据

- 元数据在本机 Room 数据库 `travel_memory.db`。
- 照片、视频和封面复制到应用私有目录 `files/media/`，不依赖相册里的原文件是否还在。
- 可以新增、编辑、删除照片、路线、视频和故事。
- 路线图用景点的经纬度在本地画出来，不使用地图 SDK。
- 添加或编辑景点时可以搜索地名。搜索走高德输入提示接口（和网站 `LocationPicker` 的 AutoComplete 相同）。Key 不写进代码：在 `android/local.properties`（已忽略）或 Gradle 参数里设置 `AMAP_KEY`。示例见 `local.properties.example`。未配置时应用会提示搜索不可用，经纬度仍可手填。
- 深色模式在「设置」里切换，可跟随系统。
- 没有登录，也没有账号服务器。

可选：在「设置」里用系统文件选择器导入网站数据包。zip 里需要有 `travel.db`（可以在 `data/travel.db`），媒体文件放在 `uploads/` 或 `public/uploads/`。导入是一次性的，不会反向同步，也不会联网。用户表和密码不会导入。网站故事里的 `photo_ids` / `route_ids` 会换成这台设备上的照片和路线。如果之前已经导入过、故事里还没有照片，再导入一次同一个包即可补上关联，不会复制出第二份故事。

## 冷启动后数据还在

在模拟器或真机上：

1. 添加一张照片、一条带两个景点的路线、一篇故事，以及一个视频。
2. 强制停止应用：

```bash
adb shell am force-stop com.carrot233.travelmemory
adb shell am start -n com.carrot233.travelmemory/.MainActivity
```

3. 四项内容都会重新出现。

也可以跑仪器测试，它会写入 Room 和私有文件、关掉数据库、再打开并核对：

```bash
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` 需要已连接的设备或模拟器。

## Gradle Wrapper

仓库里包含 `gradlew`、`gradlew.bat` 和 `gradle/wrapper/gradle-wrapper.jar`，Gradle 版本是 9.6.0。如果 jar 丢失，在已安装 Gradle 的机器上执行：

```bash
cd android
gradle wrapper --gradle-version 9.6.0
```
