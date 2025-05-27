package com.optiroute.com.ui.theme

import androidx.compose.ui.graphics.Color

// Skema Warna Terang (Light Theme) - Dihasilkan dari Material Theme Builder
val md_theme_light_primary = Color(0xFF0061A4) // Warna utama aplikasi (misalnya, tombol utama, FAB)
val md_theme_light_onPrimary = Color(0xFFFFFFFF) // Warna teks/ikon di atas warna primer
val md_theme_light_primaryContainer = Color(0xFFD1E4FF) // Warna kontainer untuk elemen primer (misalnya, TopAppBar)
val md_theme_light_onPrimaryContainer = Color(0xFF001D36) // Warna teks/ikon di atas kontainer primer

val md_theme_light_secondary = Color(0xFF535F70) // Warna sekunder (misalnya, filter, chip)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD7E3F7) // Kontainer untuk elemen sekunder
val md_theme_light_onSecondaryContainer = Color(0xFF101C2B)

val md_theme_light_tertiary = Color(0xFF6B5778) // Warna tersier (misalnya, aksen kurang menonjol)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFF2DAFF)
val md_theme_light_onTertiaryContainer = Color(0xFF251431)

val md_theme_light_error = Color(0xFFBA1A1A) // Warna untuk error (misalnya, pesan error, ikon error)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)

val md_theme_light_background = Color(0xFFFDFCFF) // Warna latar belakang utama aplikasi
val md_theme_light_onBackground = Color(0xFF1A1C1E) // Warna teks/ikon di atas latar belakang

val md_theme_light_surface = Color(0xFFFDFCFF) // Warna permukaan komponen seperti Card, Sheet
val md_theme_light_onSurface = Color(0xFF1A1C1E) // Warna teks/ikon di atas permukaan

val md_theme_light_surfaceVariant = Color(0xFFDFE2EB) // Varian warna permukaan (misalnya, divider, outline)
val md_theme_light_onSurfaceVariant = Color(0xFF42474E) // Warna teks/ikon di atas varian permukaan

val md_theme_light_outline = Color(0xFF73777F) // Warna untuk outline komponen (misalnya, TextField)
val md_theme_light_inverseOnSurface = Color(0xFFF1F0F4) // Warna teks/ikon untuk permukaan invers (jarang digunakan)
val md_theme_light_inverseSurface = Color(0xFF2F3133) // Permukaan invers (untuk kontras tinggi)
val md_theme_light_inversePrimary = Color(0xFF9FCAFF) // Primer invers
val md_theme_light_surfaceTint = Color(0xFF0061A4) // Tint untuk permukaan, biasanya sama dengan primer
val md_theme_light_outlineVariant = Color(0xFFC2C7CF) // Varian warna outline
val md_theme_light_scrim = Color(0xFF000000) // Warna untuk scrim (overlay gelap)

// Skema Warna Gelap (Dark Theme) - Dihasilkan dari Material Theme Builder
val md_theme_dark_primary = Color(0xFF9FCAFF)
val md_theme_dark_onPrimary = Color(0xFF003258)
val md_theme_dark_primaryContainer = Color(0xFF00497D)
val md_theme_dark_onPrimaryContainer = Color(0xFFD1E4FF)

val md_theme_dark_secondary = Color(0xFFBBC7DB)
val md_theme_dark_onSecondary = Color(0xFF253140)
val md_theme_dark_secondaryContainer = Color(0xFF3B4858)
val md_theme_dark_onSecondaryContainer = Color(0xFFD7E3F7)

val md_theme_dark_tertiary = Color(0xFFD6BEE4)
val md_theme_dark_onTertiary = Color(0xFF3B2948)
val md_theme_dark_tertiaryContainer = Color(0xFF523F5F)
val md_theme_dark_onTertiaryContainer = Color(0xFFF2DAFF)

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

val md_theme_dark_background = Color(0xFF1A1C1E)
val md_theme_dark_onBackground = Color(0xFFE2E2E6)

val md_theme_dark_surface = Color(0xFF1A1C1E)
val md_theme_dark_onSurface = Color(0xFFE2E2E6)

val md_theme_dark_surfaceVariant = Color(0xFF42474E)
val md_theme_dark_onSurfaceVariant = Color(0xFFC2C7CF)

val md_theme_dark_outline = Color(0xFF8D9199)
val md_theme_dark_inverseOnSurface = Color(0xFF1A1C1E)
val md_theme_dark_inverseSurface = Color(0xFFE2E2E6)
val md_theme_dark_inversePrimary = Color(0xFF0061A4)
val md_theme_dark_surfaceTint = Color(0xFF9FCAFF)
val md_theme_dark_outlineVariant = Color(0xFF42474E)
val md_theme_dark_scrim = Color(0xFF000000)


// Warna tambahan untuk rute di peta (konsisten dengan RouteResultsScreen)
val MapRouteColor1 = Color(0xFFFF5722) // Deep Orange
val MapRouteColor2 = Color(0xFF4CAF50) // Green
val MapRouteColor3 = Color(0xFF2196F3) // Blue
val MapRouteColor4 = Color(0xFFCDDC39) // Lime (diubah dari Amber agar lebih variatif)
val MapRouteColor5 = Color(0xFF9C27B0) // Purple
val MapRouteColor6 = Color(0xFFE91E63) // Pink
val MapRouteColor7 = Color(0xFF00BCD4) // Cyan
val MapRouteColor8 = Color(0xFF8BC34A) // Light Green
val MapRouteColor9 = Color(0xFFFF9800) // Orange
val MapRouteColor10 = Color(0xFF795548) // Brown (diubah dari ungu agar lebih variatif)

// Warna untuk marker (bisa disesuaikan)
val MapDepotMarkerColor = Color(0xFFD32F2F) // Merah Tua untuk Depot (konsisten dengan ic_depot_pin.xml jika menggunakan tint)
val MapCustomerMarkerColor = Color(0xFF1976D2) // Biru untuk Pelanggan

// Warna untuk efek shimmer (jika digunakan)
val ShimmerColorShades = listOf(
    Color.LightGray.copy(alpha = 0.9f),
    Color.LightGray.copy(alpha = 0.2f),
    Color.LightGray.copy(alpha = 0.9f)
)
