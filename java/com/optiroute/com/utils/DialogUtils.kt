package com.optiroute.com.utils

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.optiroute.com.R

/**
 * Composable generik untuk menampilkan dialog konfirmasi.
 *
 * @param title Judul dialog.
 * @param message Pesan atau isi dialog.
 * @param onConfirm Callback yang dipanggil saat tombol konfirmasi ditekan.
 * @param onDismiss Callback yang dipanggil saat dialog ditutup (baik dengan tombol batal atau sentuhan di luar).
 * @param confirmButtonText Teks untuk tombol konfirmasi. Default: "Konfirmasi".
 * @param dismissButtonText Teks untuk tombol batal. Default: "Batal".
 * @param modifier Modifier untuk AlertDialog.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(id = R.string.confirm),
    dismissButtonText: String = stringResource(id = R.string.cancel),
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall, // Gaya judul yang lebih menonjol
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
            TextButton(
                onClick = {
                    onConfirm()
                    // onDismiss() // Umumnya, dialog ditutup oleh pemanggil setelah onConfirm
                    // atau biarkan onDismissRequest yang menangani jika user klik di luar
                }
            ) {
                Text(confirmButtonText, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText, fontWeight = FontWeight.Medium)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface, // Warna latar dialog
        titleContentColor = MaterialTheme.colorScheme.onSurface, // Warna teks judul
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant // Warna teks isi
    )
}