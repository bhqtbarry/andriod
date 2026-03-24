package cn.syphotos.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.ViewerUiState
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

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
    val context = LocalContext.current
    val gallery = state.gallery.ifEmpty { state.detail?.photo?.let(::listOf).orEmpty() }
    val currentPhotoId = state.currentPhotoId ?: state.detail?.photo?.id
    val initialPage = remember(gallery, currentPhotoId) {
        gallery.indexOfFirst { it.id == currentPhotoId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { gallery.size.coerceAtLeast(1) })
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var showChrome by rememberSaveable { mutableStateOf(true) }
    var showDetails by rememberSaveable { mutableStateOf(false) }

    val pagePhoto = gallery.getOrNull(pagerState.currentPage)
    val photo = state.detail?.photo?.takeIf { it.id == pagePhoto?.id } ?: pagePhoto ?: state.detail?.photo

    LaunchedEffect(pagerState.currentPage, gallery) {
        val currentId = gallery.getOrNull(pagerState.currentPage)?.id ?: return@LaunchedEffect
        onPhotoChanged(currentId)
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    LaunchedEffect(gallery, pagerState.currentPage) {
        val imageLoader = SingletonImageLoader.get(context)
        listOfNotNull(
            gallery.getOrNull(pagerState.currentPage - 1),
            gallery.getOrNull(pagerState.currentPage),
            gallery.getOrNull(pagerState.currentPage + 1),
        ).forEach { item ->
            val cached = state.photosById[item.id]
            listOf(
                cached?.thumbUrl.orEmpty(),
                item.thumbUrl,
                cached?.originalUrl.orEmpty(),
                item.originalUrl,
            ).filter { it.isNotBlank() }.forEach { url ->
                imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = scale == 1f,
                beyondViewportPageCount = 1,
            ) { page ->
                val item = gallery.getOrNull(page)
                val cached = item?.let { state.photosById[it.id] }
                val matchedDetail = state.detail?.takeIf { it.photo.id == item?.id }
                val thumbUrl = listOf(
                    cached?.thumbUrl.orEmpty(),
                    item?.thumbUrl.orEmpty(),
                ).firstOrNull { it.isNotBlank() }.orEmpty()
                val originalUrl = listOf(
                    matchedDetail?.originalUrl.orEmpty(),
                    cached?.originalUrl.orEmpty(),
                    item?.originalUrl.orEmpty(),
                ).firstOrNull { it.isNotBlank() }.orEmpty()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(page, originalUrl, thumbUrl, scale) {
                            detectTapGestures(
                                onTap = { showChrome = !showChrome },
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2f
                                    }
                                },
                            )
                        }
                        .pointerInput(page, originalUrl, scale) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                if (nextScale == 1f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = nextScale
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (thumbUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(thumbUrl).build(),
                            contentDescription = item?.title ?: fallbackPhotoTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                },
                            contentScale = ContentScale.Fit,
                        )
                    }
                    if (originalUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(originalUrl).build(),
                            contentDescription = item?.title ?: fallbackPhotoTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                },
                            contentScale = ContentScale.Fit,
                        )
                    }
                    if (thumbUrl.isBlank() && originalUrl.isBlank()) {
                        Text(
                            text = "Image unavailable",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
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
                                text = "${pagerState.currentPage + 1} / ${gallery.size}",
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
