package dev.co508.soundboard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.co508.soundboard.ui.SoundboardScreen
import dev.co508.soundboard.ui.about.AboutScreen
import dev.co508.soundboard.ui.about.LicensesScreen

/**
 * Three flat destinations, each owning its own `Scaffold`.
 *
 * Unlike the sibling app, the top bar is not hoisted into `AppScaffold`: the
 * soundboard needs a FAB and a "stop all" action that the other two screens
 * have no use for, so hoisting would mean threading its ViewModel up past
 * screens that don't want it. Each screen instead receives [onOpenDrawer] and
 * renders its own bar.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Soundboard.route,
        modifier = modifier,
    ) {
        composable(Destination.Soundboard.route) { SoundboardScreen(onOpenDrawer = onOpenDrawer) }
        composable(Destination.About.route) {
            AboutScreen(
                onOpenDrawer = onOpenDrawer,
                onOpenLicenses = { navController.navigate(Destination.Licenses.route) },
            )
        }
        composable(Destination.Licenses.route) { LicensesScreen(onOpenDrawer = onOpenDrawer) }
    }
}
