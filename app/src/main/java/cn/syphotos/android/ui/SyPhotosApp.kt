package cn.syphotos.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.syphotos.android.ui.components.LanguageSwitcher
import cn.syphotos.android.ui.i18n.AppLanguage
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.i18n.rememberAppStrings
import cn.syphotos.android.ui.navigation.AppDestination
import cn.syphotos.android.ui.navigation.AppNavHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyPhotosApp() {
    val navController = rememberNavController()
    var selectedPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var languageCode by rememberSaveable { mutableStateOf(AppLanguage.ZH.code) }
    val selectedLanguage = AppLanguage.fromCode(languageCode)
    val strings = rememberAppStrings(selectedLanguage)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isViewer = currentDestination?.route == AppDestination.PhotoViewer.route
    val topLevelDestinations = listOf(
        AppDestination.AllPhotos,
        AppDestination.Map,
        AppDestination.Upload,
        AppDestination.Category,
        AppDestination.My,
    )

    CompositionLocalProvider(LocalAppStrings provides strings) {
        Scaffold(
            topBar = {
                if (!isViewer) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(strings.pageTitle(currentDestination?.route))
                                Text(strings.pageSubtitle(currentDestination?.route))
                            }
                        },
                        actions = {
                            LanguageSwitcher(
                                strings = strings,
                                selectedLanguage = selectedLanguage,
                                onLanguageSelected = { languageCode = it.code },
                            )
                        },
                    )
                }
            },
            bottomBar = {
                if (!isViewer) {
                    NavigationBar {
                        topLevelDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = when (destination) {
                                            AppDestination.AllPhotos -> Icons.Outlined.GridView
                                            AppDestination.Map -> Icons.Outlined.Map
                                            AppDestination.Upload -> Icons.Outlined.AddCircle
                                            AppDestination.Category -> Icons.Outlined.List
                                            AppDestination.My -> Icons.Outlined.AccountCircle
                                            AppDestination.PhotoViewer -> Icons.Outlined.GridView
                                        },
                                        contentDescription = strings.navLabel(destination.route),
                                    )
                                },
                                label = { Text(strings.navLabel(destination.route)) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                selectedPhotoId = selectedPhotoId,
                onOpenPhoto = { photoId ->
                    selectedPhotoId = photoId
                    navController.navigate(AppDestination.PhotoViewer.createRoute(photoId))
                },
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { languageCode = it.code },
            )
        }
    }
}
