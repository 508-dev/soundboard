package dev.co508.soundboard.ui.help

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.co508.soundboard.R
import dev.co508.soundboard.ui.components.DrawerScaffold

/** The user guide; all prose lives in `res/values/strings.xml` under `how_to_*`. */
@Composable
fun HowToUseScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DrawerScaffold(
        titleRes = R.string.nav_how_to_use,
        onOpenDrawer = onOpenDrawer,
        modifier = modifier,
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
        ) {
            HowToUseSection(R.string.how_to_add_title, R.string.how_to_add_body)
            HowToUseSection(R.string.how_to_unavailable_title, R.string.how_to_unavailable_body)
            HowToUseSection(R.string.how_to_playback_title, R.string.how_to_playback_body)
            HowToUseSection(R.string.how_to_volume_title, R.string.how_to_volume_body)
            HowToUseSection(R.string.how_to_stop_all_title, R.string.how_to_stop_all_body)
            HowToUseSection(R.string.how_to_background_title, R.string.how_to_background_body)
            HowToUseSection(R.string.how_to_focus_title, R.string.how_to_focus_body)
            HowToUseSection(R.string.how_to_options_title, R.string.how_to_options_body)
            HowToUseSection(R.string.how_to_rearrange_title, R.string.how_to_rearrange_body)
            HowToUseSection(R.string.how_to_long_names_title, R.string.how_to_long_names_body)
        }
    }
}

@Composable
private fun HowToUseSection(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
