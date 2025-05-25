package com.optiroute.com.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.optiroute.com.R // Pastikan R diimpor dengan benar

/**
 * Sealed class yang merepresentasikan semua layar (tujuan navigasi) dalam aplikasi OptiRoute.
 * Setiap objek di dalamnya mewakili satu layar.
 *
 * @property route String unik yang digunakan sebagai rute untuk navigasi.
 * @property titleResId Resource ID untuk judul layar (digunakan di TopAppBar, dll.).
 * @property selectedIcon Ikon yang akan ditampilkan di BottomNavigationBar saat item dipilih.
 * @property unselectedIcon Ikon yang akan ditampilkan di BottomNavigationBar saat item tidak dipilih.
 */
sealed class AppScreens(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector, // Ikon untuk state terpilih
    val unselectedIcon: ImageVector // Ikon untuk state tidak terpilih
) {
    // Layar Utama (Bottom Navigation)
    data object Depot : AppScreens(
        route = "depot_screen",
        titleResId = R.string.nav_depot,
        selectedIcon = Icons.Filled.EditLocationAlt,
        unselectedIcon = Icons.Outlined.EditLocationAlt
    )

    data object Vehicles : AppScreens(
        route = "vehicles_screen",
        titleResId = R.string.nav_vehicles,
        selectedIcon = Icons.Filled.LocalShipping,
        unselectedIcon = Icons.Outlined.LocalShipping
    )

    data object Customers : AppScreens(
        route = "customers_screen",
        titleResId = R.string.nav_customers,
        selectedIcon = Icons.Filled.Group,
        unselectedIcon = Icons.Outlined.Group
    )

    data object PlanRoute : AppScreens(
        route = "plan_route_screen",
        titleResId = R.string.nav_plan_route,
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map
    )

    data object Settings : AppScreens(
        route = "settings_screen",
        titleResId = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    // Layar Detail atau Sekunder (tidak di bottom navigation)
    data object AddEditVehicle : AppScreens(
        route = "add_edit_vehicle", // Rute dasar
        titleResId = R.string.add_vehicle_title, // Judul default untuk tambah
        selectedIcon = Icons.Filled.LocalShipping, // Tidak relevan untuk non-bottom nav
        unselectedIcon = Icons.Outlined.LocalShipping
    ) {
        const val ARG_VEHICLE_ID = "vehicleId"
        // Rute dengan argumen opsional (defaultValue menangani kasus tanpa argumen)
        val routeWithNavArgs = "$route?$ARG_VEHICLE_ID={$ARG_VEHICLE_ID}"

        fun routeWithArg(vehicleId: Int? = null): String {
            return if (vehicleId != null) "$route?$ARG_VEHICLE_ID=$vehicleId" else route // Navigasi ke rute dasar jika ID null
        }
    }

    data object AddEditCustomer : AppScreens(
        route = "add_edit_customer",
        titleResId = R.string.add_customer_title,
        selectedIcon = Icons.Filled.Group,
        unselectedIcon = Icons.Outlined.Group
    ) {
        const val ARG_CUSTOMER_ID = "customerId"
        val routeWithNavArgs = "$route?$ARG_CUSTOMER_ID={$ARG_CUSTOMER_ID}"

        fun routeWithArg(customerId: Int? = null): String {
            return if (customerId != null) "$route?$ARG_CUSTOMER_ID=$customerId" else route
        }
    }

    data object SelectLocationMap : AppScreens(
        route = "select_location_map",
        titleResId = R.string.select_location_on_map,
        selectedIcon = Icons.Filled.Map, // Tidak relevan
        unselectedIcon = Icons.Outlined.Map
    ) {
        const val ARG_INITIAL_LAT = "initialLat"
        const val ARG_INITIAL_LNG = "initialLng"
        const val RESULT_LAT = "resultLat" // Kunci untuk hasil latitude
        const val RESULT_LNG = "resultLng" // Kunci untuk hasil longitude

        // Rute dengan argumen opsional untuk initial location
        val routeWithNavArgs = "$route?$ARG_INITIAL_LAT={$ARG_INITIAL_LAT}&$ARG_INITIAL_LNG={$ARG_INITIAL_LNG}"

        fun createRoute(initialLat: Double? = null, initialLng: Double? = null): String {
            var path = route
            if (initialLat != null && initialLng != null) {
                // Pastikan menggunakan Float untuk NavArgs jika didefinisikan sebagai FloatType
                path += "?$ARG_INITIAL_LAT=${initialLat.toFloat()}&$ARG_INITIAL_LNG=${initialLng.toFloat()}"
            }
            return path
        }
    }

    data object RouteResultsScreen : AppScreens(
        route = "route_results", // Rute dasar
        titleResId = R.string.route_results_title,
        selectedIcon = Icons.Filled.Map, // Tidak relevan
        unselectedIcon = Icons.Outlined.Map
    ) {
        const val ARG_ROUTE_PLAN_ID = "routePlanId"
        // Rute dengan argumen wajib
        val routeWithNavArgs = "$route/{$ARG_ROUTE_PLAN_ID}"

        fun createRoute(planId: String): String {
            return "$route/$planId"
        }
    }
}

// Daftar layar yang akan muncul di Bottom Navigation Bar
val bottomNavScreens = listOf(
    AppScreens.Depot,
    AppScreens.Vehicles,
    AppScreens.Customers,
    AppScreens.PlanRoute,
    AppScreens.Settings
)
