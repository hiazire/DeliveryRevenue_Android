# 外送營業額 (DeliveryRevenue)

深色主題 Android App，辨識外送平台截圖中的訂單金額並加總，寄送報告至指定信箱。

---

## 功能

- 📸 選擇一或多張外送平台截圖（Uber Eats、Foodpanda 等）
- 🔍 ML Kit OCR 本地辨識金額（離線可用，免費）
- ➕ 自動加總所有圖片中的訂單交易金額
- 📅 自動讀取圖片 EXIF 日期
- 📧 SMTP 寄送報告至指定信箱
- ⚠️ 若有不同日期圖片，自動寄出錯誤通知
- 🎨 深紫橘配色暗色主題，固定直向顯示

---

## 需求

| 項目 | 版本 |
|------|------|
| Android | 12+ (API 31+) |
| Mac infra | macOS + Python + ADB |
| Java | 17+ |

---

## 第一次設定（Mac Mini）

### 1. 安裝必要工具

```bash
# Homebrew (如未安裝)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Java 17
brew install openjdk@17
echo 'export JAVA_HOME=$(brew --prefix openjdk@17)' >> ~/.zshrc

# ADB (已安裝可跳過)
brew install android-platform-tools
```

### 2. 安裝 Android SDK

選項 A — Android Studio（建議）：
- 下載 [Android Studio](https://developer.android.com/studio)
- 安裝後開啟 SDK Manager
- 確認安裝：Android 12 (API 31+) + Build Tools 34.0.0

選項 B — 僅 Command Line Tools：
```bash
brew install android-commandlinetools
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 3. 設定環境變數

在 `~/.zshrc` 加入：
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
export JAVA_HOME=$(brew --prefix openjdk@17)
```
然後：`source ~/.zshrc`

### 4. 初始化 Gradle Wrapper

```bash
cd DeliveryRevenue
chmod +x setup.sh build_and_install.sh
./setup.sh
```

---

## Build & 安裝

1. 手機開啟「開發者選項」→「USB 偵錯」
2. USB 連接手機和 Mac Mini
3. 執行：

```bash
./build_and_install.sh
```

或手動：
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## APP 設定（首次使用）

進入 APP → 右上角⚙️設定：

| 欄位 | 說明 |
|------|------|
| 收件者 Email | 收到報告的信箱 |
| 寄件者 Email | 寄信用的 Gmail |
| App 密碼 | Gmail App Password（非登入密碼） |
| SMTP 主機 | 預設 `smtp.gmail.com` |
| SMTP Port | 預設 `587` |

### Gmail App Password 申請方式

1. Google 帳戶 → 安全性
2. 開啟兩步驟驗證
3. 搜尋「應用程式密碼」→ 建立
4. 複製 16 碼密碼貼入 APP

---

## Email 格式

### 成功
- 標題：`q8js_外送平台營業額加總 2025/01/15`
- 日期來源：圖片 EXIF

### 失敗（日期衝突）
- 標題：`q8js_Failed calculation total delivery revenue - 2025/01/15`
- 日期來源：發送當下實際時間

---

## OCR 金額辨識邏輯

支援格式：
- `NT$123.00` / `$123.00`
- `TWD 123`
- 含關鍵字：`訂單金額`、`交易金額`、`總計`、`小計`、`合計`、`金額`

過濾掉：
- 流水號（8位以上純數字）
- 電話號碼
- 時間戳記（12:30）
- 百分比

---

## 專案結構

```
app/src/main/java/com/q8js/deliveryrevenue/
├── MainActivity.kt          # 進入點，Navigation
├── MainViewModel.kt         # 業務邏輯
├── data/
│   ├── Models.kt            # 資料模型
│   └── SettingsRepository.kt # DataStore 設定存取
├── ui/
│   ├── MainScreen.kt        # 主畫面 Composable
│   ├── SettingsScreen.kt    # 設定畫面 Composable
│   └── theme/Theme.kt       # 深紫橘暗色主題
└── util/
    ├── ExifUtil.kt          # 讀取圖片 EXIF 日期
    ├── OcrProcessor.kt      # ML Kit OCR 金額解析
    └── EmailSender.kt       # SMTP 寄信
```
