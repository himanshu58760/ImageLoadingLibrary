# Image Loading Library

Production-oriented Android image loader (Kotlin, coroutines-only concurrency).

## Modules

| Module | Role |
|--------|------|
| `:imageloader-core` | Pipeline, caches, decode, pool, public contracts |
| `:imageloader-views` | `ImageView` helpers |
| `:imageloader-compose` | Compose `AsyncImage` |
| `:app` | Sample host |

## Build

```bash
./gradlew :imageloader-core:test
./gradlew assembleDebug
```

## Status

Leaf-first implementation. See plan for layering (L0 modules → L1 types → …).

Concurrency: **kotlinx-coroutines only** (no RxJava APIs).
