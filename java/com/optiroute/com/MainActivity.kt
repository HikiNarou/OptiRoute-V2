package com.optiroute.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.optiroute.com.ui.navigation.AppScreens
import com.optiroute.com.ui.navigation.OptiRouteNavHost
import com.optiroute.com.ui.navigation.bottomNavScreens
import com.optiroute.com.ui.theme.OptiRouteTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import androidx.compose.ui.unit.dp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("MainActivity onCreate called")
        enableEdgeToEdge() // Mengaktifkan tampilan edge-to-edge
        setContent {
            OptiRouteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OptiRouteApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptiRouteApp() {
    val navController = rememberNavController()
    // State untuk menyimpan resource ID judul TopAppBar saat ini, menggunakan rememberSaveable
    var currentScreenTitleResId by rememberSaveable { mutableStateOf(AppScreens.Depot.titleResId) }

    Scaffold(
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Tentukan apakah TopAppBar harus ditampilkan
            // Tidak ditampilkan untuk SelectLocationMap dan RouteResultsScreen
            val showTopBar = currentRoute != null &&
                    !currentRoute.startsWith(AppScreens.SelectLocationMap.route) && // Cek prefix untuk argumen
                    !currentRoute.startsWith(AppScreens.RouteResultsScreen.route)   // Cek prefix untuk argumen

            if (showTopBar) {
                TopAppBar(
                    title = { Text(stringResource(id = currentScreenTitleResId)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    // Navigation icon (misalnya tombol kembali) akan ditangani oleh masing-masing layar jika diperlukan
                )
            }
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // Tampilkan BottomBar hanya jika rute saat ini ada di daftar bottomNavScreens
            val showBottomBar = bottomNavScreens.any { it.route == currentDestination?.route }

            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), // Memberi sedikit elevasi
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavScreens.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = stringResource(id = screen.titleResId)
                                )
                            },
                            label = { Text(stringResource(id = screen.titleResId)) },
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) { // Hanya navigasi jika belum dipilih
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    // Judul di TopAppBar akan diupdate oleh NavHost melalui callback
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) // Indikator yang lebih lembut
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        OptiRouteNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onTitleChanged = { titleResId ->
                // Callback ini dipanggil oleh OptiRouteNavHost setiap kali layar berubah
                currentScreenTitleResId = titleResId
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OptiRouteTheme {
        OptiRouteApp()
    }
}
