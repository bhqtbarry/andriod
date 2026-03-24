package cn.syphotos.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import cn.syphotos.android.ui.i18n.AppLanguage
import cn.syphotos.android.ui.screen.AllPhotosScreen
import cn.syphotos.android.ui.screen.CategoryScreen
import cn.syphotos.android.ui.screen.MapScreen
import cn.syphotos.android.ui.screen.MyScreen
import cn.syphotos.android.ui.screen.PhotoViewerScreen
import cn.syphotos.android.ui.screen.UploadScreen
import cn.syphotos.android.ui.state.AppViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    selectedPhotoId: Long?,
    onOpenPhoto: (Long) -> Unit,
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val viewModel: AppViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination = AppDestination.AllPhotos.route,
        modifier = modifier,
    ) {
        composable(AppDestination.AllPhotos.route) {
            AllPhotosScreen(
                state = viewModel.uiState,
                onFilterChange = viewModel::updateFilter,
                suggestionsByField = viewModel.uiState.suggestionState.itemsByField,
                onRequestSuggestions = viewModel::requestSuggestions,
                onClearSuggestions = viewModel::clearSuggestions,
                onOpenPhoto = { photoId ->
                    viewModel.prefetchPhotoDetail(photoId)
                    onOpenPhoto(photoId)
                },
                onToggleLike = viewModel::toggleLike,
            )
        }
        composable(AppDestination.Map.route) {
            MapScreen(
                state = viewModel.uiState,
                onApplyMapSelection = {
                    viewModel.updateFilter(viewModel.uiState.photoFilter.copy(locationCode = it))
                    navController.navigate(AppDestination.AllPhotos.route)
                },
            )
        }
        composable(AppDestination.Upload.route) {
            UploadScreen(
                state = viewModel.uiState.uploadState,
                onChooseImage = viewModel::updateUploadSelection,
                onDraftChange = viewModel::updateUploadDraft,
                onSubmit = viewModel::submitUpload,
            )
        }
        composable(AppDestination.Category.route) {
            CategoryScreen(
                state = viewModel.uiState.categoryState,
                onSelectAirline = { airline ->
                    viewModel.updateFilter(viewModel.uiState.photoFilter.copy(airline = airline))
                    navController.navigate(AppDestination.AllPhotos.route)
                },
            )
        }
        composable(AppDestination.My.route) {
            MyScreen(
                state = viewModel.uiState.myState,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected,
                onLogin = viewModel::login,
                onLogout = viewModel::logout,
            )
        }
        composable(
            route = AppDestination.PhotoViewer.route,
            arguments = listOf(navArgument("photoId") { type = NavType.LongType }),
            deepLinks = listOf(navDeepLink { uriPattern = "https://syphotos.cn/photo/{photoId}" }),
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: selectedPhotoId ?: return@composable
            PhotoViewerScreen(
                state = viewModel.uiState.viewerState,
                fallbackPhotoTitle = viewModel.findPhoto(photoId).title,
                onToggleLike = { viewModel.toggleLike(photoId) },
                onApplyFilter = { filter ->
                    viewModel.updateFilter(filter)
                    navController.popBackStack()
                    navController.navigate(AppDestination.AllPhotos.route)
                },
            )
        }
    }
}
