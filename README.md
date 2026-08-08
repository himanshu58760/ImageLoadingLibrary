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

## Sample app

Run `:app` then try:

- **RecyclerView gallery** — XML `ImageView.load` / `clear` on recycle
- **LazyColumn/Grid gallery** — Compose `AsyncImage` (dispose cancels)
- **Preload demo** — warms cache then opens a detail screen

## Status

Layers L0–L7: core pipeline + Views/Compose adapters + sample galleries.

Concurrency: **kotlinx-coroutines only** (no RxJava APIs).
