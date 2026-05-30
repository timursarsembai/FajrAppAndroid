# FajrApp Android — Security Audit Report

Дата аудита: 2026-05-30  
Проверенная ревизия: `3324f60`  
Область: статический анализ кода/конфигурации + Android Lint (`lintDebug`)

## Итог
- Критических RCE/утечек секретов в коде не выявлено.
- Выявлены уязвимости и security gaps, требующие исправления до production-релиза.
- Наиболее важные: backup приватных данных, отсутствие permission-check перед уведомлениями, экспортированный Boot receiver без ограничивающего permission.

## Найденные проблемы (по убыванию критичности)

### 1) Backup включает все SharedPreferences (утечка приватных данных через cloud/device backup)
Severity: **High**  
Риск:
- В backup попадают координаты/названия локаций и пользовательские настройки (включая будильники и параметры уведомлений), что повышает риск утечки приватных данных при компрометации резервных копий.

Доказательства:
- [AndroidManifest.xml](E:/Development/Android/FajrApp/app/src/main/AndroidManifest.xml): `android:allowBackup="true"`, подключены `data_extraction_rules` и `fullBackupContent`.
- [backup_rules.xml](E:/Development/Android/FajrApp/app/src/main/res/xml/backup_rules.xml): `<include domain="sharedpref" path="."/>`
- [data_extraction_rules.xml](E:/Development/Android/FajrApp/app/src/main/res/xml/data_extraction_rules.xml): backup всего `sharedpref`.
- [PreferencesManager.kt](E:/Development/Android/FajrApp/app/src/main/java/com/example/fajrapp/data/PreferencesManager.kt): хранение локации/настроек в SharedPreferences.

Рекомендация:
- Либо отключить backup полностью для приложения с приватными данными, либо исключить чувствительные ключи/файлы из backup.
- Для чувствительных данных рассмотреть EncryptedSharedPreferences.

---

### 2) Отправка уведомления без проверки POST_NOTIFICATIONS (runtime crash / DoS-поведение)
Severity: **Medium**  
Риск:
- На Android 13+ при отозванном permission вызов уведомления может падать (SecurityException), что ломает сценарий будильника/уведомления.

Доказательства:
- Android Lint: `MissingPermission` (lint report).
- [PrayerAlarmReceiver.kt](E:/Development/Android/FajrApp/app/src/main/java/com/example/fajrapp/data/PrayerAlarmReceiver.kt): вызов `NotificationManagerCompat.from(context).notify(...)` без runtime-check.

Рекомендация:
- Перед `notify(...)` проверять `POST_NOTIFICATIONS` permission.
- Добавить безопасный fallback (не падать, а логично пропускать уведомление/перезапланировать).

---

### 3) Экспортированный Boot Receiver без permission-ограничения
Severity: **Medium**  
Риск:
- `PrayerAlarmBootReceiver` экспортирован, и внешний app может отправлять explicit broadcast в receiver, провоцируя лишний `scheduleAllEnabled()` (battery/behavior abuse).

Доказательства:
- [AndroidManifest.xml](E:/Development/Android/FajrApp/app/src/main/AndroidManifest.xml):  
  `android:name=".data.PrayerAlarmBootReceiver"` + `android:exported="true"` + action `BOOT_COMPLETED`.
- [PrayerAlarmBootReceiver.kt](E:/Development/Android/FajrApp/app/src/main/java/com/example/fajrapp/data/PrayerAlarmBootReceiver.kt): принимает broadcast и запускает рескейджулинг.

Рекомендация:
- Для receiver добавить `android:permission="android.permission.RECEIVE_BOOT_COMPLETED"` (или другой подход ограничения внешних отправителей).
- В коде дополнительно проверять `intent.action` (уже сделано) и, при необходимости, защитный rate-limit/guard.

---

### 4) Недостаточное hardening release-сборки (обфускация выключена)
Severity: **Low**  
Риск:
- Упрощает reverse engineering, статический анализ и автоматизацию атак на логику приложения.

Доказательства:
- [build.gradle.kts](E:/Development/Android/FajrApp/app/build.gradle.kts): `isMinifyEnabled = false` в `release`.
- [proguard-rules.pro](E:/Development/Android/FajrApp/app/proguard-rules.pro): практически пустой.

Рекомендация:
- Включить R8/обфускацию для release, аккуратно настроить keep-rules.

---

### 5) Избыточное permission: INTERNET без фактического использования сетевого слоя
Severity: **Low**  
Риск:
- Лишняя площадь attack surface и вопросы privacy/compliance при ревью.

Доказательства:
- [AndroidManifest.xml](E:/Development/Android/FajrApp/app/src/main/AndroidManifest.xml): `android.permission.INTERNET`.
- Поиск по коду не выявил сетевых клиентов/HTTP-вызовов.

Рекомендация:
- Удалить INTERNET permission, если сетевой функционал действительно не нужен.

## Что проверено дополнительно
- `PendingIntent` используется с `FLAG_IMMUTABLE` (это хорошо):
  - [PrayerAlarmScheduler.kt](E:/Development/Android/FajrApp/app/src/main/java/com/example/fajrapp/data/PrayerAlarmScheduler.kt)
  - [PrayerAlarmReceiver.kt](E:/Development/Android/FajrApp/app/src/main/java/com/example/fajrapp/data/PrayerAlarmReceiver.kt)
- `PrayerAlarmReceiver` не экспортирован (`exported=false`) — корректно.
- WebView/JS interfaces/cleartext-HTTP в коде не обнаружены.
- Жестко зашитых API keys/secrets в repo не обнаружено.

## Ограничения аудита
- Не выполнялся внешний CVE-аудит зависимостей через специализированные базы (OSV/Snyk/Dependabot report).
- Не проводился динамический pentest/runtime instrumentation beyond lint/smoke.

## Рекомендуемый план исправлений
1. Закрыть High: переработать backup-стратегию для SharedPreferences.  
2. Закрыть Medium: permission-check для уведомлений + защитить BootReceiver permission-ом.  
3. Закрыть Low: включить release minify/обфускацию, удалить лишний INTERNET permission.  
4. Повторный security-прогон (lint + regression tests + dependency CVE scan).

---

## Статус после remediation (2026-05-30)

### Закрыто
- [x] **Backup privacy risk**: `android:allowBackup` переключен в `false`; backup shared preferences отключен.
- [x] **MissingPermission notifications**: добавлен runtime gate `canPostNotifications(...)` + безопасная обработка `SecurityException`.
- [x] **Exported BootReceiver hardening**: добавлен `android:permission="android.permission.RECEIVE_BOOT_COMPLETED"` для `PrayerAlarmBootReceiver`.
- [x] **Release hardening**: включены `isMinifyEnabled = true`, `isShrinkResources = true`; release сборка проходит.
- [x] **Excess INTERNET permission**: удален из manifest.

### Проверка после исправлений
- `testDebugUnitTest` — успешно.
- `assembleRelease` — успешно (включая R8/shrinkResources).
- `lintDebug` по-прежнему содержит множество non-security замечаний проекта (например, `NewApi`), но исходный security-issue `MissingPermission` в `PrayerAlarmReceiver` закрыт.

### Что еще рекомендуется
- Провести отдельный CVE-аудит зависимостей (OSV/Dependabot/Snyk).
- Отдельно пройтись по оставшимся lint-ошибкам качества/совместимости (они не классифицированы как security-критичные в этом отчете).
