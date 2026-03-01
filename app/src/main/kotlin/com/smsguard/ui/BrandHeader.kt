package com.smsguard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smsguard.R

@Composable
fun BrandHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 72.dp,
    textSize: TextUnit = 36.sp,
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge,
    gap: Dp = 8.dp,
    stacked: Boolean = true,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = Color.Transparent,
) {
    val content: @Composable () -> Unit = {
        if (stacked) {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.setup_app_brand),
                    modifier = Modifier.size(iconSize),
                )
                Spacer(modifier = Modifier.height(gap))
                Text(
                    text = stringResource(R.string.setup_app_brand),
                    style = titleStyle,
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                )
                subtitle?.takeIf { it.isNotBlank() }?.let { supporting ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                    )
                }
            }
        } else {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.setup_app_brand),
                    modifier = Modifier.size(iconSize),
                )
                Spacer(modifier = Modifier.width(gap))
                Column {
                    Text(
                        text = stringResource(R.string.setup_app_brand),
                        style = titleStyle,
                        fontSize = textSize,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let { supporting ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = supporting,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor,
                        )
                    }
                }
            }
        }
    }

    if (containerColor == Color.Transparent) {
        content()
        return
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
