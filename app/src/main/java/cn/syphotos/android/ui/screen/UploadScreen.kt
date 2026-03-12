package cn.syphotos.android.ui.screen

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.syphotos.android.ui.components.GradientHero
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.UploadUiState

@Composable
fun UploadScreen(
    state: UploadUiState,
    onChooseImage: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val fileName = uri?.let { selectedUri ->
            context.contentResolver.query(selectedUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } ?: uri?.lastPathSegment
        if (!fileName.isNullOrBlank()) {
            onChooseImage(fileName)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GradientHero(
            eyebrow = strings.navUpload,
            title = strings.uploadTitle,
            subtitle = strings.uploadSubtitle,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (state.fileName.isBlank()) strings.noImageSelected else state.fileName)
                Text("Max file size: ${state.config.maxFileSizeMb} MB")
                Text("Accepted ratio: ${state.config.minAspectRatio} to ${state.config.maxAspectRatio}")
                if (state.config.exifEnabled) Text(strings.exifEnabled)
                if (state.config.watermarkEnabled) Text(strings.watermarkEnabled)
                if (state.config.registrationOcrEnabled) Text(strings.registrationOcr)
                if (state.config.uploadUrl.isNotBlank()) Text("Upload endpoint: ${state.config.uploadUrl}")
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Text(strings.retryInfo)
                Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.chooseImage)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.deletionRules)
                Text(strings.irreversibleRule)
                Text(strings.pendingRule)
                Text(strings.rejectedRule)
                Text(strings.approvedRule)
            }
        }
        state.errorMessage?.let { message ->
            Surface(
                color = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                shape = androidx.compose.material3.MaterialTheme.shapes.large,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
