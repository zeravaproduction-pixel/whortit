package com.example.share

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.domain.Formatters
import com.example.model.EvaluationRecord
import com.example.model.VerdictType
import java.io.File
import java.io.FileOutputStream

object ShareCardGenerator {

    const val CARD_WIDTH = 1080
    const val CARD_HEIGHT = 1350

    fun buildShareText(evaluation: EvaluationRecord): String {
        val ratioText = Formatters.formatPercent(evaluation.priceRatio)
        val priceText = Formatters.formatRupiah(evaluation.itemPrice)
        return buildString {
            appendLine("WorthIt? — Hasil Refleksi Belanja")
            appendLine("Barang: ${evaluation.itemName}")
            appendLine("Harga: $priceText")
            appendLine("Skor Akhir: ${evaluation.finalScore}/100")
            appendLine("• Behavioral Score: ${evaluation.behavioralScore}/100")
            appendLine("• Financial Penalty: -${evaluation.financialPenalty}")
            appendLine("• Rasio Dana Bebas: $ratioText")
            appendLine("Vonis: ${evaluation.verdict.title}")
            appendLine("\"${evaluation.verdict.copywriting}\"")
            append("Pause. Think. Then Decide.")
        }
    }

    // PRD 4.4: 1080x1350 px PNG rendered with Canvas API using system fonts and no external service
    fun renderShareCardBitmap(evaluation: EvaluationRecord): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val warmCream = 0xFFF7F4EC.toInt()
        val softWhite = 0xFFFFFFFF.toInt()
        val sageGreen = 0xFF52785B.toInt()
        val paleSage = 0xFFDCE9DC.toInt()
        val softApricot = 0xFFF5E3C9.toInt()
        val deepCharcoal = 0xFF303832.toInt()
        val warmGray = 0xFF77766F.toInt()
        val borderWarmGray = 0xFFE8E3D8.toInt()

        val (statusBg, statusFg) = when (evaluation.verdict) {
            VerdictType.WORTH_IT -> 0xFFE3F0E3.toInt() to 0xFF214E2C.toInt()
            VerdictType.RECONSIDER -> 0xFFFFF0D5.toInt() to 0xFF744807.toInt()
            VerdictType.LIKELY_IMPULSIVE -> 0xFFF9E2DE.toInt() to 0xFF872920.toInt()
        }

        // Background
        canvas.drawColor(warmCream)

        // Subtle organic top-right apricot circle
        val decorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = softApricot }
        canvas.drawCircle(CARD_WIDTH - 90f, 110f, 180f, decorPaint)
        decorPaint.color = paleSage
        canvas.drawCircle(110f, CARD_HEIGHT - 90f, 150f, decorPaint)

        // Main card surface
        val cardRect = RectF(64f, 64f, CARD_WIDTH - 64f, CARD_HEIGHT - 64f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = softWhite }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderWarmGray
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint)
        canvas.drawRoundRect(cardRect, 40f, 40f, borderPaint)

        // Brand pill
        val brandRect = RectF(112f, 118f, 360f, 178f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = paleSage }
        canvas.drawRoundRect(brandRect, 30f, 30f, pillPaint)

        val brandTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sageGreen
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("WorthIt?", 148f, 159f, brandTextPaint)

        val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warmGray
            textSize = 26f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(
            Formatters.formatIsoDate(evaluation.createdAt),
            CARD_WIDTH - 112f,
            158f,
            datePaint
        )

        // Item name & price
        val itemTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deepCharcoal
            textSize = 48f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val titleLayout = StaticLayout.Builder.obtain(
            evaluation.itemName,
            0,
            evaluation.itemName.length,
            itemTitlePaint,
            CARD_WIDTH - 224
        ).setMaxLines(2).build()
        canvas.save()
        canvas.translate(112f, 224f)
        titleLayout.draw(canvas)
        canvas.restore()

        val priceY = 224f + titleLayout.height + 54f
        val pricePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sageGreen
            textSize = 56f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(Formatters.formatRupiah(evaluation.itemPrice), 112f, priceY, pricePaint)

        // Score hero box
        val scoreBoxTop = priceY + 48f
        val scoreBoxRect = RectF(112f, scoreBoxTop, CARD_WIDTH - 112f, scoreBoxTop + 280f)
        val statusBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusBg }
        canvas.drawRoundRect(scoreBoxRect, 28f, 28f, statusBgPaint)

        val scoreLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusFg
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("SKOR AKHIR", 152f, scoreBoxTop + 62f, scoreLabelPaint)

        val bigScorePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusFg
            textSize = 112f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("${evaluation.finalScore}/100", 152f, scoreBoxTop + 178f, bigScorePaint)

        val verdictTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusFg
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(evaluation.verdict.title, 152f, scoreBoxTop + 236f, verdictTitlePaint)

        // Metrics row
        val metricsTop = scoreBoxTop + 318f
        val colWidth = (CARD_WIDTH - 224f - 32f) / 3f
        drawMetricBox(
            canvas,
            RectF(112f, metricsTop, 112f + colWidth, metricsTop + 148f),
            "Behavioral",
            "${evaluation.behavioralScore}/100",
            warmCream,
            borderWarmGray,
            warmGray,
            deepCharcoal
        )
        drawMetricBox(
            canvas,
            RectF(112f + colWidth + 16f, metricsTop, 112f + 2 * colWidth + 16f, metricsTop + 148f),
            "Penalty",
            "-${evaluation.financialPenalty}",
            warmCream,
            borderWarmGray,
            warmGray,
            deepCharcoal
        )
        drawMetricBox(
            canvas,
            RectF(112f + 2 * colWidth + 32f, metricsTop, CARD_WIDTH - 112f, metricsTop + 148f),
            "Rasio Dana",
            Formatters.formatPercent(evaluation.priceRatio),
            warmCream,
            borderWarmGray,
            warmGray,
            deepCharcoal
        )

        // Copywriting quote
        val quoteTop = metricsTop + 190f
        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deepCharcoal
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val quoteText = "\"${evaluation.verdict.copywriting}\""
        val quoteLayout = StaticLayout.Builder.obtain(
            quoteText,
            0,
            quoteText.length,
            quotePaint,
            CARD_WIDTH - 224
        ).setLineSpacing(8f, 1f).setMaxLines(3).build()
        canvas.save()
        canvas.translate(112f, quoteTop)
        quoteLayout.draw(canvas)
        canvas.restore()

        // Footer disclaimer
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warmGray
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(
            "Pause. Think. Then Decide. • Alat bantu refleksi belanja",
            112f,
            CARD_HEIGHT - 122f,
            footerPaint
        )

        return bitmap
    }

    private fun drawMetricBox(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        bgColor: Int,
        borderColor: Int,
        labelColor: Int,
        valueColor: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 22f, 22f, bgPaint)
        canvas.drawRoundRect(rect, 22f, 22f, borderPaint)

        val lPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val vPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = valueColor
            textSize = 38f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(label, rect.left + 24f, rect.top + 52f, lPaint)
        canvas.drawText(value, rect.left + 24f, rect.top + 112f, vPaint)
    }

    fun savePngToPictures(
        context: Context,
        bitmap: Bitmap,
        itemName: String
    ): Result<Uri> {
        return try {
            val safeName = itemName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "barang" }
            val fileName = "worthit-$safeName-${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/WorthIt")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Gagal membuat file gambar di Galeri."))

                resolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        return Result.failure(IllegalStateException("Gagal menulis PNG."))
                    }
                } ?: return Result.failure(IllegalStateException("Tidak dapat membuka output gambar."))

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Result.success(uri)
            } else {
                createShareablePngUri(context, bitmap)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createShareablePngUri(context: Context, bitmap: Bitmap): Result<Uri> {
        return try {
            val dir = File(context.cacheDir, "share_cards").apply { mkdirs() }
            val file = File(dir, "worthit-card-${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    return Result.failure(IllegalStateException("Gagal membuat file PNG."))
                }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
