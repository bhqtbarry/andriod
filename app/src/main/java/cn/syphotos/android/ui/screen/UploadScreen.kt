package cn.syphotos.android.ui.screen

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.syphotos.android.ui.common.GlideFitWidthImage
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.UploadUiState
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    state: UploadUiState,
    onChooseImage: (String, String) -> Unit,
    onDraftChange: ((UploadUiState) -> UploadUiState) -> Unit,
    onRegistrationChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var showPreviewDialog by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val fileName = uri?.let { selectedUri ->
            context.contentResolver.query(selectedUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } ?: uri?.lastPathSegment
        if (uri != null) {
            onChooseImage(
                uri.toString(),
                fileName?.takeIf { it.isNotBlank() } ?: (uri.lastPathSegment ?: "selected-image.jpg"),
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.fileName.isBlank()) strings.chooseImage else state.fileName)
                    }
                    if (state.selectedImageUri.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp))
                                .clickable { showPreviewDialog = true },
                        ) {
                            GlideFitWidthImage(
                                url = state.selectedImageUri,
                                contentDescription = state.fileName,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            UploadWatermarkOverlay(
                                state = state,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                    }
                    FormTextField(
                        value = state.title,
                        label = strings.uploadFieldTitle,
                        onValueChange = { value -> onDraftChange { current -> current.copy(title = value) } },
                    )
                    FormTextField(
                        state.registrationNumber,
                        strings.uploadFieldRegistration,
                        onRegistrationChange,
                    )
                    state.registrationLookupMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    FormTextField(state.aircraftModel, strings.uploadFieldAircraftModel, { value -> onDraftChange { current -> current.copy(aircraftModel = value) } })
                    FormTextField(state.airline, strings.uploadFieldAirline, { value -> onDraftChange { current -> current.copy(airline = value) } })
                    FormTextField(
                        state.shootingTime,
                        strings.uploadFieldShootingTime,
                        { value -> onDraftChange { current -> current.copy(shootingTime = value) } },
                        placeholder = strings.uploadFieldShootingTimeHint,
                    )
                    FormTextField(
                        state.shootingLocation,
                        strings.uploadFieldShootingLocation,
                        { value -> onDraftChange { current -> current.copy(shootingLocation = value.uppercase()) } },
                        placeholder = strings.uploadFieldShootingLocationHint,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FormTextField(
                            value = state.cameraModel,
                            label = strings.uploadFieldCamera,
                            onValueChange = { value -> onDraftChange { current -> current.copy(cameraModel = value) } },
                            modifier = Modifier.weight(1f),
                        )
                        FormTextField(
                            value = state.lensModel,
                            label = strings.uploadFieldLens,
                            onValueChange = { value -> onDraftChange { current -> current.copy(lensModel = value) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FormTextField(
                            value = state.focalLength,
                            label = "Focal Length",
                            onValueChange = { value -> onDraftChange { current -> current.copy(focalLength = value) } },
                            modifier = Modifier.weight(1f),
                        )
                        FormTextField(
                            value = state.iso,
                            label = "ISO",
                            onValueChange = { value -> onDraftChange { current -> current.copy(iso = value) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FormTextField(
                            value = state.aperture,
                            label = "Aperture",
                            onValueChange = { value -> onDraftChange { current -> current.copy(aperture = value) } },
                            modifier = Modifier.weight(1f),
                        )
                        FormTextField(
                            value = state.shutter,
                            label = "Shutter",
                            onValueChange = { value -> onDraftChange { current -> current.copy(shutter = value) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    state.exifMessage?.let { exifMessage ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(exifMessage, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(strings.uploadWatermarkSettings, style = MaterialTheme.typography.titleMedium)
                    SliderBlock(
                        label = strings.uploadWatermarkSize,
                        valueText = "${state.watermarkSize}%",
                        value = state.watermarkSize.toFloat(),
                        valueRange = 5f..50f,
                        steps = 44,
                        onValueChange = { value -> onDraftChange { current -> current.copy(watermarkSize = value.toInt()) } },
                    )
                    SliderBlock(
                        label = strings.uploadWatermarkOpacity,
                        valueText = "${state.watermarkOpacity}%",
                        value = state.watermarkOpacity.toFloat(),
                        valueRange = 10f..100f,
                        steps = 17,
                        onValueChange = { value -> onDraftChange { current -> current.copy(watermarkOpacity = value.toInt()) } },
                    )
                    ChipGroup(
                        title = strings.uploadWatermarkColor,
                        options = listOf("white" to strings.uploadColorWhite, "black" to strings.uploadColorBlack),
                        selected = state.watermarkColor,
                        onSelect = { value -> onDraftChange { current -> current.copy(watermarkColor = value) } },
                    )
                    ChipGroup(
                        title = strings.uploadAuthorStyle,
                        options = listOf("default" to strings.uploadStyleDefault, "simple" to strings.uploadStyleSimple, "bold" to strings.uploadStyleBold),
                        selected = state.watermarkAuthorStyle,
                        onSelect = { value -> onDraftChange { current -> current.copy(watermarkAuthorStyle = value) } },
                    )
                    ChipGroup(
                        title = strings.uploadWatermarkPosition,
                        options = listOf(
                            "top-left" to strings.uploadPositionTopLeft,
                            "top-center" to strings.uploadPositionTopCenter,
                            "top-right" to strings.uploadPositionTopRight,
                            "middle-left" to strings.uploadPositionMiddleLeft,
                            "middle-center" to strings.uploadPositionMiddleCenter,
                            "middle-right" to strings.uploadPositionMiddleRight,
                            "bottom-left" to strings.uploadPositionBottomLeft,
                            "bottom-center" to strings.uploadPositionBottomCenter,
                            "bottom-right" to strings.uploadPositionBottomRight,
                        ),
                        selected = state.watermarkPosition,
                        onSelect = { value -> onDraftChange { current -> current.copy(watermarkPosition = value) } },
                    )
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.uploadTermsTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = strings.uploadTermsDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.allowUse,
                            onCheckedChange = { checked -> onDraftChange { current -> current.copy(allowUse = checked) } },
                        )
                    }
                    Button(onClick = onSubmit, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(strings.uploadSubmit)
                        }
                    }
                }
            }
        }
        item {
            state.errorMessage?.let { message -> MessageCard(message = message, error = true) }
            state.successMessage?.let { message -> MessageCard(message = message, error = false) }
        }
    }

    if (showPreviewDialog && state.selectedImageUri.isNotBlank()) {
        UploadImagePreviewDialog(
            imageUri = state.selectedImageUri,
            state = state,
            onDismiss = { showPreviewDialog = false },
        )
    }
}

@Composable
private fun FormTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder) }),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
    )
}

@Composable
private fun SliderBlock(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: String,
    error: Boolean,
) {
    Surface(
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun UploadImagePreviewDialog(
    imageUri: String,
    state: UploadUiState,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PhotoView(context).apply {
                        minimumScale = 1f
                        mediumScale = 2f
                        maximumScale = 5f
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { photoView ->
                    Glide.with(photoView)
                        .load(imageUri)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .fitCenter()
                        .dontAnimate()
                        .into(photoView)
                },
            )
            UploadWatermarkOverlay(
                state = state,
                modifier = Modifier.matchParentSize(),
            )
            Text(
                text = "点击空白处关闭，可双指缩放",
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .clickable { onDismiss() },
                color = Color(0x55000000),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = "关闭",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun UploadWatermarkOverlay(
    state: UploadUiState,
    modifier: Modifier = Modifier,
) {
    val watermarkText = state.registrationNumber.ifBlank {
        state.title.ifBlank { "SYPHOTOS" }
    }
    val alignment = when (state.watermarkPosition) {
        "top-left" -> Alignment.TopStart
        "top-center" -> Alignment.TopCenter
        "top-right" -> Alignment.TopEnd
        "middle-left" -> Alignment.CenterStart
        "middle-center" -> Alignment.Center
        "middle-right" -> Alignment.CenterEnd
        "bottom-left" -> Alignment.BottomStart
        "bottom-center" -> Alignment.BottomCenter
        else -> Alignment.BottomEnd
    }
    val textColor = if (state.watermarkColor == "black") Color.Black else Color.White
    val fontWeight = when (state.watermarkAuthorStyle) {
        "bold" -> FontWeight.Bold
        else -> FontWeight.Medium
    }
    val fontSize = watermarkFontSize(state.watermarkSize)
    val horizontalPadding = (12 + state.watermarkSize / 2).dp
    val verticalPadding = (12 + state.watermarkSize / 3).dp

    Box(modifier = modifier) {
        Text(
            text = watermarkText,
            color = textColor.copy(alpha = state.watermarkOpacity.coerceIn(10, 100) / 100f),
            fontSize = fontSize,
            fontWeight = fontWeight,
            modifier = Modifier
                .align(alignment)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        )
    }
}

private fun watermarkFontSize(sizePercent: Int): TextUnit {
    return when {
        sizePercent <= 10 -> 14.sp
        sizePercent <= 15 -> 16.sp
        sizePercent <= 20 -> 18.sp
        sizePercent <= 25 -> 20.sp
        sizePercent <= 30 -> 22.sp
        sizePercent <= 35 -> 24.sp
        sizePercent <= 40 -> 26.sp
        sizePercent <= 45 -> 28.sp
        else -> 30.sp
    }
}
