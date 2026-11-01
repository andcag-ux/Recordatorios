package com.example.recordatorios.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recordatorios.ui.crear.CrearRoute
import com.example.recordatorios.ui.detalle.DetalleRoute
import com.example.recordatorios.ui.listado.ListadoRoute

/**
 * Grafo de navegación. El NavController se crea aquí, en el nivel de la
 * aplicación, y NO se pasa a las pantallas ni a los componentes: ellos solo
 * reciben lambdas (onCrearRecordatorio, onRecordatorioClick, onGuardar,
 * onCancelar, onVolver).
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Rutas.LISTADO,
        modifier = modifier
    ) {
        composable(Rutas.LISTADO) {
            ListadoRoute(
                onCrearRecordatorio = {
                    navController.navigate(Rutas.CREAR) { launchSingleTop = true }
                },
                onRecordatorioClick = { id ->
                    navController.navigate(Rutas.detalle(id)) { launchSingleTop = true }
                }
            )
        }

        composable(Rutas.CREAR) {
            CrearRoute(
                // popBackStack() vuelve al listado que ya está en la pila: no se duplica.
                onGuardar = { navController.popBackStack() },
                onCancelar = { navController.popBackStack() }
            )
        }

        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(Rutas.ARG_ID) ?: -1L
            DetalleRoute(
                id = id,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
