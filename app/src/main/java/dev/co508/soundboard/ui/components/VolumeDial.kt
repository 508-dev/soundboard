package dev.co508.soundboard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.co508.soundboard.R
import dev.co508.soundboard.data.Sound
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Where the dial's arc begins, in degrees clockwise from 12 o'clock. */
private const val START_ANGLE_FROM_TOP = 210f

/** How far it sweeps. The remaining 60° is a dead zone at the bottom. */
private const val SWEEP_DEGREES = 300f

/**
 * Modal volume dial for one sound: tap or drag around the ring to set percent.
 *
 * Changes are applied to the live mix through [onPreview] as the finger moves,
 * so the user hears the result while dragging, but only written to disk via
 * [onCommit] when the dialog closes — a drag would otherwise be dozens of
 * DataStore writes.
 */
@Composable
fun VolumeDialDialog(
    sound: Sound,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var percent by remember(sound.id) { mutableIntStateOf(sound.volumePercent) }

    fun finish() {
        onCommit(percent)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = ::finish,
        title = { Text(stringResource(R.string.volume_for, sound.name)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VolumeDial(
                    percent = percent,
                    onPercentChange = {
                        percent = it
                        onPreview(it)
                    },
                )
                Text(
                    text = stringResource(R.string.volume_dial_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = ::finish) { Text(stringResource(R.string.done)) }
        },
    )
}

@Composable
private fun VolumeDial(
    percent: Int,
    onPercentChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val thumbColor = MaterialTheme.colorScheme.primary
    val dialLabel = stringResource(R.string.volume_percent, percent)

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(
            modifier =
                Modifier
                    .size(DIAL_SIZE)
                    .semantics { contentDescription = dialLabel }
                    .pointerInput(Unit) {
                        detectTapGestures { onPercentChange(percentAt(it, size)) }
                    }.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onPercentChange(percentAt(it, size)) },
                            onDrag = { change, _ ->
                                change.consume()
                                onPercentChange(percentAt(change.position, size))
                            },
                        )
                    },
        ) {
            val stroke = STROKE_WIDTH.toPx()
            val inset = stroke / 2f
            val diameter = min(size.width, size.height) - stroke
            val topLeft = Offset(inset, inset)
            val arcSize = Size(diameter, diameter)

            // drawArc measures from 3 o'clock; our angles are from 12 o'clock.
            val startAngle = START_ANGLE_FROM_TOP - 90f

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = SWEEP_DEGREES,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = startAngle,
                sweepAngle = SWEEP_DEGREES * percent / 100f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            val thumbAngle = Math.toRadians((START_ANGLE_FROM_TOP + SWEEP_DEGREES * percent / 100f - 90f).toDouble())
            val radius = diameter / 2f
            drawCircle(
                color = thumbColor,
                radius = stroke * 0.75f,
                center =
                    Offset(
                        x = center.x + radius * cos(thumbAngle).toFloat(),
                        y = center.y + radius * sin(thumbAngle).toFloat(),
                    ),
            )
            drawCircle(
                color = Color.White,
                radius = stroke * 0.3f,
                center =
                    Offset(
                        x = center.x + radius * cos(thumbAngle).toFloat(),
                        y = center.y + radius * sin(thumbAngle).toFloat(),
                    ),
            )
        }

        Text(
            text = stringResource(R.string.volume_percent, percent),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(8.dp),
        )
    }
}

/**
 * Maps a touch point to a volume percent.
 *
 * Angles run clockwise from 12 o'clock. Points inside the 60° dead zone at the
 * bottom snap to whichever end of the arc is nearer, so dragging past 100%
 * parks at 100% instead of wrapping around to 0%.
 */
internal fun percentAt(
    position: Offset,
    size: IntSize,
): Int {
    val dx = position.x - size.width / 2f
    val dy = position.y - size.height / 2f
    val angleFromTop = (Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat() + 360f) % 360f
    val travelled = (angleFromTop - START_ANGLE_FROM_TOP + 360f) % 360f

    if (travelled <= SWEEP_DEGREES) {
        return (travelled / SWEEP_DEGREES * 100f).roundToInt()
    }
    val deadZoneMidpoint = SWEEP_DEGREES + (360f - SWEEP_DEGREES) / 2f
    return if (travelled < deadZoneMidpoint) 100 else 0
}

private val DIAL_SIZE = 200.dp
private val STROKE_WIDTH = 18.dp
