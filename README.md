# Smart Clicker (Android Game Automation)

[![Build Android APK](https://github.com/Overlord96/android-smart-clicker/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Overlord96/android-smart-clicker/actions/workflows/build-apk.yml)

A high-performance Android smart automation and auto-clicker application written in modern Kotlin. Designed for third-party games and apps using Android's native `AccessibilityService` API and computer vision template matching fallback.

[English](#english) | [Русский](#русский)

---

## English

### ✨ Key Features
- **Real-Time Node Tree Parsing**:
  - Traverses the `AccessibilityNodeInfo` hierarchy on window state and content changes.
  - Matches custom target triggers (`Claim`, `Collect`, `Start`, `Battle`, `Next`, etc.).
  - Executes programmatic clicks directly (`AccessibilityNodeInfo.ACTION_CLICK`) or falls back to programmatic coordinates via `dispatchGesture`.
- **Vision Fallback Engine**:
  - Lightweight pixel/template matching engine for custom canvas, OpenGL, and Unity-rendered games where accessibility nodes are absent.
  - Captures frames via `takeScreenshot` (Android 11+) and computes target tap locations.
- **Smart Anti-Spam & Debounce**:
  - Configurable cooldown timers (in milliseconds) and per-element click debounce to prevent infinite rapid triggering.
- **Clean Dashboard UI**:
  - Real-time status indicator (Active / Stopped / Permission missing).
  - Live activity & trigger log with millisecond-precision timestamps.
  - Runtime bilingual localization toggle (English ⇄ Russian).
- **Automated CI/CD**:
  - Pre-configured GitHub Actions workflow compiling `./gradlew assembleDebug` on every push and publishing the `.apk` as a downloadable artifact.

### 🚀 Getting Started

#### 1. Download Compiled APK
Go to the **[GitHub Actions Tab](../../actions)**:
1. Select the latest successful workflow run for **Build Android APK**.
2. Scroll down to the **Artifacts** section.
3. Download `smart-clicker-debug-apk` and install it on your device.

#### 2. Grant Accessibility Permissions
1. Launch **Smart Clicker**.
2. Tap **Grant Accessibility** (or open Android *Settings* -> *Accessibility* -> *Downloaded apps*).
3. Enable **Smart Clicker Accessibility Service**.
4. Return to the app and tap **Start Automation**.

#### 3. Build Locally
```bash
git clone https://github.com/Overlord96/android-smart-clicker.git
cd android-smart-clicker
./gradlew assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Русский

### ✨ Основные возможности
- **Парсинг дерева элементов в реальном времени**:
  - Сканирует иерархию `AccessibilityNodeInfo` при изменении состояния окон и контента.
  - Распознает триггеры на русском и английском языках («Забрать», «Собрать», «Старт», «Бой», «Далее» и др.).
  - Выполняет программные клики через `AccessibilityNodeInfo.ACTION_CLICK` либо программные жесты `dispatchGesture`.
- **Движок компьютерного зрения (Vision Fallback)**:
  - Модуль сопоставления шаблонов (Template Matching) для игр на Unity, OpenGL и Canvas, где отсутствуют стандартные ноды Android.
  - Анализ скриншотов через `takeScreenshot` (Android 11+) и симуляция тапов по найденным координатам.
- **Защита от спама и кулдаун**:
  - Настраиваемый интервал задержки (мс) и кэш элементов для предотвращения зацикливания.
- **Удобный интерфейс и двуязычность**:
  - Индикатор состояния службы (Активна / Остановлена / Нет разрешений).
  - Журнал событий в реальном времени с точностью до миллисекунд.
  - Мгновенное переключение языка интерфейса (Русский ⇄ English).
- **CI/CD в GitHub Actions**:
  - Автоматическая сборка `./gradlew assembleDebug` при каждом коммите и публикация готового `.apk` файла в артефактах GitHub.

### 🚀 Запуск и использование

#### 1. Скачивание готового APK
Перейдите во вкладку **[GitHub Actions](../../actions)**:
1. Откройте последний успешный запуск **Build Android APK**.
2. Внизу страницы найдите блок **Artifacts**.
3. Скачайте архив `smart-clicker-debug-apk`, распакуйте и установите APK на смартфон.

#### 2. Активация специальных возможностей
1. Откройте приложение **Smart Clicker**.
2. Нажмите **Включить в настройках** (или перейдите в *Настройки Android* -> *Спец. возможности* -> *Скачанные приложения*).
3. Включите службу **Служба автокликера Smart Clicker**.
4. Вернитесь в приложение и нажмите **Запустить автоматизацию**.

---

### 📁 Структура проекта / Project Structure
```
android-smart-clicker/
├── .github/workflows/
│   └── build-apk.yml             # CI/CD: сборка APK в GitHub Actions
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # Манифест и регистрация AccessibilityService
│   │   ├── java/com/smartclicker/app/
│   │   │   ├── SmartClickerApp.kt
│   │   │   ├── model/
│   │   │   │   ├── AutomationRule.kt
│   │   │   │   └── LogEntry.kt
│   │   │   ├── service/
│   │   │   │   └── SmartAccessibilityService.kt   # Ядро Accessibility & кликов
│   │   │   ├── vision/
│   │   │   │   └── VisionAutomationEngine.kt      # Template matching для Canvas/Unity
│   │   │   ├── utils/
│   │   │   │   ├── LocaleHelper.kt                # Локализация EN/RU
│   │   │   │   └── PermissionUtils.kt
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt
│   │   │       └── LogAdapter.kt
│   │   └── res/
│   │       ├── layout/
│   │       ├── values/strings.xml                 # English localization
│   │       ├── values-ru/strings.xml              # Русская локализация
│   │       └── xml/accessibility_service_config.xml
│   └── build.gradle.kts
├── gradle/wrapper/
├── build.gradle.kts
└── settings.gradle.kts
```
