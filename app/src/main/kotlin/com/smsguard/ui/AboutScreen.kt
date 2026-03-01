package com.smsguard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smsguard.BuildConfig
import com.smsguard.R
import com.smsguard.core.BuildChannel
import com.smsguard.core.BuildChannelResolver
import com.smsguard.ui.theme.SMSGuardTheme

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val links = aboutLinkItems()
    val buildChannel = BuildChannelResolver.current()
    var pendingLink by rememberSaveable { mutableStateOf<AboutLinkItem?>(null) }

    pendingLink?.let { link ->
        AlertDialog(
            onDismissRequest = { pendingLink = null },
            title = { Text(text = stringResource(R.string.dialog_open_browser_title)) },
            text = { Text(text = stringResource(R.string.dialog_open_browser_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        openUrl(context = context, url = context.getString(link.urlRes))
                        pendingLink = null
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_open_browser_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLink = null }) {
                    Text(text = stringResource(R.string.dialog_open_browser_cancel))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        BrandHeader(
            iconSize = 72.dp,
            titleStyle = MaterialTheme.typography.titleLarge,
            textSize = MaterialTheme.typography.titleLarge.fontSize,
            gap = 12.dp,
        )

        Text(
            text = stringResource(R.string.about_app_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.joao_illustration),
                    contentDescription = stringResource(R.string.about_image_desc),
                    modifier = Modifier.widthIn(max = 220.dp),
                    contentScale = ContentScale.Fit,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.about_intro_1),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.about_intro_2),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.about_intro_3),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.about_intro_4),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.about_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.about_version_format, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Text(
            text =
                stringResource(
                    R.string.about_channel_format,
                    stringResource(buildChannel.labelResId()),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            links.forEach { link ->
                AboutLinkCard(
                    title = stringResource(link.titleRes),
                    subtitle = stringResource(link.subtitleRes),
                    icon = link.icon(),
                    onOpenRequest = { pendingLink = link },
                    onCopy = {
                        copyUrlToClipboard(
                            context = context,
                            url = context.getString(link.urlRes),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AboutLinkCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onOpenRequest: () -> Unit,
    onCopy: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenRequest),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.about_copy_link),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal data class AboutLinkItem(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val urlRes: Int,
)

internal fun aboutLinkItems(): List<AboutLinkItem> =
    listOf(
        AboutLinkItem(
            titleRes = R.string.about_link_privacy_title,
            subtitleRes = R.string.about_link_privacy_subtitle,
            urlRes = R.string.about_url_privacy,
        ),
        AboutLinkItem(
            titleRes = R.string.about_link_github_title,
            subtitleRes = R.string.about_link_github_subtitle,
            urlRes = R.string.about_url_source,
        ),
    )

@Composable
private fun AboutLinkItem.icon(): ImageVector =
    when (urlRes) {
        R.string.about_url_privacy -> Icons.Outlined.PrivacyTip
        R.string.about_url_source -> Icons.Outlined.Terminal
        else -> Icons.AutoMirrored.Outlined.OpenInNew
    }

@StringRes
private fun BuildChannel.labelResId(): Int =
    when (this) {
        BuildChannel.TEST -> R.string.build_channel_test
        BuildChannel.PROD -> R.string.build_channel_prod
    }

internal fun openAboutUrlIntent(url: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)

private fun openUrl(
    context: Context,
    url: String,
) {
    runCatching {
        context.startActivity(openAboutUrlIntent(url))
    }
}

private fun copyUrlToClipboard(
    context: Context,
    url: String,
) {
    val clipboardManager = context.getSystemService(ClipboardManager::class.java)
    clipboardManager?.setPrimaryClip(ClipData.newPlainText(url, url))
    Toast.makeText(context, context.getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
}

@Preview(name = "About - Light", showBackground = true)
@Composable
private fun AboutScreenPreview() {
    SMSGuardTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            AboutScreen()
        }
    }
}
