package cn.syphotos.android.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.ui.common.GlideFitWidthImage
import cn.syphotos.android.ui.i18n.AppLanguage
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.MyUiState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    state: MyUiState,
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onDeletePhoto: (PhotoItem) -> Unit,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var loginInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<PhotoItem?>(null) }
    val selectedTab = state.selectedTab
    val worksListState = rememberLazyListState()

    pendingDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除作品") },
            text = { Text("确定删除《${photo.title}》吗？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDeletePhoto(photo)
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { onTabSelected(0) }, text = { Text("作品管理") })
            Tab(selected = selectedTab == 1, onClick = { onTabSelected(1) }, text = { Text("资料") })
        }

        when (selectedTab) {
            0 -> PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = worksListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SectionCard(strings.myWorks) {
                            Text(strings.worksCount(state.summary.allPhotos))
                        }
                    }
                    if (state.authSession.isLoggedIn) {
                        item {
                            SectionCard("统计") {
                                Text("上传总数: ${state.summary.allPhotos}")
                                Text("通过: ${state.summary.approvedPhotos}")
                                Text("待审核: ${state.summary.pendingPhotos}")
                                Text("被拒绝: ${state.summary.rejectedPhotos}")
                                Text("Likes: ${state.summary.likedPhotos}")
                            }
                        }
                    }
                    state.successMessage?.let { item { MessageSurface(it, false) } }
                    state.errorMessage?.let { item { MessageSurface(it, true) } }
                    if (state.isDeleting) {
                        item { CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp)) }
                    }
                    items(state.works, key = { it.id }) { photo ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (photo.thumbUrl.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenPhoto(photo.id) },
                                    ) {
                                        GlideFitWidthImage(
                                            url = photo.thumbUrl,
                                            contentDescription = photo.title,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                Text(
                                    photo.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    WorkInfoCell(
                                        modifier = Modifier.weight(1f),
                                        label = "编号",
                                        value = photo.registration.ifBlank { "#${photo.id}" },
                                    )
                                    WorkInfoCell(
                                        modifier = Modifier.weight(1f),
                                        label = "机型",
                                        value = photo.aircraftModel.ifBlank { "-" },
                                    )
                                    WorkInfoCell(
                                        modifier = Modifier.weight(1f),
                                        label = "位置",
                                        value = photo.location.ifBlank { "-" },
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = statusBackground(photo.status),
                                            disabledContentColor = statusForeground(photo.status),
                                        ),
                                    ) {
                                        Text(statusLabel(photo.status), fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = { pendingDelete = photo },
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Text("删除这张照片", fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SectionCard(strings.languageTitle) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    onLanguageSelected(if (selectedLanguage == AppLanguage.ZH) AppLanguage.EN else AppLanguage.ZH)
                                },
                            ) {
                                Text(if (selectedLanguage == AppLanguage.ZH) "Switch to English" else "切换到中文")
                            }
                            Text(selectedLanguage.nativeName, modifier = Modifier.padding(top = 10.dp))
                        }
                    }
                }
                item {
                    SectionCard(strings.account) {
                        if (!state.authSession.isLoggedIn) {
                            OutlinedTextField(
                                value = loginInput,
                                onValueChange = { loginInput = it },
                                label = { Text(strings.loginLabel) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text(strings.passwordField) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(onClick = { onLogin(loginInput, passwordInput) }, modifier = Modifier.fillMaxWidth()) {
                                Text(strings.signIn)
                            }
                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.syphotos.cn/register.php")),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("注册")
                            }
                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.syphotos.cn/forgot_password.php")),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("忘记密码")
                            }
                            state.authErrorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        } else {
                            Text("${strings.userLabel}: ${state.user.username}")
                            Text("${strings.emailLabel}: ${state.user.email}")
                            Text(if (state.user.emailVerified) strings.emailVerified else strings.emailVerificationRequired)
                            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                                Text(strings.signOut)
                            }
                        }
                    }
                }
                state.successMessage?.let { item { MessageSurface(it, false) } }
                state.errorMessage?.let { item { MessageSurface(it, true) } }
                if (state.isLoading || state.isDeleting) {
                    item { CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun WorkInfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun statusLabel(status: String): String {
    return when (status) {
        "approved" -> "已批准"
        "rejected" -> "已拒绝"
        else -> "待审核"
    }
}

private fun statusBackground(status: String): Color {
    return when (status) {
        "approved" -> Color(0xFFDDF6E8)
        "rejected" -> Color(0xFFFCE3E3)
        else -> Color(0xFFE8F0FF)
    }
}

private fun statusForeground(status: String): Color {
    return when (status) {
        "approved" -> Color(0xFF176B43)
        "rejected" -> Color(0xFFB42318)
        else -> Color(0xFF245BDB)
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

@Composable
private fun MessageSurface(
    message: String,
    error: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
