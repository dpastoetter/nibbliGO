package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawP1Icon(
    icon: P1StatIcon,
    topLeft: Offset,
    scale: Float,
    color: Color,
) {
    val rows = P1StatIconBitmaps.bitmap(icon)
    rows.forEachIndexed { y, row ->
        row.forEachIndexed { x, ch ->
            if (ch == '#') {
                drawRect(
                    color = color,
                    topLeft = Offset(topLeft.x + x * scale, topLeft.y + y * scale),
                    size = Size(scale, scale),
                )
            }
        }
    }
}
