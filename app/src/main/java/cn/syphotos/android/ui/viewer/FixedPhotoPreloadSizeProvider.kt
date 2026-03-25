package cn.syphotos.android.ui.viewer

import com.bumptech.glide.ListPreloader

class FixedPhotoPreloadSizeProvider<T>(
    width: Int,
    height: Int,
) : ListPreloader.PreloadSizeProvider<T> {
    private val size = intArrayOf(width, height)

    override fun getPreloadSize(
        item: T,
        adapterPosition: Int,
        perItemPosition: Int,
    ): IntArray? = size
}
