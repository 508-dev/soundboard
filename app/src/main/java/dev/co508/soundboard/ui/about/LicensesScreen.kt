package dev.co508.soundboard.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.co508.soundboard.R
import dev.co508.soundboard.ui.components.DrawerScaffold
import dev.co508.soundboard.ui.components.clickableItem
import dev.co508.soundboard.ui.components.rememberUrlOpener

/**
 * One third-party component the app ships.
 *
 * Grouped by project rather than by artifact — listing every `androidx.*`
 * coordinate separately would be noise, and they share a licence.
 */
private data class Dependency(
    val name: String,
    val license: String,
    val url: String,
)

/**
 * Hand-maintained, and deliberately so.
 *
 * The Play-services OSS-licences plugin is not an option here: it isn't free
 * software, which would defeat the F-Droid goal (see `DECISIONS.md` → "GPL-3,
 * Targeting F-Droid"). **Update this list whenever `gradle/libs.versions.toml`
 * gains or loses a dependency.**
 */
private val DEPENDENCIES =
    listOf(
        Dependency(
            name = "AndroidX (Core, Activity, Lifecycle, Navigation, DataStore)",
            license = "Apache License 2.0",
            url = "https://developer.android.com/jetpack/androidx",
        ),
        Dependency(
            name = "Jetpack Compose (UI, Material 3, Material Icons)",
            license = "Apache License 2.0",
            url = "https://developer.android.com/jetpack/compose",
        ),
        Dependency(
            name = "AndroidX Media3 / ExoPlayer",
            license = "Apache License 2.0",
            url = "https://github.com/androidx/media",
        ),
        Dependency(
            name = "Kotlin standard library",
            license = "Apache License 2.0",
            url = "https://github.com/JetBrains/kotlin",
        ),
        Dependency(
            name = "kotlinx.coroutines",
            license = "Apache License 2.0",
            url = "https://github.com/Kotlin/kotlinx.coroutines",
        ),
        Dependency(
            name = "kotlinx.serialization",
            license = "Apache License 2.0",
            url = "https://github.com/Kotlin/kotlinx.serialization",
        ),
    )

@Composable
fun LicensesScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openUrl = rememberUrlOpener()

    DrawerScaffold(
        titleRes = R.string.nav_licenses,
        onOpenDrawer = onOpenDrawer,
        modifier = modifier,
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.licenses_app_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.licenses_app_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.licenses_gpl_link)) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                    modifier = Modifier.clickableItem { openUrl(GPL_URL) },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    text = stringResource(R.string.licenses_third_party_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }

            items(DEPENDENCIES, key = { it.name }) { dependency ->
                ListItem(
                    headlineContent = { Text(dependency.name) },
                    supportingContent = { Text(dependency.license) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                    modifier = Modifier.clickableItem { openUrl(dependency.url) },
                )
            }

            item {
                Text(
                    text = stringResource(R.string.licenses_apache_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        }
    }
}

private const val GPL_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
