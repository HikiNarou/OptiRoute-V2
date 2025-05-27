package com.optiroute.com.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.optiroute.com.domain.model.LatLng
import com.optiroute.com.ui.screens.customer.AddEditCustomerScreen
import com.optiroute.com.ui.screens.customer.CustomerViewModel
import com.optiroute.com.ui.screens.customer.CustomersScreen
import com.optiroute.com.ui.screens.depot.DepotScreen
import com.optiroute.com.ui.screens.depot.DepotViewModel
import com.optiroute.com.ui.screens.planroute.PlanRouteScreen
import com.optiroute.com.ui.screens.planroute.PlanRouteViewModel
import com.optiroute.com.ui.screens.planroute.RouteResultsScreen
import com.optiroute.com.ui.screens.settings.SettingsScreen
import com.optiroute.com.ui.screens.settings.SettingsViewModel
import com.optiroute.com.ui.screens.utils.SelectLocationMapScreen
import com.optiroute.com.ui.screens.utils.SelectLocationMapViewModel // Import ViewModel
import com.optiroute.com.ui.screens.vehicle.AddEditVehicleScreen
import com.optiroute.com.ui.screens.vehicle.VehicleViewModel
import com.optiroute.com.ui.screens.vehicle.VehiclesScreen
import timber.log.Timber

@Composable
fun OptiRouteNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onTitleChanged: (Int) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = AppScreens.Depot.route,
        modifier = modifier
    ) {
        // Layar Depot
        composable(AppScreens.Depot.route) {
            onTitleChanged(AppScreens.Depot.titleResId)
            val depotViewModel: DepotViewModel = hiltViewModel()
            DepotScreen(navController = navController, viewModel = depotViewModel)
        }

        // Layar Kendaraan
        composable(AppScreens.Vehicles.route) {
            onTitleChanged(AppScreens.Vehicles.titleResId)
            val vehicleViewModel: VehicleViewModel = hiltViewModel() // Instance baru untuk Vehicles graph
            VehiclesScreen(
                navController = navController,
                viewModel = vehicleViewModel,
                onNavigateToAddEditVehicle = { vehicleId ->
                    // Navigasi ke AddEditVehicle, ViewModel akan di-scope ke NavGraph yang sama
                    navController.navigate(AppScreens.AddEditVehicle.routeWithArg(vehicleId))
                }
            )
        }

        // Layar Tambah/Ubah Kendaraan
        composable(
            route = AppScreens.AddEditVehicle.routeWithNavArgs,
            arguments = listOf(
                navArgument(AppScreens.AddEditVehicle.ARG_VEHICLE_ID) {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getInt(AppScreens.AddEditVehicle.ARG_VEHICLE_ID) ?: -1
            val titleRes = if (vehicleId == -1) AppScreens.AddEditVehicle.titleResId else com.optiroute.com.R.string.edit_vehicle_title
            onTitleChanged(titleRes)
            // Berbagi ViewModel dengan VehiclesScreen (parent NavGraph)
            val vehiclesBackStackEntry = remember(backStackEntry) {
                navController.getBackStackEntry(AppScreens.Vehicles.route)
            }
            val vehicleViewModel: VehicleViewModel = hiltViewModel(vehiclesBackStackEntry)
            AddEditVehicleScreen(
                navController = navController,
                viewModel = vehicleViewModel,
                vehicleId = if (vehicleId == -1) null else vehicleId
            )
        }

        // Layar Pelanggan
        composable(AppScreens.Customers.route) {
            onTitleChanged(AppScreens.Customers.titleResId)
            val customerViewModel: CustomerViewModel = hiltViewModel() // Instance baru untuk Customers graph
            CustomersScreen(
                navController = navController,
                viewModel = customerViewModel,
                onNavigateToAddEditCustomer = { customerId ->
                    navController.navigate(AppScreens.AddEditCustomer.routeWithArg(customerId))
                }
            )
        }

        // Layar Tambah/Ubah Pelanggan
        composable(
            route = AppScreens.AddEditCustomer.routeWithNavArgs,
            arguments = listOf(
                navArgument(AppScreens.AddEditCustomer.ARG_CUSTOMER_ID) {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getInt(AppScreens.AddEditCustomer.ARG_CUSTOMER_ID) ?: -1
            val titleRes = if (customerId == -1) AppScreens.AddEditCustomer.titleResId else com.optiroute.com.R.string.edit_customer_title
            onTitleChanged(titleRes)
            // Berbagi ViewModel dengan CustomersScreen (parent NavGraph)
            val customersBackStackEntry = remember(backStackEntry) {
                navController.getBackStackEntry(AppScreens.Customers.route)
            }
            val customerViewModel: CustomerViewModel = hiltViewModel(customersBackStackEntry)
            AddEditCustomerScreen(
                navController = navController,
                viewModel = customerViewModel, // ViewModel di-pass dari parent
                customerId = if (customerId == -1) null else customerId
            )
        }


        // Layar Perencanaan Rute
        composable(AppScreens.PlanRoute.route) {
            onTitleChanged(AppScreens.PlanRoute.titleResId)
            val planRouteViewModel: PlanRouteViewModel = hiltViewModel()
            PlanRouteScreen(navController = navController, viewModel = planRouteViewModel)
        }

        // Layar Hasil Rute
        composable(
            route = AppScreens.RouteResultsScreen.routeWithNavArgs,
            arguments = listOf(navArgument(AppScreens.RouteResultsScreen.ARG_ROUTE_PLAN_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString(AppScreens.RouteResultsScreen.ARG_ROUTE_PLAN_ID)
            onTitleChanged(AppScreens.RouteResultsScreen.titleResId)
            val planRouteBackStackEntry = remember(backStackEntry) {
                navController.getBackStackEntry(AppScreens.PlanRoute.route)
            }
            val planRouteViewModel: PlanRouteViewModel = hiltViewModel(planRouteBackStackEntry)
            RouteResultsScreen(
                navController = navController,
                routePlanId = planId,
                planRouteViewModel = planRouteViewModel
            )
        }

        // Layar Pengaturan
        composable(AppScreens.Settings.route) {
            onTitleChanged(AppScreens.Settings.titleResId)
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(navController = navController, viewModel = settingsViewModel)
        }

        // Layar Pilih Lokasi di Peta
        composable(
            route = AppScreens.SelectLocationMap.routeWithNavArgs,
            arguments = listOf(
                navArgument(AppScreens.SelectLocationMap.ARG_INITIAL_LAT) {
                    type = NavType.FloatType
                    defaultValue = -999.0f
                },
                navArgument(AppScreens.SelectLocationMap.ARG_INITIAL_LNG) {
                    type = NavType.FloatType
                    defaultValue = -999.0f
                }
            )
        ) { backStackEntry ->
            onTitleChanged(AppScreens.SelectLocationMap.titleResId) // Judul tidak akan ditampilkan karena TopBar disembunyikan di MainActivity untuk layar ini
            val initialLatArg = backStackEntry.arguments?.getFloat(AppScreens.SelectLocationMap.ARG_INITIAL_LAT)
            val initialLngArg = backStackEntry.arguments?.getFloat(AppScreens.SelectLocationMap.ARG_INITIAL_LNG)

            val initialLatLng = if (initialLatArg != -999.0f && initialLngArg != -999.0f && initialLatArg != null && initialLngArg != null) {
                LatLng(initialLatArg.toDouble(), initialLngArg.toDouble())
            } else {
                null
            }
            // ViewModel untuk SelectLocationMapScreen
            val selectLocationMapViewModel: SelectLocationMapViewModel = hiltViewModel()

            SelectLocationMapScreen(
                navController = navController,
                initialLatLng = initialLatLng,
                onLocationSelected = { latLng ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(AppScreens.SelectLocationMap.RESULT_LAT, latLng.latitude)
                    navController.previousBackStackEntry?.savedStateHandle?.set(AppScreens.SelectLocationMap.RESULT_LNG, latLng.longitude)
                    Timber.d("Location selected on map: $latLng, navigating back.")
                    navController.popBackStack()
                },
                viewModel = selectLocationMapViewModel // Pass ViewModel
            )
        }
    }
}