package cn.syphotos.android.ui.navigation

sealed class AppDestination(val route: String) {
    data object AllPhotos : AppDestination("all_photos")
    data object Map : AppDestination("map")
    data object Upload : AppDestination("upload")
    data object Category : AppDestination("category")
    data object My : AppDestination("my")
    data object PhotoViewer : AppDestination("photo/{photoId}") {
        fun createRoute(photoId: Long): String = "photo/$photoId"
    }
}
