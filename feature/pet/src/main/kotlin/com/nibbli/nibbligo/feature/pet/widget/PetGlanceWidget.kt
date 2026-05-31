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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class PetGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = PetWidgetSnapshot.read(context)
        val launch = homeIntent(context)
        provideContent {
            PetWidgetContent(context, snapshot, launch)
        }
    }
}

private fun homeIntent(context: Context): Intent =
    Intent(Intent.ACTION_MAIN).apply {
        setClassName(context.packageName, "com.nibbli.nibbligo.MainActivity")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

private fun widgetActionIntent(context: Context, action: String): Intent =
    homeIntent(context).putExtra(PetWidgetActions.EXTRA, action)

@Composable
private fun PetWidgetContent(
    context: Context,
    snapshot: PetWidgetSnapshot.Snapshot,
    launch: Intent,
) {
    val lcdGreen = ColorProvider(android.graphics.Color.parseColor("#8BAF6A"))
    val lcdBg = ColorProvider(android.graphics.Color.parseColor("#2A4A2E"))
    val lcdBorder = ColorProvider(android.graphics.Color.parseColor("#1E3320"))
    val accent = ColorProvider(android.graphics.Color.parseColor("#9AE66E"))
    val muted = ColorProvider(android.graphics.Color.parseColor("#B8C4B0"))

    Column(
        modifier = GlanceModifier
            .padding(10.dp)
            .clickable(actionStartActivity(launch)),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(72.dp)
                .cornerRadius(6.dp)
                .background(lcdBorder)
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .cornerRadius(4.dp)
                    .background(lcdBg)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = snapshot.glyph,
                        style = TextStyle(
                            fontSize = 26.sp,
                            color = lcdGreen,
                            textAlign = TextAlign.Center,
                        ),
                    )
                    if (snapshot.need != "NONE") {
                        Text(
                            text = "Needs ${snapshot.need.lowercase()}",
                            style = TextStyle(fontSize = 9.sp, color = accent),
                        )
                    }
                }
            }
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = snapshot.name,
                    style = TextStyle(fontSize = 14.sp, color = ColorProvider(android.graphics.Color.WHITE)),
                )
                Text(
                    text = snapshot.stage.lowercase().replaceFirstChar { it.titlecase() },
                    style = TextStyle(fontSize = 11.sp, color = muted),
                )
            }
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(
                text = "Feed",
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(end = 4.dp)
                    .cornerRadius(8.dp)
                    .background(lcdBorder)
                    .padding(vertical = 6.dp)
                    .clickable(actionStartActivity(widgetActionIntent(context, PetWidgetActions.FEED))),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = accent,
                    textAlign = TextAlign.Center,
                ),
            )
            Text(
                text = "Talk",
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(start = 4.dp)
                    .cornerRadius(8.dp)
                    .background(lcdBorder)
                    .padding(vertical = 6.dp)
                    .clickable(actionStartActivity(widgetActionIntent(context, PetWidgetActions.TALK))),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = accent,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        if (snapshot.streakDays > 0) {
            Text(
                text = "Streak ${snapshot.streakDays}d · mood ${snapshot.mood}",
                modifier = GlanceModifier.padding(top = 4.dp),
                style = TextStyle(fontSize = 10.sp, color = muted),
            )
        }
    }
}
