# i18n Translation Instructions

You are translating strings in the FeatureHub Flutter admin frontend (`admin-frontend/open_admin_app/`).

## Toolchain

- ARB template: `lib/l10n/app_en.arb`
- Chinese translations: `lib/l10n/app_zh.arb`
- Generated code: `lib/generated/l10n/app_localizations.dart` (run `flutter gen-l10n` or `flutter pub get`)
- Import in Dart: `package:open_admin_app/generated/l10n/app_localizations.dart`
- Usage: `AppLocalizations.of(context)!.keyName` / `AppLocalizations.of(context)!.keyName(param)`

## Workflow for each widget file

1. Add new keys to `app_en.arb` (English, camelCase keys)
2. Add corresponding translations to `app_zh.arb` (Simplified Chinese)
3. Update the Dart widget file — replace hardcoded strings with `AppLocalizations.of(context)!.keyName`
4. Remove `const` from any widget that calls `AppLocalizations.of(context)!`

## ARB key conventions

- camelCase, descriptive: `signInTitle`, `emailLabel`, `deleteUserContent`
- Parameterised strings use placeholders:
  ```json
  "userActivated": "User '{name}' activated!",
  "@userActivated": {
    "placeholders": { "name": { "type": "String" } }
  }
  ```
- Chinese: use `「」` corner brackets instead of `""`

## Passing `AppLocalizations` into helper methods

When `build()` delegates to a helper method that needs localised strings, pass `AppLocalizations` as a parameter:
```dart
// in build():
final l10n = AppLocalizations.of(context)!;
_handleValidation(l10n);
```

For closures/callbacks that run after `build()` returns, capture `l10n` from the outer `build` scope before the callback.

## NEVER translate: SDK enum values

These are serialised into feature flag configs consumed by FeatureHub SDKs in all languages. Keep them in English — do not add ARB keys, do not wrap in `AppLocalizations`.

### Strategy attribute well-known names
`StrategyAttributeWellKnownNames` — shown as `+ ${e.name}` buttons and card labels:
`country`, `device`, `platform`, `version`, `userkey`, `session`

### Strategy attribute values (dropdown options)
Passed directly to/from SDKs as rule values:
- `StrategyAttributeCountryName` — rendered via `_countryNameMapper`
- `StrategyAttributeDeviceName` — rendered via `_deviceNameMapper`
- `StrategyAttributePlatformName` — rendered via `_platformNameMapper`

### Rule condition operators
`RolloutStrategyAttributeConditional` — rendered via `transformStrategyAttributeConditionalValueToString`:
`EQUALS`, `NOT_EQUALS`, `INCLUDES`, `EXCLUDES`, `GREATER`, `GREATER_EQUAL`, `LESS`, `LESS_EQUAL`, `STARTS_WITH`, `ENDS_WITH`, `REGEX`, etc.

### Value field types
`RolloutStrategyFieldType` — rendered via `transformRolloutStrategyTypeFieldToString`:
`STRING`, `NUMBER`, `BOOLEAN`, `DATE`, `DATETIME`, `SEMANTIC_VERSION`, `IP_ADDRESS`

### Boolean values
`true` / `false` in the boolean attribute dropdown — string representations of SDK boolean values.

## What IS safe to translate

- Dropdown **hint** text (e.g. "Select condition", "Select Country") — placeholders, not values
- Field **labels** and **helper text** (e.g. "Custom key", "e.g. warehouse-id")
- Button labels, tooltips, validation messages, page headers, snackbars
- `FeatureValueType` display names — UI-friendly strings are decoupled from the serialised enum value