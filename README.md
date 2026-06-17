# 📱 小管家 Android App

给小爱音箱 Play 增强版配置 AI 大模型的 Android 客户端。

## 功能

- 🎤 **语音输入** — 使用 Android 内置语音识别，点击麦克风说话
- ✏️ **文字输入** — 可切换到键盘打字
- 🔊 **音箱推送** — AI 回答通过 Home Assistant MiIoT TTS 推送到小爱音箱
- ⚙️ **可配置服务器地址** — 支持局域网和外网服务器

## 下载 APK

每次推送代码到 `main` 分支，GitHub Actions 会自动编译 APK：

1. 打开项目的 **Actions** 标签页
2. 选择最新的成功运行的工作流
3. 在 **Artifacts** 中下载 `xiaoai-assistant-debug.zip`
4. 解压后安装 `app-debug.apk` 到手机

> 需要在手机「设置-安全」中启用「允许安装未知来源应用」

## 使用

1. 安装 APK 后打开 App
2. 确保手机和服务器在同一 Wi-Fi 网络
3. App 默认连接 `http://192.168.1.15:8765`
4. 点右上角 ⚙️ 可修改服务器地址
5. 点击 🎤 按钮说话，或切到文字输入聊天

## 编译方式

### GitHub Actions（自动）

推送到 `main` 分支即可自动编译，APK 在 Actions 产物中下载。

### 本地编译

```bash
# 需要安装 JDK 17 和 Android SDK
./gradlew assembleDebug
# APK 在 app/build/outputs/apk/debug/app-debug.apk
```

### 相关项目

- 🖥️ [xiaoai-assistant](https://github.com/JunbiaoXue/xiaoai-assistant) — 服务端（FastAPI + DeepSeek + HomeAssistant TTS）
