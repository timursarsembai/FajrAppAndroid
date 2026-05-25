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

### Задача: добавить режим "Переход на летнее время" в метод расчета
- Статус: выполнено
- Что сделано:
  - В экран `Метод расчета` добавлен новый пункт `Переход на летнее время` с 3 вариантами:
    - `Авто (по региону)`
    - `-1 час`
    - `+1 час`
  - Режим сохраняется в `SharedPreferences` и загружается при старте приложения.
  - В `PrayerViewModel` добавлено применение DST-смещения к расчету всех времен намаза.
  - Для `Авто` реализовано определение DST по региону выбранной локации:
    - определяется страна из сохраненной локации,
    - подбирается соответствующая таймзона региона,
    - проверяется, действует ли DST на текущую дату по системным timezone-правилам Android (tzdb).
  - Добавлены новые строковые ресурсы EN/RU для пункта DST.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/CalculationMethodScreen.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/SettingsViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/PrayerViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/data/PreferencesManager.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: исправить отсутствие live-обновления на экране "Смещение времени"
- Статус: выполнено
- Что сделано:
  - Найдена причина: `TimeOffsetScreen` не был подписан на `uiState`, из-за чего Compose не выполнял мгновенную перерисовку значений после нажатий `+/-`.
  - Добавлена подписка `collectAsState()` на `viewModel.uiState` в `TimeOffsetScreen`.
  - Значение смещения для каждой строки теперь берется из `uiState.timeOffsets` вместо прямого чтения через вызов метода без наблюдения состояния.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/TimeOffsetScreen.kt`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: переработать структуру настроек и блоки намазов на главном экране
- Статус: выполнено
- Что сделано:
  - Пункт `Смещение времени` убран с главной страницы `Настройки` и перенесен в экран `Метод расчета`.
  - На экране `Метод расчета` добавлен отдельный переход в `Смещение времени` (с текущим статусом смещений).
  - На главной странице добавлен намаз `Тахаджуд` после `Иша`.
  - Время `Тахаджуд` рассчитывается как начало последней трети ночи между `Иша` и `Фаджр`:
    - берется интервал `Иша -> Фаджр следующего дня`,
    - вычисляется старт третьей части ночи (`Иша + 2/3 интервала`).
  - `Восход` заменен на `Духа`; время `Духа` считается как `Восход + 20 минут`.
  - Каждый блок намаза на главной странице сделан раскрывающимся (`dropdown`):
    - по нажатию раскрывается контент,
    - добавлены две пока неактивные кнопки: `Уроки омовения` и `Уроки <название>-намаза`.
  - Модель `PrayerData` расширена ключом `key` для стабильного состояния раскрытия.
  - Добавлены новые строковые ресурсы EN/RU (`Тахаджуд`, уроки, `Духа`).
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/SettingsScreen.kt`
  - `app/src/main/java/com/example/fajrapp/ui/CalculationMethodScreen.kt`
  - `app/src/main/java/com/example/fajrapp/MainActivity.kt`
  - `app/src/main/java/com/example/fajrapp/model/PrayerData.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/PrayerViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/ui/PrayerScreen.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: устранить «крякозябры» (проблемы кодировки)
- Статус: выполнено
- Что сделано:
  - Полностью восстановлен `values-ru/strings.xml` в корректной UTF-8 кодировке с нормальными русскими строками.
  - В `SettingsViewModel` исправлены поврежденные литералы языка/названий и строка нормализации символов `Ё/ё`.
  - Убраны поврежденные символы в обработке апострофа для поиска городов.
- Измененные файлы:
  - `app/src/main/res/values-ru/strings.xml`
  - `app/src/main/java/com/example/fajrapp/viewmodel/SettingsViewModel.kt`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: изменить нижний блок главного экрана (локация в часах, увеличить шрифт времени, убрать дуа)
- Статус: выполнено
- Что сделано:
  - Блок локации перенесен внутрь стеклянного блока с текущим временем (часы + строка локации в одном блоке).
  - Размер шрифта часов увеличен примерно на треть (`40sp -> 53.3sp` с учетом масштаба экрана).
  - Текст `Да примет Аллах ваши молитвы` полностью удален с главного экрана.
  - Нижний отдельный блок с локацией удален.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerScreen.kt`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: добавить экран календаря по клику на дату на главной странице
- Статус: выполнено
- Что сделано:
  - По нажатию на блок даты на главной теперь открывается экран календаря.
  - Добавлен новый экран `HijriCalendarScreen` с прокруткой месяцев (диапазон: ±120 месяцев от текущего, можно расширить при необходимости).
  - В верхней части календаря отображается год по хиджре и в скобках соответствующий год (или диапазон лет) по миляди.
  - Для каждого месяца отображается:
    - название месяца по хиджре,
    - под ним в скобках месяц(ы) по миляди (например: `Май - Июнь`),
    - таблица на 7 столбцов (ПН..ВС),
    - в каждой ячейке: число хиджри и под ним число миляди меньшим шрифтом.
  - При прокрутке месяцев автоматически меняются заголовок года/месяца и содержимое таблицы.
  - Для Android ниже 8.0 добавлен fallback-экран с сообщением, что календарь недоступен.
  - Добавлены строковые ресурсы EN/RU для календаря.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerScreen.kt`
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `app/src/main/java/com/example/fajrapp/MainActivity.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: визуально выделить текущий месяц и год на экране календаря
- Статус: выполнено
- Что сделано:
  - Годовой заголовок вынесен в отдельный стеклянный блок в шапке календаря.
  - Текущий месяц выделен визуально:
    - добавлена акцентная рамка карточки,
    - добавлен бейдж `Текущий месяц`,
    - слегка усилен цвет заголовка месяца.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: сделать ячейки календаря почти квадратными
- Статус: выполнено
- Что сделано:
  - Убрана фиксированная вертикальная высота ячеек календаря.
  - Добавлена пропорция `aspectRatio(0.95f)`, чтобы высота была почти равна ширине.
  - Уменьшены внутренние вертикальные отступы внутри ячейки.
  - Немного уменьшен `lineHeight` у чисел, чтобы ячейка визуально была компактнее.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Задача: оптимизировать производительность на слабых устройствах
- Статус: выполнено
- Что сделано:
  - В `GlassContainer` добавлен автоматический `performance mode`:
    - на low-RAM устройствах и Android <= 11 realtime blur отключается,
    - вместо blur используется облегченный полупрозрачный фон + рамка (без `hazeChild`).
  - Добавлен параметр `blurEnabled` в `GlassContainer` для точечного отключения blur.
  - Для сетки календаря blur в ячейках отключен принудительно (`blurEnabled = false`), чтобы снизить нагрузку на UI-поток.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/components/GlassComponents.kt`
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство
  - `adb shell dumpsys meminfo com.example.fajrapp` -> TOTAL PSS около 157 MB
  - `adb shell dumpsys gfxinfo com.example.fajrapp` -> подтверждена основная нагрузка на UI thread (а не GPU)

### Задача: исправить склонения месяцев по миляди в календаре (русский)
- Статус: выполнено
- Что сделано:
  - Для названий месяцев по миляди в календаре заменено форматирование на `TextStyle.FULL_STANDALONE`.
  - Это дает именительный падеж в русском (`Май - Июнь` вместо `Мая - Июня`).
  - Добавлен fallback на `TextStyle.FULL`, если standalone-форма недоступна.
- Измененные файлы:
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `CHANGELOG_AGENT.md`
- Проверка:
  - `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\gradlew.bat installDebug` -> APK установлен на устройство

### Task: replace fade navigation transitions with right-to-left slide transitions
- Status: completed
- What was done:
  - In `MainActivity`, replaced `fadeIn/fadeOut` transitions with horizontal slide transitions for all major routes (`home`, `calendar`, `settings`, `languages`, `location`, `calculation_method`, `time_offset`).
  - Forward navigation now visually pushes a new screen from the right.
  - Back navigation now returns with reverse slide, matching mobile native navigation expectations.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/MainActivity.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: optimize main screen recomposition load (clock/countdown)
- Status: completed
- What was done:
  - Removed per-second full `uiState` updates from `PrayerViewModel`.
  - `PrayerViewModel` timer now checks config periodically (every ~30s) and recalculates prayer times when the next prayer time is reached, instead of rebuilding screen state every second.
  - Added `timeMillis` to `PrayerData` for precise next-prayer countdown rendering in UI.
  - Moved live ticking clock (`HH:mm:ss`) to local Compose state in `PrayerScreen` (`produceState`), so only clock text updates every second.
  - Moved next-prayer countdown ticking to local Compose state inside prayer item (`produceState`), so only the active countdown text updates every second.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/model/PrayerData.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/PrayerViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/ui/PrayerScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: profile and optimize UI rendering on low-end device
- Status: completed
- What was done:
  - Profiled current app on physical device (`CPH2127`) using `adb dumpsys gfxinfo` and `adb dumpsys meminfo`.
  - Reduced expensive per-frame work on the main screen:
    - moved live clock and next-prayer countdown into isolated composables so only small text regions update each second,
    - replaced nested `GlassContainer` for passed-prayer check icon with lightweight circle background,
    - disabled blur for frequently updating/interactive prayer UI blocks (`PrayerItem`, lesson buttons, and clock card) while keeping glass styling via translucent background and border.
  - Previously completed state optimization retained: `PrayerViewModel` no longer rebuilds full screen state every second.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `adb shell dumpsys gfxinfo com.example.fajrapp` (after changes): 50th percentile ~48ms (was ~61ms in prior sample)
  - `adb shell dumpsys meminfo com.example.fajrapp`: TOTAL PSS ~140 MB

### Task: disable blur globally across the whole app
- Status: completed
- What was done:
  - Updated `GlassContainer` to always render simple glass style (translucent fill + border) without realtime blur.
  - Removed conditional blur logic and haze child rendering, so calendar/settings/home screens all use no-blur glass on every device.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/components/GlassComponents.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: highlight next prayer block and optimize calendar rendering
- Status: completed
- What was done:
  - Added thin gold border highlight for the expected/next prayer block on the main screen (same visual style direction as current-month highlight in calendar).
  - Calendar performance optimizations:
    - reduced month scroll range from ±120 to ±72 months,
    - replaced heavy glass rendering for each day cell with lightweight background+border cell rendering,
    - precomputed `weeks` in month data model to avoid repeated `chunked(7)` work during recomposition.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerScreen.kt`
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: highlight next prayer name and time with gold color
- Status: completed
- What was done:
  - On main screen, for the expected/next prayer block (`isNext`), set prayer name (Cyrillic) and prayer time text color to gold (`#FFE7A3`).
  - Other prayer blocks keep default white color.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: reduce calendar range to 36 months
- Status: completed
- What was done:
  - Reduced Hijri calendar month scroll range from `±72` to `±36` months.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: add prayer-based alarms screen opened by tapping the clock
- Status: completed
- What was done:
  - Added navigation from main clock block to a new `PrayerAlarmsScreen` (tap on current time opens alarms page).
  - Implemented prayer alarm model/storage:
    - new `PrayerAlarm` data model,
    - persistent alarm list in `SharedPreferences` as JSON.
  - Implemented full alarms UI:
    - choose prayer (Fajr, Duha, Dhuhr, Asr, Maghrib, Isha, Tahajjud),
    - choose relative direction (before/after prayer),
    - set minutes offset,
    - add alarm,
    - enable/disable existing alarms,
    - delete alarms.
  - Implemented scheduling/runtime logic:
    - `AlarmManager` scheduling via `PrayerAlarmScheduler`,
    - next trigger time computed dynamically from current prayer times and app settings (method/madhab/DST/time offsets/location),
    - support for Tahajjud and Duha calculations,
    - automatic rescheduling after trigger for next occurrence.
  - Implemented alarm trigger receiver:
    - `PrayerAlarmReceiver` shows high-priority alarm-category notification,
    - alarm channel configured with device default alarm sound.
  - Implemented reboot recovery:
    - `PrayerAlarmBootReceiver` re-schedules enabled alarms after device boot.
  - Added required manifest permissions and receivers:
    - `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`.
  - Updated permission request flow in `MainActivity` to include notifications permission on Android 13+.
  - Added RU/EN localized strings for alarms UI and notifications.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/model/PrayerAlarm.kt`
  - `app/src/main/java/com/example/fajrapp/data/PreferencesManager.kt`
  - `app/src/main/java/com/example/fajrapp/data/PrayerAlarmScheduler.kt`
  - `app/src/main/java/com/example/fajrapp/data/PrayerAlarmReceiver.kt`
  - `app/src/main/java/com/example/fajrapp/data/PrayerAlarmBootReceiver.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/PrayerAlarmViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/ui/PrayerAlarmsScreen.kt`
  - `app/src/main/java/com/example/fajrapp/ui/PrayerScreen.kt`
  - `app/src/main/java/com/example/fajrapp/MainActivity.kt`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\\gradlew.bat installDebug` -> APK installed on device
  - `adb shell am start -n com.example.fajrapp/com.example.fajrapp.MainActivity` -> app starts

### Task: limit calendar to fixed 6-month window
- Status: completed
- What was done:
  - Changed calendar range to a fixed list of 6 months:
    - 4 previous months,
    - current month,
    - 1 next month.
  - Implemented offsets range `-4..+1` in Hijri calendar screen.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: rework alarms UX into list page + separate add form page
- Status: completed
- What was done:
  - Updated alarms list page behavior:
    - default page now shows only alarm list,
    - when empty it shows `Нет будильников` (RU locale).
  - Added `+` button in the top-right corner of alarms list page.
  - Added separate add-alarm page (`alarm_add`) with prayer-relative form.
  - Wired navigation flow:
    - tap `+` -> open add form page,
    - after successful add -> form page closes and user returns to alarms list page.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerAlarmsScreen.kt`
  - `app/src/main/java/com/example/fajrapp/MainActivity.kt`
  - `app/src/main/res/values-ru/strings.xml`
  - `app/src/main/res/values/strings.xml`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: open alarm edit form when tapping an existing alarm
- Status: completed
- What was done:
  - Added edit flow for existing prayer alarms.
  - Tapping an alarm item on alarms list now opens a dedicated edit form page.
  - Added `alarm_edit/{alarmId}` navigation route.
  - Added `PrayerAlarmEditScreen` with prefilled values:
    - selected prayer,
    - before/after mode,
    - minutes offset.
  - Added save action on edit form that updates alarm settings and returns user to alarms list page.
  - Extended `PrayerAlarmViewModel` with:
    - `getAlarmById(alarmId)`,
    - `updateAlarm(alarmId, prayerKey, offsetMinutes)` with rescheduling logic.
  - Added RU/EN strings for edit page and not-found state.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerAlarmsScreen.kt`
  - `app/src/main/java/com/example/fajrapp/MainActivity.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/PrayerAlarmViewModel.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: show only one month in calendar and switch by arrow buttons
- Status: completed
- What was done:
  - Removed calendar scrolling and multi-month list rendering.
  - Calendar now displays only one month at a time (current by default).
  - Added month navigation arrows (left/right) in the top-right area of the month block.
  - Left arrow switches to previous month, right arrow switches to next month.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: calendar UX update + advanced alarm forms (repeat/ringtone/wheel picker)
- Status: completed
- What was done:
  - Calendar updates:
    - under Hijri month title, added month name in Arabic in parentheses,
    - when month is not current, added reset button near month navigation arrows to return to current month,
    - reworked header layout: top row is now back button + centered title `Календарь`; year block moved to a separate row below.
  - Alarm list updates:
    - alarms are now sorted by prayer order (Fajr, Duha, Dhuhr, Asr, Maghrib, Isha, Tahajjud),
    - alarms are visually grouped by prayer with section headers.
  - Add/Edit alarm forms updates:
    - replaced numeric input with vertical wheel picker (animated spinner style) for minutes,
    - added repeat settings:
      - `Каждую неделю`,
      - `Выбор дней` with weekday chips,
    - added ringtone setting with device alarm sounds list + default system alarm option.
  - Alarm model/runtime updates:
    - extended alarm model with repeat mode, selected weekdays, ringtone uri/title,
    - scheduler now respects repeat weekdays for alarm trigger dates,
    - receiver now uses per-alarm ringtone and per-alarm notification channel.
  - Prepared embedded-audio directories in project:
    - `app/src/main/assets/audio/alarms/`
    - `app/src/main/assets/audio/azan/`
    - added `.gitkeep` in both directories.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `app/src/main/java/com/example/fajrapp/ui/PrayerAlarmsScreen.kt`
  - `app/src/main/java/com/example/fajrapp/viewmodel/PrayerAlarmViewModel.kt`
  - `app/src/main/java/com/example/fajrapp/model/PrayerAlarm.kt`
  - `app/src/main/java/com/example/fajrapp/data/PreferencesManager.kt`
  - `app/src/main/java/com/example/fajrapp/data/PrayerAlarmScheduler.kt`
  - `app/src/main/java/com/example/fajrapp/data/PrayerAlarmReceiver.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `app/src/main/assets/audio/alarms/.gitkeep`
  - `app/src/main/assets/audio/azan/.gitkeep`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: refine minute wheel picker visuals in alarm form
- Status: completed
- What was done:
  - Increased minute wheel number size for better readability.
  - Changed number color from black to white to match app text style.
  - Replaced dark wheel separator lines with subtle light separators.
  - Applied styling in both add and edit alarm forms (shared minute picker).
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerAlarmsScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: adjust alarm repeat UI blocks and fix scrolling on forms
- Status: completed
- What was done:
  - In alarm add/edit forms, removed separate repeat mode pair (`Каждую неделю` / `Выбор дней`).
  - Repeat section now always shows blocks directly:
    - `Каждую неделю`,
    - weekday blocks (`ПН..ВС`) on the same section.
  - Weekly block now sets all weekdays; weekday blocks can be toggled individually.
  - Fixed form scrolling on small screens:
    - add/edit form content now uses vertical scroll when it exceeds screen height.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerAlarmsScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: enforce white large text for all minute wheel values and remove dividers
- Status: completed
- What was done:
  - Updated minute wheel styling so both selected and non-selected values stay white and large during scrolling.
  - Applied style via NumberPicker internal selector paint (`mSelectorWheelPaint`) for stable scrolling behavior.
  - Removed separator lines entirely by setting transparent divider and divider height to 0.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerAlarmsScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: fix minute wheel so style does not reset while scrolling
- Status: completed
- What was done:
  - Reworked minute wheel implementation in alarm forms to avoid style resets during scroll.
  - Kept all minute values consistently large and white for both selected and non-selected items.
  - Removed wheel border lines for cleaner glass-style block.
  - Fixed scroll synchronization loop by avoiding forced reposition while user is actively scrolling.
  - Added required Compose `@OptIn(ExperimentalFoundationApi::class)` and corrected `snapshotFlow` import.
  - Installed updated debug build to connected device (`CPH2127`) after successful compile.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerAlarmsScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
  - `.\\gradlew.bat installDebug` -> `Installed on 1 device`

### Task: fix minute wheel auto-scrolling and correct default/edit minute behavior
- Status: completed
- What was done:
  - Fixed minute wheel selected value calculation bug (removed outdated index offset in centered-item mapping).
  - Set default minute for new alarm to `1` (instead of previous `10`).
  - Ensured edit form initializes wheel from the existing alarm minute and keeps that value active.
  - Replaced form `rememberSaveable` with `remember` for alarm form fields to avoid stale restored values when reopening add/edit screens.
  - Changed wheel synchronization scroll to immediate `scrollToItem` to prevent visible self-animated rewinding on initialization.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerAlarmsScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: highlight active minute in wheel with larger golden text
- Status: completed
- What was done:
  - Added dynamic active-minute detection in wheel picker based on current centered item while scrolling.
  - Active minute is now shown with golden color.
  - Active minute font size is now slightly larger than the rest.
  - Non-active minute values keep white color and slightly smaller size.
  - Behavior is shared for both add and edit alarm forms.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/PrayerAlarmsScreen.kt`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`

### Task: add Islamic holidays block in calendar and highlight holiday dates in grid
- Status: completed
- What was done:
  - Added holiday model for calendar:
    - Eid al-Fitr (1 Shawwal),
    - Eid al-Adha (10 Dhu al-Hijjah).
  - Implemented conversion of holiday dates for the currently selected Hijri year into Gregorian dates.
  - Added a new info block under the month grid with holiday names and both dates:
    - Hijri date,
    - Gregorian date.
  - Added holiday highlighting in calendar cells while switching months:
    - holiday cells now have golden border and golden text style.
  - Added RU/EN localization strings for holiday section title and holiday names.
- Changed files:
  - `app/src/main/java/com/example/fajrapp/ui/HijriCalendarScreen.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ru/strings.xml`
  - `CHANGELOG_AGENT.md`
- Verification:
  - `.\\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL`
