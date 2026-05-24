# CHANGELOG_AGENT

Журнал изменений, выполненных AI-агентом в проекте `FajrApp`.

## Правило ведения
- Файл обновляется после завершения каждой задачи в рамках этой рабочей сессии.
- Для каждой записи фиксируются: дата, что сделано, какие файлы затронуты, как проверено.

---

## 2026-05-25

### Задача: создать базовый файл инструкций для агента
- Статус: выполнено
- Что сделано:
  - Создан файл `Agent.md` в корне проекта.
  - Добавлены: назначение, принципы работы, рабочий процесс, полезные команды, формат отчета.
- Измененные файлы:
  - `Agent.md`
- Проверка:
  - Файл успешно создан и доступен в корне проекта.

### Задача: исправить критические ошибки запуска/сборки
- Статус: выполнено
- Что сделано:
  - Исправлена нестабильная версия Gradle Wrapper:
    - `gradle-9.0-milestone-1` -> `gradle-8.2`.
  - Зафиксирован JDK для Gradle на встроенный JBR Android Studio:
    - `org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr`.
  - Добавлен отсутствующий файл `app/proguard-rules.pro`, на который ссылается release-конфиг.
- Измененные файлы:
  - `gradle/wrapper/gradle-wrapper.properties`
  - `gradle.properties`
  - `app/proguard-rules.pro`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на эмулятор
  - `adb shell am start -n com.example.fajrapp/com.example.fajrapp.MainActivity` -> Activity запускается

### Задача: создать журнал изменений агента
- Статус: выполнено
- Что сделано:
  - Создан `CHANGELOG_AGENT.md`.
  - Добавлены записи по ранее выполненным задачам.
- Измененные файлы:
  - `CHANGELOG_AGENT.md`
- Проверка:
  - Файл создан в корне проекта и заполнен.

### Задача: выгрузка проекта в удаленный репозиторий GitHub
- Статус: выполнено
- Что сделано:
  - Настроен удаленный репозиторий `origin`:
    - `https://github.com/timursarsembai/FajrAppAndroid.git`
  - Выполнена публикация ветки `main` на удаленный репозиторий.
- Измененные файлы:
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `git push origin main` выполнен успешно.

### Задача: реализовать пункт Location в настройках
- Статус: выполнено
- Что сделано:
  - Добавлен экран `LocationScreen` с действием "Use current location".
  - Подключена навигация `settings -> location`.
  - Убран хардкод `Moscow, Russia`: подпись локации теперь берется из `SettingsViewModel`.
  - В `SettingsViewModel` добавлены:
    - состояние `locationSubtitle`,
    - флаг `isUpdatingLocation`,
    - метод `updateLocationFromDevice()` с сохранением в `SharedPreferences`.
  - Добавлены новые строковые ресурсы для EN/RU.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/LocationScreen.kt`
  - `app/src/main/java/com/example/fajrapp/ui/SettingsScreen.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/SettingsViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/MainActivity.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на эмулятор
  - `adb shell am start -n com.example.fajrapp/com.example.fajrapp.MainActivity` -> экран запускается

### Задача: добавить ручной ввод города и координат в настройках локации
- Статус: выполнено
- Что сделано:
  - В экран `LocationScreen` добавлены:
    - поле ввода города + кнопка поиска города,
    - поля широты/долготы + кнопка сохранения по координатам,
    - вывод сообщения об успехе/ошибке.
  - В `SettingsViewModel` добавлены сценарии:
    - `updateLocationFromCity(cityQuery)` — geocoding по названию города,
    - `updateLocationFromCoordinates(lat, lon)` — валидация и сохранение координат,
    - сохранение найденной локации в `SharedPreferences`,
    - обновление `locationSubtitle`, `locationLatitude`, `locationLongitude`.
  - Добавлены строковые ресурсы EN/RU для новых элементов UI и сообщений ошибок.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/LocationScreen.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/SettingsViewModel.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: привести кнопки локации к общему стеклянному стилю приложения
- Статус: выполнено
- Что сделано:
  - Удалены синие `Material` кнопки на экране локации.
  - Добавлены стеклянные action-кнопки в стиле остальных элементов UI.
  - Сохранены прежние действия:
    - поиск по городу,
    - сохранение по координатам.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/LocationScreen.kt`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Задача: улучшить поиск города (кириллица/латиница)
- Статус: выполнено
- Что сделано:
  - В `SettingsViewModel` доработан поиск города:
    - добавлены несколько вариантов запроса (включая транслитерацию),
    - поиск выполняется по нескольким locale (`default`, `en`, `ru`, `kk`),
    - это повышает шанс, что `Алматы` и `Almaty` будут находиться одинаково.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/viewmodel/SettingsViewModel.kt`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Задача: автопоиск города с выпадающими вариантами + старт реализации "Метод расчета"
- Статус: выполнено
- Что сделано:
  - Реализован автопоиск города по мере ввода:
    - при вводе от 2 символов запускается поиск,
    - показывается выпадающий список найденных городов,
    - выбор варианта из списка сразу сохраняет локацию.
  - Добавлен экран `Метод расчета`:
    - выбор метода расчета,
    - выбор метода расчета Асра (Шафии/Ханафи).
  - Выбранные настройки сохраняются в `SharedPreferences`.
  - `PrayerViewModel` начал использовать сохраненные:
    - метод расчета,
    - метод Асра,
    - и автоматически пересчитывает времена при изменении конфигурации.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/viewmodel/SettingsViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/ui/LocationScreen.kt`
  - `app/src/main/java/com/example/fajrapp/ui/CalculationMethodScreen.kt`
  - `app/src/main/java/com/example/fajrapp/ui/SettingsScreen.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/PrayerViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/data/PreferencesManager.kt`
  - `app/src/main/java/com/example/fajrapp/MainActivity.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: исправить вылет при вводе второго символа в поиске города
- Статус: выполнено
- Что сделано:
  - Убран popup `DropdownMenu` для подсказок городов.
  - Реализован встроенный под полем "dropdown-like" список подсказок в стеклянном контейнере.
  - Сохранена логика автопоиска и выбора города из списка.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/LocationScreen.kt`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: устранить повторный вылет при поиске города
- Статус: выполнено
- Что сделано:
  - По `logcat` выявлена точная причина падения:
    - `NoSuchMethodError` внутри `CircularProgressIndicator` (Compose animation/runtime mismatch на устройстве).
  - Удален анимированный `CircularProgressIndicator` из `TextField` при поиске.
  - Заменен на статичный безопасный индикатор (`"..."`) без анимации.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/LocationScreen.kt`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: реализовать экран "Смещение времени" по каждому намазу
- Статус: выполнено
- Что сделано:
  - Добавлен отдельный экран `TimeOffsetScreen` со списком намазов и кнопками `+`/`-` для каждого пункта.
  - Реализовано двустороннее смещение времени (в минутах) для каждого намаза с ограничением диапазона `-180..+180`.
  - Смещения сохраняются в `SharedPreferences` и подгружаются при открытии приложения.
  - Пункт `Смещение времени` в `Settings` показывает состояние: значение по умолчанию или пользовательские смещения.
  - Расчет времен намаза в `PrayerViewModel` обновлен: применяются индивидуальные смещения перед отображением и расчетом следующего намаза.
  - Добавлены локализованные строки EN/RU для новых подписей и форматирования смещений.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/TimeOffsetScreen.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/SettingsViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/PrayerViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/ui/SettingsScreen.kt`
  - `app/src/main/java/com/example/fajrapp/data/PreferencesManager.kt`
  - `app/src/main/java/com/example/fajrapp/MainActivity.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство
