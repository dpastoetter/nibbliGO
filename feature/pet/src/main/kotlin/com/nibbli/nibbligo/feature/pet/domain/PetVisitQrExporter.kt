package com.nibbli.nibbligo.feature.pet.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.nibbli.nibbligo.core.model.PetState
import java.io.File
import java.io.FileOutputStream

object PetVisitQrExporter {

    fun shareVisitQr(context: Context, pet: PetState): Intent {
        val payload = PetVisitQrCodec.encode(pet)
        val qr = PetVisitQrGenerator.generate(payload, sizePx = 480)
        val framed = frameQr(qr, pet.name)
        val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(cacheDir, "nibbli_visit_qr.png")
        FileOutputStream(file).use { out ->
            framed.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Visit ${pet.name} on nibbliGO")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun qrBitmapForPet(pet: PetState, sizePx: Int = 280): Bitmap {
        val payload = PetVisitQrCodec.encode(pet)
        return PetVisitQrGenerator.generate(payload, sizePx = sizePx)
    }

    private fun frameQr(qr: Bitmap, petName: String): Bitmap {
        val pad = 48
        val captionHeight = 72
        val width = qr.width + pad * 2
        val height = qr.height + pad * 2 + captionHeight
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(android.graphics.Color.parseColor("#F0E6DA"))
        canvas.drawBitmap(qr, pad.toFloat(), pad.toFloat(), null)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1A1A1E")
            textSize = 36f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#4A5D3A")
            textSize = 24f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val y = qr.height + pad + 36f
        canvas.drawText("Visit $petName", width / 2f, y, titlePaint)
        canvas.drawText("Scan to visit · 24h", width / 2f, y + 32f, subtitlePaint)
        return output
    }
}
