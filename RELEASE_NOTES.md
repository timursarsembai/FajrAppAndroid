# FajrApp Android — Release Notes

## 2026-05-26

### Added
- Full multi-language resource packs for supported app languages.
- Alarm settings screens and flow improvements, including per-prayer configuration.
- UI smoke tests for navigation and runtime language switching.
- Localization integrity unit test to detect mojibake/encoding corruption in `strings.xml`.

### Changed
- Calendar and holiday information layout/formatting updates.
- Runtime language switching flow and related UI behavior stabilized.
- Notification settings UX and localization coverage improved.
- Automatic language initialization from device locale on first launch (fallback to English when unsupported).

### Fixed
- Multiple locale encoding issues (mojibake) in Cyrillic and RTL locale resources.
- Incomplete translations in several settings and calendar blocks.
- Flaky instrumentation test path for language switching.

### Quality
- `testDebugUnitTest` passes.
- `compileDebugAndroidTestKotlin` passes.
- `connectedDebugAndroidTest` passes on physical device.
