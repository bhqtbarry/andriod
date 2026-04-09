package cn.syphotos.android

import android.app.Application
import cn.syphotos.android.image.PersistentImageLoader
import cn.syphotos.android.image.PersistentImageStore
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer

class SyPhotosApplication : Application() {
    val persistentImageStore by lazy { PersistentImageStore(this) }
    val persistentImageLoader by lazy { PersistentImageLoader(this, persistentImageStore) }

    override fun onCreate() {
        super.onCreate()
        SDKInitializer.setAgreePrivacy(this, true)
        SDKInitializer.initialize(this)
        SDKInitializer.setCoordType(CoordType.BD09LL)
    }
}
