package dev.co508.soundboard.ui.about

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
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
import dev.co508.soundboard.BuildConfig
import dev.co508.soundboard.R
import dev.co508.soundboard.ui.components.DrawerScaffold
import dev.co508.soundboard.ui.components.clickableItem
import dev.co508.soundboard.ui.components.rememberUrlOpener

/**
 * Template About page.
 *
 * The prose lives in `res/values/strings.xml` under the `about_*` keys, marked
 * `TODO`, so filling this in is a strings edit rather than a Compose edit. The
 * link URLs are in the same place under `about_link_*_url`.
 */
@Composable
fun AboutScreen(
    onOpenDrawer: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DrawerScaffold(
        titleRes = R.string.nav_about,
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
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AboutSection(
                titleRes = R.string.about_app_title,
                bodyRes = R.string.about_app_body,
            )
            AboutSection(
                titleRes = R.string.about_508_title,
                bodyRes = R.string.about_508_body,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LinkItem(
                labelRes = R.string.about_link_repo,
                urlRes = R.string.about_link_repo_url,
            )
            LinkItem(
                labelRes = R.string.about_link_508,
                urlRes = R.string.about_link_508_url,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.nav_licenses)) },
                supportingContent = { Text(stringResource(R.string.about_licenses_summary)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null) },
                modifier = Modifier.clickableItem(onClick = onOpenLicenses),
            )

            Text(
                text = stringResource(R.string.about_license_line),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun AboutSection(
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

@Composable
private fun LinkItem(
    @StringRes labelRes: Int,
    @StringRes urlRes: Int,
) {
    val url = stringResource(urlRes)
    val openUrl = rememberUrlOpener()

    ListItem(
        headlineContent = { Text(stringResource(labelRes)) },
        supportingContent = { Text(url) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
        modifier = Modifier.clickableItem(onClick = { openUrl(url) }),
    )
}
