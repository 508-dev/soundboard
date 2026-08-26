package dev.co508.soundboard.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import dev.co508.soundboard.R

/**
 * The chrome every destination shares: a top bar whose navigation icon opens
 * the drawer.
 *
 * [actions] and [floatingActionButton] are open so the soundboard can add its
 * "stop all" control and `+` button without the other screens carrying either.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScaffold(
    @StringRes titleRes: Int,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.nav_menu_content_description),
                        )
                    }
                },
                actions = actions,
            )
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

/** Makes a `ListItem` behave like a full-width row button. */
fun Modifier.clickableItem(onClick: () -> Unit): Modifier = fillMaxWidth().clickable(onClick = onClick)

/**
 * Returns a function that opens a URL in the user's browser.
 *
 * No-ops when the device has nothing that can handle the intent, rather than
 * crashing — a stripped-down or work-profiled device may genuinely have no
 * browser.
 */
@Composable
fun rememberUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: ActivityNotFoundException) {
                // Nothing installed to open links; silently ignore.
            }
        }
    }
}
