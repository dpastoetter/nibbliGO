package com.nibbli.nibbligo.feature.pet.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import com.nibbli.nibbligo.core.designsystem.theme.NibbliTheme
import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.share.PetShareCard
import com.nibbli.nibbligo.feature.pet.ui.share.PetShareCardKind
import java.io.File
import java.io.FileOutputStream

object PetShareExporter {

    fun catchChallengeUri(score: Int): String =
        "nibbli://challenge/catch?score=$score"

    fun renderCard(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        content: @Composable () -> Unit,
    ): Bitmap {
        val composeView = ComposeView(context)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent { NibbliTheme { content() } }
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        composeView.measure(widthSpec, heightSpec)
        composeView.layout(0, 0, widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        composeView.draw(canvas)
        return bitmap
    }

    fun shareTodayCard(context: Context, pet: PetState): Intent =
        shareCard(context, pet, "nibbli_today.png") {
            PetShareCard(kind = PetShareCardKind.TODAY, pet = pet)
        }

    fun shareEvolutionCard(context: Context, pet: PetState, stage: LifeStage): Intent =
        shareCard(context, pet, "nibbli_evolved.png") {
            PetShareCard(kind = PetShareCardKind.EVOLUTION, pet = pet, evolutionStage = stage)
        }

    fun shareCatchCard(
        context: Context,
        pet: PetState,
        score: Int,
        combo: Int,
    ): Intent {
        val link = if (score > 0) catchChallengeUri(score) else null
        return shareCard(context, pet, "nibbli_catch.png") {
            PetShareCard(
                kind = PetShareCardKind.CATCH,
                pet = pet,
                catchScore = score,
                catchCombo = combo,
                challengeLink = link,
            )
        }
    }

    fun shareQuoteCard(context: Context, pet: PetState, line: String): Intent =
        shareCard(context, pet, "nibbli_quote.png") {
            PetShareCard(kind = PetShareCardKind.QUOTE, pet = pet, quoteLine = line)
        }

    private fun shareCard(
        context: Context,
        pet: PetState,
        fileName: String,
        content: @Composable () -> Unit,
    ): Intent {
        val density = context.resources.displayMetrics.density
        val widthPx = (360 * density).toInt()
        val heightPx = (640 * density).toInt()
        val bitmap = renderCard(context, widthPx, heightPx, content)
        val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${pet.name} on nibbliGO — raised on-device!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
