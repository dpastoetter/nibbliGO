package com.nibbli.nibbligo.feature.pet.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
class PetWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = PetGlanceWidget()
}

object PetWidgetUpdater {
  suspend fun refresh(context: Context) {
    PetGlanceWidget().updateAll(context)
  }
}
