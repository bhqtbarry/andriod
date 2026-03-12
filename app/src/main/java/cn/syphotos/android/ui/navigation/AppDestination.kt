package cn.syphotos.android.ui.navigation

sealed class AppDestination(val route: String, val label: String) {
    data object AllPhotos : AppDestination("all_photos", "All Photos")
    data object Map : AppDestination("map", "Map")
    data object Upload : AppDestination("upload", "Upload")
    data object Category : AppDestination("category", "Category")
    data object My : AppDestination("my", "My")
    data object PhotoViewer : AppDestination("photo/{photoId}", "Viewer") {
        fun createRoute(photoId: Long): String = "photo/$photoId"
    }
}

