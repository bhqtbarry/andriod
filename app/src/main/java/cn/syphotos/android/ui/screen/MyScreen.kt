package cn.syphotos.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.syphotos.android.ui.state.MyUiState

@Composable
fun MyScreen(state: MyUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Account") {
                Text("User: ${state.user.username}")
                Text("Email: ${state.user.email}")
                Text(if (state.user.emailVerified) "Email verified" else "Email verification required")
                Text("Password change supported in app auth flow")
            }
        }
        item {
            SectionCard("My Works") {
                Text("Approved / pending / rejected filtering should live here")
                Text("Current loaded works: ${state.works.size}")
            }
        }
        item {
            SectionCard("My Likes") {
                Text("Liked photos: ${state.likedPhotos.size}")
            }
        }
        item {
            SectionCard("Pending") {
                Text("Editable items: ${state.pending.size}")
            }
        }
        item {
            SectionCard("Rejected") {
                state.rejected.forEach {
                    Text("Reason: ${it.rejectionReason ?: "-"}")
                    Text("Admin: ${it.adminComment ?: "-"}")
                }
            }
        }
        item {
            SectionCard("Devices") {
                Text("Current + revocable sessions")
            }
        }
        items(state.sessions, key = { it.id }) { session ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(session.deviceName, style = MaterialTheme.typography.titleMedium)
                    Text("${session.systemVersion} • ${session.ipAddress}")
                    Text("Login: ${session.loginTime}")
                    Text(if (session.isCurrent) "Current device" else "Revocable")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

