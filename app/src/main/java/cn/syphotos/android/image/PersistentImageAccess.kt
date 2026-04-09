package cn.syphotos.android.image

import android.content.Context
import cn.syphotos.android.SyPhotosApplication

fun Context.persistentImageStore(): PersistentImageStore {
    return (applicationContext as SyPhotosApplication).persistentImageStore
}

fun Context.persistentImageLoader(): PersistentImageLoader {
    return (applicationContext as SyPhotosApplication).persistentImageLoader
}
