package com.optiroute.com.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Data class untuk mendefinisikan nilai-nilai spasi kustom.
 * Ini memungkinkan konsistensi spasi di seluruh aplikasi dan memudahkan
 * penyesuaian jika diperlukan.
 */
data class Spacing(
    val default: Dp = 0.dp,         // Spasi default, biasanya tidak digunakan secara eksplisit
    val micro: Dp = 2.dp,           // Spasi sangat kecil
    val extraSmall: Dp = 4.dp,      // Spasi ekstra kecil
    val small: Dp = 8.dp,           // Spasi kecil
    val medium: Dp = 16.dp,         // Spasi medium (paling umum)
    val large: Dp = 24.dp,          // Spasi besar
    val extraLarge: Dp = 32.dp,     // Spasi ekstra besar
    val huge: Dp = 48.dp,           // Spasi sangat besar
    val gigantic: Dp = 64.dp        // Spasi paling besar
)

/**
 * CompositionLocal untuk menyediakan instance Spacing ke seluruh hierarki Composable.
 * Menggunakan staticCompositionLocalOf karena nilai spasi cenderung tidak berubah secara dinamis
 * selama runtime, yang memberikan sedikit keuntungan performa.
 */
val LocalSpacing = staticCompositionLocalOf { Spacing() }

/**
 * Extension property pada MaterialTheme untuk mengakses nilai Spacing dengan mudah.
 * Contoh penggunaan: `MaterialTheme.spacing.medium`
 */
val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable // Menandakan bahwa getter ini tidak memiliki side effects dan hanya membaca state.
    get() = LocalSpacing.current
