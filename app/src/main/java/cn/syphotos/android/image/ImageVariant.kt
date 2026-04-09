package cn.syphotos.android.image

enum class ImageVariant(
    val folderName: String,
    val fileSuffix: String,
) {
    THUMBNAIL(folderName = "thumbs", fileSuffix = "thumb"),
    ORIGINAL(folderName = "originals", fileSuffix = "original"),
}
