# SY Photos Android

Native Android v1 scaffold generated from `android_app_handoff.md`.

Current implementation:
- Jetpack Compose single-module app
- Bottom navigation with `All Photos`, `Map`, `Upload`, `Category`, `My`
- Photo viewer route with HTTPS deep link pattern `https://syphotos.cn/photo/{photoId}`
- Fake in-memory repository matching the handoff domains for UI scaffolding

Environment requirements:
- Android Studio with Android SDK 35
- JDK 17

Next integration step:
- Replace `FakeSyPhotosRepository` with `/api/app/v1/...` backend services
