package com.pokerlanka.pirith.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pokerlanka.pirith.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?
)

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateUiState
    data class UpToDate(val currentVersion: String) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

private fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
    val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
    val cleanLocal = localVersion.trim().removePrefix("v").removePrefix("V")

    val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
    val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }

    val maxLength = maxOf(remoteParts.size, localParts.size)
    for (i in 0 until maxLength) {
        val remotePart = remoteParts.getOrElse(i) { 0 }
        val localPart = localParts.getOrElse(i) { 0 }
        if (remotePart > localPart) return true
        if (remotePart < localPart) return false
    }
    return false
}

private suspend fun fetchLatestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
    try {
        val url = URI("https://api.github.com/repos/GayanChinthaka/Pirith/releases/latest").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            setRequestProperty("User-Agent", "Pirith-Android-App")
        }

        if (connection.responseCode in 200..299) {
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val tagName = json.optString("tag_name", "")
            val name = json.optString("name", "")
            val body = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "")

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        if (assetName.startsWith("Pirith-", ignoreCase = true)) {
                            break
                        }
                    }
                }
            }

            Result.success(
                ReleaseInfo(
                    tagName = tagName,
                    name = name,
                    body = body,
                    htmlUrl = htmlUrl,
                    apkDownloadUrl = apkUrl ?: htmlUrl
                )
            )
        } else {
            Result.failure(IOException("Server returned ${connection.responseCode}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val preferences by viewModel.preferences.collectAsState()
    val showEnglish = preferences.showEnglishNames
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }

    fun checkUpdates() {
        if (uiState is UpdateUiState.Checking) return
        uiState = UpdateUiState.Checking
        coroutineScope.launch {
            val result = fetchLatestRelease()
            result.fold(
                onSuccess = { release ->
                    if (isNewerVersion(release.tagName, BuildConfig.VERSION_NAME)) {
                        uiState = UpdateUiState.UpdateAvailable(release)
                    } else {
                        uiState = UpdateUiState.UpToDate(BuildConfig.VERSION_NAME)
                    }
                },
                onFailure = { error ->
                    uiState = UpdateUiState.Error(
                        error.localizedMessage ?: "යාවත්කාලීන පරීක්ෂා කිරීමට නොහැකි විය"
                    )
                    Toast.makeText(
                        context,
                        "අන්තර්ජාල සම්බන්ධතාවය පරීක්ෂා කරන්න",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        checkUpdates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showEnglish) "යාවත්කාලීන (Updater)" else "යාවත්කාලීන",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Status Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                ) {
                    when (val state = uiState) {
                        is UpdateUiState.Checking, is UpdateUiState.Idle -> {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "යාවත්කාලීන පරීක්ෂා කෙරේ...",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "වත්මන් අනුවාදය: v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is UpdateUiState.UpdateAvailable -> {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "නව යාවත්කාලීනයක් ඇත!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "නවතම අනුවාදය: ${state.release.tagName} (වත්මන්: v${BuildConfig.VERSION_NAME})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (state.release.body.isNotBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "අලුත් විශේෂාංග / වෙනස්කම්:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = state.release.body.trim(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    val targetUrl = state.release.apkDownloadUrl ?: state.release.htmlUrl
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = "යාවත්කාලීනය බාගන්න (Download APK)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.release.htmlUrl))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("GitHub හි විස්තර බලන්න")
                            }
                        }

                        is UpdateUiState.UpToDate -> {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "පිරිත් යෙදුම යාවත්කාලීනයි!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "ඔබ සතුව ඇත්තේ අලුත්ම අනුවාදයයි (v${state.currentVersion})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = { checkUpdates() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = "නැවත පරීක්ෂා කරන්න",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        is UpdateUiState.Error -> {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "පරීක්ෂා කිරීම අසාර්ථක විය",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "අන්තර්ජාල සම්බන්ධතාවය පරීක්ෂා කරන්න (වත්මන්: v${BuildConfig.VERSION_NAME})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = { checkUpdates() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = "නැවත උත්සාහ කරන්න",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
