# :bliss-search-providers

Owns built-in web suggestion provider transport and response parsing.

Owned packages:
- `foundation.e.bliss.suggestions`
- `foundation.e.bliss.suggestions.duckduckgo`
- `foundation.e.bliss.suggestions.qwant`

Public API:
- `SuggestionProvider`
- `SuggestionsResult`
- `Providers`
- `BaseSuggestionProvider`
- `DuckDuckGoProvider`
- `QwantProvider`

Allowed dependencies:
- `:bliss-core-contracts`
- Kotlin coroutines
- Kotlin serialization JSON
- OkHttp and logging-interceptor

Forbidden dependencies:
- `com.android.launcher3.*`
- `LauncherPrefs`
- `LauncherComponentProvider`
- App `R`
- Android View/UI widgets

App-root adapters and consumers:
- `foundation.e.bliss.suggestions.SearchSuggestionUtil` selects providers from preferences/default engine state.
- `foundation.e.bliss.suggestions.BlissInput` renders provider results.
- `foundation.e.bliss.suggestions.CustomUrlProvider` stays app-owned because it reads URL/timeouts/counts from `LauncherPrefs`.

Validation:
```bash
ANDROID_HOME=/data/android-sdk ./gradlew :bliss-search-providers:assembleDebug :bliss-search-providers:testDebugUnitTest
```
