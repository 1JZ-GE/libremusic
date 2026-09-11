package app.pulse.android.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import app.pulse.android.utils.thumbnail
import app.pulse.android.ui.modifiers.verticalFadingEdge
import app.pulse.core.ui.LocalAppearance
import androidx.compose.runtime.LaunchedEffect
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MosaicThumbnail(
    urls: List<String>,
    modifier: Modifier = Modifier
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val placeholder = remember { ColorPainter(colorPalette.background1) }

    @Composable
    fun cell(url: String, size: Int, cellModifier: Modifier) {
        AsyncImage(
            model = url.thumbnail(size) ?: url,
            contentDescription = null,
            placeholder = placeholder,
            contentScale = ContentScale.Crop,
            modifier = cellModifier
        )
    }

    if (urls.size == 1) {
        cell(urls.first(), 512, modifier)
    } else {
        Box(modifier = modifier) {
            listOf(
                Alignment.TopStart,
                Alignment.TopEnd,
                Alignment.BottomStart,
                Alignment.BottomEnd
            ).forEachIndexed { index, alignment ->
                urls.getOrNull(index)?.let { url ->
                    cell(
                        url,
                        256,
                        Modifier
                            .align(alignment)
                            .fillMaxSize(0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun QuadrantTint(
    urls: List<String>,
    modifier: Modifier = Modifier,
    tintAlpha: Float = 0.85f
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val context = LocalContext.current
    val bgColor = colorPalette.background1

    var colors by remember(urls) { mutableStateOf<List<Int>?>(null) }

    LaunchedEffect(urls) {
        if (urls.isEmpty()) {
            colors = null
            return@LaunchedEffect
        }
        colors = withContext(Dispatchers.IO) {
            urls.take(4).map { url ->
                runCatching {
                    val result = context.imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(url.thumbnail(256) ?: url)
                            .allowHardware(false)
                            .build()
                    )
                    if (result is SuccessResult) {
                        val palette = Palette.from(result.image.toBitmap()).generate()
                        palette.vibrantSwatch?.rgb ?: palette.getDominantColor(bgColor.toArgb())
                    } else bgColor.toArgb()
                }.getOrDefault(bgColor.toArgb())
            }
        }
    }

    val tintList = colors
    if (tintList == null || tintList.isEmpty()) return
    val avgColor = run {
        val count = tintList.size
        tintList.fold(Triple(0f, 0f, 0f)) { acc, c ->
            val col = Color(c)
            Triple(acc.first + col.red, acc.second + col.green, acc.third + col.blue)
        }.let { (r, g, b) -> Color(r / count, g / count, b / count, 1f) }
    }

    BoxWithConstraints(modifier = modifier) {
        val w = with(LocalDensity.current) { maxWidth.toPx() }
        val h = with(LocalDensity.current) { maxHeight.toPx() }
Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalFadingEdge(top = false, bottom = true, bottomSize = 5)
                .background(
                    Brush.radialGradient(
                        colors = listOf(avgColor.copy(alpha = tintAlpha), Color.Transparent),
                        center = Offset(w / 2f, h / 2f),
                        radius = sqrt(w * w + h * h) / 2f
                    )
                )
        )
    }
}
