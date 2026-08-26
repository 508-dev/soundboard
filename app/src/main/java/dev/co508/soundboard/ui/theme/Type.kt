package dev.co508.soundboard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Material3's default type scale, with a bolder display style for the volume
// dial's centre readout (see ui/components/VolumeDial.kt).
val AppTypography =
    Typography(
        displaySmall =
            TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                lineHeight = 46.sp,
            ),
    )
