package dev.co508.soundboard.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector
import dev.co508.soundboard.R

/** The hamburger menu's destinations, in drawer order. The first is the start destination. */
enum class Destination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Soundboard("soundboard", R.string.nav_soundboard, Icons.Filled.GraphicEq),
    About("about", R.string.nav_about, Icons.Filled.Info),
    Licenses("licenses", R.string.nav_licenses, Icons.AutoMirrored.Filled.LibraryBooks),
}
