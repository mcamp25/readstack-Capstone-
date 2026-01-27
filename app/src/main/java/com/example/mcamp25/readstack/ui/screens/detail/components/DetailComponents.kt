package com.example.mcamp25.readstack.ui.screens.detail.components

import android.text.Html
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.mcamp25.readstack.data.network.ImageLinks
import com.example.mcamp25.readstack.data.network.getBestUrl
import com.example.mcamp25.readstack.ui.toHighResBookUrl

@Composable
fun ReadingControls(
    isRead: Boolean,
    inProgress: Boolean,
    onAdd: () -> Unit,
    onToggleReading: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val readingButtonColor by animateColorAsState(
        targetValue = if (inProgress) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
        label = "readingButtonColor"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "buttonColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isRead) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onAdd,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            modifier = Modifier
                .weight(0.7f)
                .height(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(2.dp))
            Text("Add", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
            onClick = onToggleReading,
            modifier = Modifier
                .weight(1.3f)
                .height(32.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = readingButtonColor,
                contentColor = if (inProgress) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = if (inProgress) null else ButtonDefaults.outlinedButtonBorder(true)
        ) {
            Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                if (inProgress) "Reading" else "Read",
                style = MaterialTheme.typography.labelMedium
            )
        }

        OutlinedButton(
            onClick = onToggleRead,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            modifier = Modifier
                .weight(0.9f)
                .height(32.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = buttonColor,
                contentColor = contentColor
            ),
            border = if (isRead) null else ButtonDefaults.outlinedButtonBorder(true)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = if (isRead) "Done" else "Finish",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun CoverHeader(
    imageLinks: ImageLinks?,
    title: String,
    noCoverPainter: Painter,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(300.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageLinks?.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(20.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )

        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageLinks.getBestUrl()?.toHighResBookUrl())
                .crossfade(true)
                .allowHardware(false)
                .precision(Precision.EXACT)
                .build(),
            contentDescription = title,
            modifier = Modifier
                .height(250.dp)
                .width(180.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            loading = { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) },
            error = { Image(painter = noCoverPainter, contentDescription = null) }
        )
    }
}

@Composable
fun MetaRow(date: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        date?.take(4)?.let { year ->
            Icon(
                imageVector = Icons.Default.CalendarMonth, 
                contentDescription = null, 
                modifier = Modifier.size(16.dp), 
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = year, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

val String.parseHtml: String 
    get() = Html.fromHtml(this, Html.FROM_HTML_MODE_COMPACT).toString()
