package cn.syphotos.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.viewpager2.widget.ViewPager2
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ViewerPhotoState
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.ViewerUiState
import cn.syphotos.android.ui.viewer.PhotoPagerAdapter
import cn.syphotos.android.ui.viewer.PhotoPreloader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    state: ViewerUiState,
    fallbackPhotoTitle: String,
    onToggleLike: (Long) -> Unit,
    onPhotoChanged: (Long) -> Unit,
    onApplyFilter: (PhotoFilter) -> Unit,
) {
    val strings = LocalAppStrings.current
    val gallery = state.gallery.ifEmpty { state.detail?.photo?.let(::listOf).orEmpty() }
    val currentPhotoId = state.currentPhotoId ?: state.detail?.photo?.id
    val initialPage = remember(gallery, currentPhotoId) {
        gallery.indexOfFirst { it.id == currentPhotoId }.takeIf { it >= 0 } ?: 0
    }
    var currentPage by rememberSaveable(gallery, currentPhotoId) { mutableIntStateOf(initialPage) }
    var showChrome by rememberSaveable { mutableStateOf(true) }
    var showDetails by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialPage) {
        currentPage = initialPage
    }

    val pagePhoto = gallery.getOrNull(currentPage)
    val photo = state.detail?.photo?.takeIf { it.id == pagePhoto?.id } ?: pagePhoto ?: state.detail?.photo

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (gallery.isNotEmpty()) {
                ViewerPager(
                    gallery = gallery,
                    currentPhotoId = currentPhotoId,
                    photosById = state.photosById,
                    onPhotoChanged = {
                        currentPage = gallery.indexOfFirst { photoItem -> photoItem.id == it }.takeIf { index -> index >= 0 }
                            ?: currentPage
                        onPhotoChanged(it)
                    },
                    onToggleChrome = { showChrome = !showChrome },
                )
            }

            if (showChrome && gallery.isNotEmpty()) {
                Surface(
                    color = Color(0x66000000),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = photo?.title ?: fallbackPhotoTitle,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${currentPage + 1} / ${gallery.size}",
                                color = Color(0xCCFFFFFF),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(onClick = { showDetails = true }) {
                            Text("信息")
                        }
                    }
                }

                Surface(
                    color = Color(0x66000000),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = listOfNotNull(
                                photo?.airline?.takeIf { it.isNotBlank() },
                                photo?.aircraftModel?.takeIf { it.isNotBlank() },
                                photo?.registration?.takeIf { it.isNotBlank() },
                            ).joinToString("  "),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = photo?.location.orEmpty(),
                            color = Color(0xCCFFFFFF),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (state.isLoading && state.detail == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (gallery.isEmpty() && !state.isLoading) {
                Text(
                    text = "Image unavailable",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }

    if (showDetails && photo != null) {
        ModalBottomSheet(onDismissRequest = { showDetails = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(photo.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DetailLinkRow("作者", photo.author, onClick = null)
                DetailLinkRow("航司", photo.airline, onClick = { onApplyFilter(PhotoFilter(airline = photo.airline)) })
                DetailLinkRow("型号", photo.aircraftModel, onClick = { onApplyFilter(PhotoFilter(aircraftModel = photo.aircraftModel)) })
                DetailLinkRow("注册号", photo.registration, onClick = { onApplyFilter(PhotoFilter(registration = photo.registration)) })
                DetailLinkRow("拍摄地点", photo.location, onClick = { onApplyFilter(PhotoFilter(locationCode = photo.location)) })
                DetailLinkRow("相机", photo.camera, onClick = { onApplyFilter(PhotoFilter(camera = photo.camera)) })
                DetailLinkRow("镜头", photo.lens, onClick = { onApplyFilter(PhotoFilter(lens = photo.lens)) })
                DetailLinkRow("拍摄时间", state.detail?.takeIf { it.photo.id == photo.id }?.shootingTime.orEmpty(), onClick = null)
                DetailLinkRow("焦距", state.detail?.takeIf { it.photo.id == photo.id }?.focalLength?.let { "$it mm" }.orEmpty(), onClick = null)
                DetailLinkRow("ISO", state.detail?.takeIf { it.photo.id == photo.id }?.iso.orEmpty(), onClick = null)
                DetailLinkRow("光圈", state.detail?.takeIf { it.photo.id == photo.id }?.aperture?.let { "f/$it" }.orEmpty(), onClick = null)
                DetailLinkRow("快门", state.detail?.takeIf { it.photo.id == photo.id }?.shutter.orEmpty(), onClick = null)
                Button(onClick = { onToggleLike(photo.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (photo.liked) strings.unlike else strings.like)
                }
                Button(onClick = { showDetails = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.back)
                }
            }
        }
    }
}

@Composable
private fun ViewerPager(
    gallery: List<PhotoItem>,
    currentPhotoId: Long?,
    photosById: Map<Long, ViewerPhotoState>,
    onPhotoChanged: (Long) -> Unit,
    onToggleChrome: () -> Unit,
) {
    val updatedPhotoChanged by rememberUpdatedState(onPhotoChanged)
    val updatedToggleChrome by rememberUpdatedState(onToggleChrome)
    val updatedPhotosById by rememberUpdatedState(photosById)
    val initialPage = remember(gallery, currentPhotoId) {
        gallery.indexOfFirst { it.id == currentPhotoId }.takeIf { it >= 0 } ?: 0
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        factory = { context ->
            val adapter = PhotoPagerAdapter(
                items = gallery,
                photosById = photosById,
                onTap = { updatedToggleChrome() },
            )
            val preloader = PhotoPreloader(
                context = context,
                items = gallery,
                photoStateProvider = { id -> updatedPhotosById[id] },
            )
            ViewPager2(context).apply {
                offscreenPageLimit = 1
                this.adapter = adapter
                setCurrentItem(initialPage, false)
                registerOnPageChangeCallback(
                    object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            val current = gallery.getOrNull(position) ?: return
                            updatedPhotoChanged(current.id)
                            preloader.preloadAround(position)
                        }
                    },
                )
                preloader.preloadAround(initialPage)
                tag = preloader
            }
        },
        update = { pager ->
            (pager.adapter as? PhotoPagerAdapter)?.updateItems(gallery, photosById)
            val targetIndex = gallery.indexOfFirst { it.id == currentPhotoId }.takeIf { it >= 0 } ?: 0
            if (gallery.isNotEmpty() && pager.currentItem != targetIndex && gallery.getOrNull(pager.currentItem)?.id != currentPhotoId) {
                pager.setCurrentItem(targetIndex, false)
            }
            val preloader = pager.tag as? PhotoPreloader
            preloader?.preloadAround(pager.currentItem)
        },
    )
}

@Composable
private fun DetailLinkRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
        )
    }
}
