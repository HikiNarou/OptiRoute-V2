package com.optiroute.com.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.optiroute.com.R

/**
 * Memeriksa apakah izin lokasi (FINE dan COARSE) telah diberikan.
 *
 * @return True jika kedua izin lokasi telah diberikan, false sebaliknya.
 */
fun Context.hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Membuka halaman pengaturan aplikasi untuk aplikasi saat ini.
 * Pengguna dapat secara manual memberikan izin dari sana.
 */
fun Context.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also {
        // Tambahkan flag untuk memastikan intent dimulai dari context yang benar jika dipanggil dari non-Activity
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(it)
    }
}

/**
 * Memeriksa apakah pengguna telah memilih "Don't ask again" untuk suatu izin.
 *
 * @param activity Activity yang digunakan untuk memeriksa status izin.
 * @param permission Izin yang akan diperiksa (misalnya, Manifest.permission.ACCESS_FINE_LOCATION).
 * @return True jika pengguna telah memilih "Don't ask again", false sebaliknya.
 */
fun Activity.shouldShowRationaleForPermission(permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
}


/**
 * Composable untuk menampilkan dialog yang menjelaskan mengapa izin diperlukan.
 * Memberikan opsi kepada pengguna untuk membuka pengaturan aplikasi atau menolak.
 *
 * @param title Judul dialog.
 * @param message Pesan penjelasan mengapa izin diperlukan.
 * @param onConfirm Callback saat pengguna memilih untuk membuka pengaturan.
 * @param onDismiss Callback saat pengguna memilih untuk menolak atau menutup dialog.
 * @param confirmText Teks untuk tombol konfirmasi (membuka pengaturan).
 * @param dismissText Teks untuk tombol batal/tolak.
 * @param modifier Modifier untuk AlertDialog.
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit, // Biasanya untuk membuka pengaturan
    onDismiss: () -> Unit, // Saat pengguna menolak
    confirmText: String = stringResource(R.string.open_settings),
    dismissText: String = stringResource(R.string.cancel), // Atau "Tolak"
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss, // Jika pengguna klik di luar dialog
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, fontWeight = FontWeight.Medium)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
