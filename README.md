# SY Photos Android

SY Photos Android client, single-module native Android app based on Kotlin + Jetpack Compose.

## Current Status

- Architecture: single `app` module
- UI shell: Jetpack Compose
- Heavy image surfaces:
  - home photo grid uses native `RecyclerView + GridLayoutManager`
  - full-screen viewer uses native `ViewPager2 + PhotoView`
- Bottom navigation:
  - `All Photos`
  - `Map`
  - `Upload`
  - `Category`
  - `My`
- Deep link route:
  - `https://syphotos.cn/photo/{photoId}`

## Environment

- Android SDK: `35`
- Min SDK: `26`
- Java target: `17`
- Kotlin plugin: root `build.gradle.kts` currently declares `2.2.10`

Confirmed local JDK on this machine:

- `C:\software\java\jdk-17.0.18`

Recommended `JAVA_HOME`:

- `C:\software\java\jdk-17.0.18`

Temporary PowerShell setup:

```powershell
$env:JAVA_HOME='C:\software\java\jdk-17.0.18'
$env:Path='C:\software\java\jdk-17.0.18\bin;' + $env:Path
```

Persistent user-level setup:

```powershell
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\software\java\jdk-17.0.18', 'User')
[System.Environment]::SetEnvironmentVariable(
  'Path',
  'C:\software\java\jdk-17.0.18\bin;' + [System.Environment]::GetEnvironmentVariable('Path', 'User'),
  'User'
)
```

## Build Notes

App module config is in [app/build.gradle.kts](/c:/wwwroot2/andriod/app/build.gradle.kts).

Root plugin versions are in [build.gradle.kts](/c:/wwwroot2/andriod/build.gradle.kts):

- `com.android.application` version `9.1.0`
- `org.jetbrains.kotlin.android` version `2.2.10`
- `org.jetbrains.kotlin.plugin.compose` version `2.2.10`

Known issue seen on this machine:

- Gradle resolved JDK 17 correctly after fixing `JAVA_HOME`
- build can still fail before compilation if Android Gradle Plugin `9.1.0` is not resolvable from configured repositories
- if that happens, fix plugin version/repository compatibility first before debugging app code

## Base URL

Default API base URL:

- `https://www.syphotos.cn/api/app/v1/`

Override with Gradle property:

```powershell
./gradlew.bat assembleDebug -PSY_PHOTOS_BASE_URL=https://YOUR_HOST/api/app/v1/
```

The app module writes this into `BuildConfig.SY_PHOTOS_BASE_URL`.

## Current App Data Sources

Implemented repository:

- [WebSyPhotosRepository.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/data/WebSyPhotosRepository.kt)

Important endpoints already wired:

- `GET /photos/feed.php`
- `GET /photos/detail.php?id={photoId}`
- `GET /map/clusters.php`
- `GET /me/summary.php`
- `GET /photos/my.php?status=all|pending|rejected`
- `GET /photos/likes.php`
- `GET /auth/devices.php`

## Image Viewer And Cache

### Viewer structure

Relevant files:

- [AllPhotosScreen.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/screen/AllPhotosScreen.kt)
- [PhotoGridRecyclerView.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/gallery/PhotoGridRecyclerView.kt)
- [PhotoViewerScreen.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/screen/PhotoViewerScreen.kt)
- [GalleryViewerPagerView.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/gallery/GalleryViewerPagerView.kt)
- [GalleryZoomPhotoView.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/gallery/GalleryZoomPhotoView.kt)

Current intent:

- home thumbnails scroll smoothly in a native grid
- full-screen viewer supports:
  - horizontal paging
  - double tap zoom
  - pinch zoom
  - panning while zoomed

Gesture behavior is still an active tuning area. If image paging feels wrong, inspect:

- [GalleryZoomPhotoView.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/gallery/GalleryZoomPhotoView.kt)
- [GalleryViewerPagerView.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/gallery/GalleryViewerPagerView.kt)

### Permanent local cache

Permanent image cache is separate from Glide memory/disk cache.

Core files:

- [PersistentImageStore.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/image/PersistentImageStore.kt)
- [PersistentImageLoader.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/image/PersistentImageLoader.kt)
- [ImageVariant.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/image/ImageVariant.kt)

Cache root:

- `filesDir/persistent-photo-cache`

Subdirectories:

- thumbnails: `filesDir/persistent-photo-cache/thumbs`
- originals: `filesDir/persistent-photo-cache/originals`

File naming:

- thumbnail: `${photoId}_thumb.img`
- original: `${photoId}_original.img`

Behavior:

- cache key is photo `id + variant`
- thumbnails and originals are stored separately
- app restart does not clear this cache
- duplicate downloads are prevented with an inflight map in `PersistentImageStore`
- viewer prefetches nearby originals

## Home Grid Notes

Thumbnail grid now uses a native RecyclerView-backed view instead of Compose `LazyVerticalGrid`.

Relevant files:

- [PhotoGridAdapter.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/gallery/PhotoGridAdapter.kt)
- [WideCropImageView.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/gallery/WideCropImageView.kt)

Current thumbnail presentation:

- 2:1 card ratio
- width fills horizontally
- vertical direction centers the image instead of hard center-crop

## Filter Panel

Shared filter panel file:

- [PhotoFilterPanel.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/screen/PhotoFilterPanel.kt)

Current behavior:

- live filter updates still happen while editing
- `应用筛选` button closes the filter panel
- `清除筛选` resets the draft and closes the panel

Used by:

- [AllPhotosScreen.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/screen/AllPhotosScreen.kt)
- [MapScreen.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/screen/MapScreen.kt)

## My Page Notes

Relevant file:

- [MyScreen.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/ui/screen/MyScreen.kt)

Current Android-side layout changes:

- work card thumbnail displays full width, without vertical crop
- metadata row displays:
  - 编号
  - 机型
  - 位置
- next row shows larger button-like status and delete action

Reference web page mentioned during this iteration:

- `C:\wwwroot2\syphotos\user_center.php`

## Application Entry

Application class:

- [SyPhotosApplication.kt](/c:/wwwroot2/andriod/app/src/main/java/cn/syphotos/android/SyPhotosApplication.kt)

It currently initializes:

- Baidu map SDK
- shared `PersistentImageStore`
- shared `PersistentImageLoader`

## Dependencies In Use

Important app dependencies from [app/build.gradle.kts](/c:/wwwroot2/andriod/app/build.gradle.kts):

- `androidx.compose.material3:material3`
- `androidx.navigation:navigation-compose`
- `androidx.recyclerview:recyclerview:1.4.0`
- `androidx.viewpager2:viewpager2:1.1.0`
- `com.github.bumptech.glide:glide:4.16.0`
- `com.github.chrisbanes:PhotoView:2.3.0`
- `com.baidu.lbsyun:BaiduMapSDK_Map:7.5.4`

## Working Habit

If future work starts in this repo, it is reasonable to read this `README.md` first because it now contains:

- confirmed local JDK location
- current image/cache architecture
- major UI rewrites already done
- known build blockers

