package cn.syphotos.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.syphotos.android.ui.i18n.AppLanguage
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.i18n.rememberAppStrings
import cn.syphotos.android.ui.navigation.AppDestination
import cn.syphotos.android.ui.navigation.AppNavHost
import cn.syphotos.android.ui.state.AppViewModel

@Composable
fun SyPhotosApp() {
    val viewModel: AppViewModel = viewModel()
    val navController = rememberNavController()
    var selectedPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var languageCode by rememberSaveable { mutableStateOf(AppLanguage.ZH.code) }
    val selectedLanguage = AppLanguage.fromCode(languageCode)
    val strings = rememberAppStrings(selectedLanguage)
    val versionState = viewModel.uiState.versionState
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
        when {
            versionState.isChecking && !versionState.hasChecked -> {
                VersionCheckingScreen()
            }

            versionState.requiresUpgrade -> {
                UpgradeRequiredScreen(
                    message = versionState.result?.message?.ifBlank { "当前版本过低" } ?: "当前版本过低",
                    upgradeUrl = versionState.result?.upgradeUrl ?: "https://www.syphotos.cn",
                )
            }

            else -> {
                Scaffold(
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
                        viewModel = viewModel,
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
    }
}

@Composable
private fun VersionCheckingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun UpgradeRequiredScreen(
    message: String,
    upgradeUrl: String,
) {
    val context = LocalContext.current
    LaunchedEffect(upgradeUrl) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(upgradeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(message, style = MaterialTheme.typography.headlineSmall)
                Text("当前版本过低，请前往官网更新。", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(upgradeUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    },
                ) {
                    Text("打开官网")
                }
            }
        }
    }
}
