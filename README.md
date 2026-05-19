# FridgeChef Android

Native Android port of the iOS FridgeChef app, based on `recipe-ingredients-ios/docs/CROSS_PLATFORM_SPEC.md`.

## Stack

- Kotlin + Jetpack Compose
- MVVM with `StateFlow`
- Local SQLite persistence using the portable schema from the spec
- SharedPreferences for theme and daily-pick cache
- Direct OpenAI Chat Completions calls with strict JSON schema responses

## Setup

Create `local.properties` in the repo root:

```properties
OPENAI_API_KEY=<your-openai-api-key>
```

Then build:

```bash
./gradlew :app:assembleDebug
```

The API key is read into `BuildConfig.OPENAI_API_KEY` and is ignored by git.
