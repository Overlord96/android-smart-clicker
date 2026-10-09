# Smart Clicker (Android Game Automation)

[![Build Android APK](https://github.com/Overlord96/android-smart-clicker/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Overlord96/android-smart-clicker/actions/workflows/build-apk.yml)

A full-featured Android smart automation and auto-clicker application written in modern Kotlin. Designed for third-party games and apps, featuring an interactive floating overlay with draggable target points, custom trigger rule creator, native `AccessibilityService` node parsing, and computer vision fallback.

[English](#english) | [Русский](#русский)

---

## English

### ✨ Key Features
- **🎯 Interactive Floating Overlay (Draggable Target Points)**:
  - Floating control bar (`Play / Pause`, `Add Target [+]`, `Remove Target [-]`, `Settings [⚙]`, `Close [✕]`) positioned over any game or app.
  - Numbered target points (`1`, `2`, `3`...) that can be dragged directly onto game buttons or screen areas.
  - Per-point click delay settings and automatic multi-point sequence looping.
- **⚡ Custom Trigger Rules Manager**:
  - Add, edit, toggle, and delete custom text/ID rules directly in the app.
  - Specify custom game trigger words (e.g., `Start Battle`, `Claim Reward`, `Continue`, `Auto-play`).
  - Persistent storage in SharedPreferences.
- **🔍 Real-Time Node Tree Parsing**:
  - Traverses the `AccessibilityNodeInfo` hierarchy on window state and content changes.
  - Programmatic clicks directly via `AccessibilityNodeInfo.ACTION_CLICK` with fallback coordinate gesture taps via `dispatchGesture`.
- **👁 Vision Fallback Engine**:
  - Lightweight template matching engine for custom canvas, OpenGL, and Unity games where accessibility nodes are absent.
- **🛡 Anti-Spam & Debounce Protection**:
  - Configurable cooldown timers (in milliseconds) and per-element click debounce.
- **🌐 Clean Dashboard UI & Bilingual Localization**:
  - Service and overlay status indicators.
  - Real-time activity & trigger logs with millisecond timestamps.
  - Instant runtime language toggle (English ⇄ Russian).
- **🚀 Automated CI/CD**:
  - GitHub Actions workflow compiling `./gradlew assembleDebug` on every push and publishing the `.apk` as a downloadable artifact.

### 🚀 Getting Started

#### 1. Download Compiled APK
Go to the **[GitHub Actions Tab](../../actions)**:
1. Select the latest successful workflow run for **Build Android APK**.
2. Scroll down to the **Artifacts** section.
3. Download `smart-clicker-debug-apk` and install it on your device.

#### 2. Permissions Setup
1. Launch **Smart Clicker**.
2. Tap **Grant Accessibility** -> Enable **Smart Clicker Accessibility Service**.
3. Tap **Launch Floating Widget** -> Grant **Display over other apps** (Overlay permission).
4. Switch to your target game, place your target points with `(+)`, drag them into position, and press `▶`!

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
- **🎯 Плавающий оверлей с настройкой точек клика (Target Points)**:
  - Плавающая панель управления поверх игр и приложений (`Старт / Пауза`, `Добавить точку [+]`, `Удалить точку [-]`, `Настройки [⚙]`, `Закрыть [✕]`).
  - Пронумерованные маркеры точек (`1`, `2`, `3`...), которые можно перетаскивать пальцем прямо на кнопки игры.
  - Индивидуальная настройка задержки (мс) для каждой точки при клике по маркеру.
  - Автоматический циклический кликер по выставленным координатам.
- **⚡ Конструктор пользовательских правил и триггеров**:
  - Создание, включение/отключение и удаление собственных правил прямо в интерфейсе.
  - Возможность задать свои слова-триггеры для любой игры (например: *«Автобой», «Забрать награду», «Продолжить», «В бой»*).
  - Сохранение настроек между запусками в памяти устройства.
- **🔍 Парсинг дерева элементов в реальном времени (Accessibility)**:
  - Сканирует иерархию `AccessibilityNodeInfo` при изменении состояния окон и контента.
  - Выполняет программные клики через `AccessibilityNodeInfo.ACTION_CLICK` либо программные жесты `dispatchGesture`.
- **👁 Движок компьютерного зрения (Vision Fallback)**:
  - Модуль сопоставления шаблонов (Template Matching) для игр на Unity, OpenGL и Canvas, где отсутствуют стандартные ноды Android.
- **🛡 Защита от спама и кулдаун**:
  - Настраиваемый интервал задержки (мс) и кэш элементов для предотвращения зацикливания.
- **🌐 Удобный интерфейс и двуязычность**:
  - Индикаторы состояния службы и плавающего виджета.
  - Журнал событий в реальном времени с точностью до миллисекунд.
  - Мгновенное переключение языка интерфейса (Русский ⇄ English).
- **🚀 CI/CD в GitHub Actions**:
  - Автоматическая сборка `./gradlew assembleDebug` при каждом коммите и публикация готового `.apk` файла в артефактах GitHub.

### 🚀 Запуск и использование

#### 1. Скачивание готового APK
Перейдите во вкладку **[GitHub Actions](../../actions)**:
1. Откройте последний успешный запуск **Build Android APK**.
2. Внизу страницы найдите блок **Artifacts**.
3. Скачайте архив `smart-clicker-debug-apk`, распакуйте и установите APK на смартфон.

#### 2. Настройка и запуск
1. Откройте **Smart Clicker**.
2. Предоставьте разрешение **Специальные возможности** (Accessibility).
3. Нажмите **Запустить плавающий виджет** и предоставьте разрешение на показ поверх других окон.
4. Откройте нужную игру, нажмите `(+)` на плавающей панели, перетащите появившиеся точки на нужные кнопки игры и нажмите `▶`!
