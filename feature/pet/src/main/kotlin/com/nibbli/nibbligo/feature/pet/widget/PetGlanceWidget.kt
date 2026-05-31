package com.nibbli.nibbligo.feature.pet.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class PetGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = PetWidgetSnapshot.read(context)
        val launch = Intent(Intent.ACTION_MAIN).apply {
            setClassName(context.packageName, "com.nibbli.nibbligo.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        provideContent {
            PetWidgetContent(snapshot, launch)
        }
    }
}

@Composable
private fun PetWidgetContent(snapshot: PetWidgetSnapshot.Snapshot, launch: Intent) {
    Column(
        modifier = GlanceModifier
            .padding(12.dp)
            .clickable(actionStartActivity(launch)),
    ) {
        Row(modifier = GlanceModifier.padding(bottom = 4.dp)) {
            Text(
                text = snapshot.glyph,
                style = TextStyle(fontSize = 28.sp, color = ColorProvider(android.graphics.Color.parseColor("#9AE66E"))),
                modifier = GlanceModifier.padding(end = 8.dp),
            )
            Column {
                Text(
                    text = snapshot.name,
                    style = TextStyle(fontSize = 16.sp, color = ColorProvider(android.graphics.Color.WHITE)),
                )
                Text(
                    text = snapshot.stage.lowercase().replaceFirstChar { it.titlecase() },
                    style = TextStyle(fontSize = 12.sp, color = ColorProvider(android.graphics.Color.LTGRAY)),
                )
            }
        }
        if (snapshot.cosmetic.isNotBlank()) {
            Text(
                text = snapshot.cosmetic.replace('_', ' ').lowercase(),
                style = TextStyle(fontSize = 10.sp, color = ColorProvider(android.graphics.Color.parseColor("#9AE66E"))),
            )
        }
        if (snapshot.need != "NONE") {
            Text(
                text = "Needs: ${snapshot.need.lowercase()}",
                style = TextStyle(fontSize = 11.sp, color = ColorProvider(android.graphics.Color.YELLOW)),
            )
        }
        if (snapshot.streakDays > 0) {
            Text(
                text = "Streak ${snapshot.streakDays}d · mood ${snapshot.mood}",
                style = TextStyle(fontSize = 10.sp, color = ColorProvider(android.graphics.Color.LTGRAY)),
            )
        }
    }
}
