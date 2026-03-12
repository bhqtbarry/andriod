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

Current web service integration:
- Default `BASE_URL` is `https://www.syphotos.cn/api/app/v1/`
- Override with Gradle property: `-PSY_PHOTOS_BASE_URL=https://YOUR_HOST/api/app/v1/`
- Integrated endpoints:
  - `GET /photos`
  - `GET /photos/{photoId}`
  - `GET /map/clusters`
  - `GET /upload/config`
  - `GET /me`
  - `GET /me/likes`
  - `GET /me/pending`
  - `GET /me/rejected`
  - `GET /me/devices`
