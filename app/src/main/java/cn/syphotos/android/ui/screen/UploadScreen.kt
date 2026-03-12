package cn.syphotos.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.syphotos.android.ui.components.GradientHero
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.UploadDraftUiState

@Composable
fun UploadScreen(state: UploadDraftUiState) {
    val strings = LocalAppStrings.current
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
                Text(strings.ratioRule)
                if (state.exifEnabled) Text(strings.exifEnabled)
                if (state.watermarkEnabled) Text(strings.watermarkEnabled)
                if (state.registrationOcrEnabled) Text(strings.registrationOcr)
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Text(strings.retryInfo)
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.chooseImage)
                }
                OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.retryUpload)
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
    }
}
