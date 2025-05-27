package com.optiroute.com.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.optiroute.com.R
import com.optiroute.com.ui.theme.spacing
import com.optiroute.com.utils.ConfirmationDialog
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController, // Meskipun tidak digunakan saat ini, bisa berguna di masa depan
    viewModel: SettingsViewModel // Terima ViewModel dari NavHost
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val uiState by viewModel.uiState.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }

    val cannotOpenEmailMessage = stringResource(R.string.error_cannot_open_email)
    val cannotOpenUrlMessage = stringResource(R.string.error_cannot_open_url)

    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is SettingsUiEvent.ShowSnackbar -> {
                        val message = event.messageText ?: event.messageResId?.let { context.getString(it) } ?: ""
                        if (message.isNotBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDataDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.settings_clear_data_title),
            message = stringResource(R.string.settings_confirm_clear_data_message),
            onConfirm = {
                viewModel.onClearAllDataConfirmed()
                showClearDataDialog = false
            },
            onDismiss = {
                showClearDataDialog = false
            },
            confirmButtonText = stringResource(R.string.delete), // Menggunakan string yang lebih umum
            dismissButtonText = stringResource(R.string.cancel)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
        // TopAppBar sudah diatur oleh MainActivity
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Bagian Data Aplikasi
            SettingsSectionTitle(title = stringResource(R.string.settings_section_data_management))
            SettingItem(
                icon = Icons.Filled.DeleteForever,
                title = stringResource(R.string.settings_clear_data_title),
                subtitle = stringResource(R.string.settings_clear_data_summary),
                onClick = { showClearDataDialog = true },
                titleColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.error,
                enabled = !uiState.isClearingData // Nonaktifkan saat proses penghapusan
            )
            if (uiState.isClearingData) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Text(stringResource(R.string.settings_clearing_data_progress), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Divider(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small))

            // Bagian Informasi
            SettingsSectionTitle(title = stringResource(R.string.settings_section_about_app))
            SettingItem(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.settings_app_version),
                subtitle = uiState.appVersion,
                onClick = {} // Tidak ada aksi, hanya info
            )
            SettingItem(
                icon = Icons.Filled.Terminal,
                title = stringResource(R.string.settings_developed_by),
                subtitle = stringResource(R.string.developer_name_placeholder), // Gunakan string resource
                onClick = {}
            )
            SettingItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = stringResource(R.string.settings_help_support),
                subtitle = stringResource(R.string.contact_email_placeholder),
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${context.getString(R.string.contact_email_placeholder)}")
                            putExtra(Intent.EXTRA_SUBJECT, "OptiRoute App Support")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to open mail client")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(cannotOpenEmailMessage)
                        }
                    }
                }
            )
            SettingItem(
                icon = Icons.Filled.Policy,
                title = stringResource(R.string.settings_privacy_policy),
                subtitle = stringResource(R.string.settings_privacy_policy_summary),
                onClick = {
                    try {
                        uriHandler.openUri(context.getString(R.string.privacy_policy_url_placeholder))
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to open privacy policy URL")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(cannotOpenUrlMessage)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.app_copyright_placeholder, "2025"), // Gunakan string resource
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium)
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.medium,
            end = MaterialTheme.spacing.medium,
            top = MaterialTheme.spacing.large,
            bottom = MaterialTheme.spacing.small
        )
    )
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = LocalContentColor.current,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
    enabled: Boolean = true, // Tambahkan parameter enabled
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick), // Gunakan parameter enabled
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) iconTint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // Ubah tint jika disabled
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) titleColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}
