package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LcdBackground = Color(0xFF9EAD86)
val DeviceBezel = Color(0xFFE8D5C4)
val DeviceBezelDark = Color(0xFFC4A88E)

@Composable
fun PixelDeviceFrame(
    modifier: Modifier = Modifier,
    roomColor: Color = LcdBackground,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DeviceBezel)
            .border(4.dp, DeviceBezelDark, RoundedCornerShape(24.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(roomColor)
                .border(2.dp, Color(0xFF6B7B5C), RoundedCornerShape(8.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

fun roomColorFor(roomId: String): Color = when (roomId) {
    "cozy" -> Color(0xFF9EAD86)
    "sunset" -> Color(0xFFD4A574)
    "night" -> Color(0xFF6B7B9E)
    else -> LcdBackground
}
