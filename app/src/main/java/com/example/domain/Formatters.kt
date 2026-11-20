package com.example.domain

import java.net.URI
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Formatters {

    const val MAX_REASONABLE_IDR: Long = 1_000_000_000_000L // 1 Triliun IDR
    const val MAX_ITEM_NAME_LENGTH: Int = 80
    const val DEFAULT_UNNAMED_ITEM: String = "Barang tanpa nama"

    private val idLocale = Locale("id", "ID")
    private val groupingSymbols = DecimalFormatSymbols(idLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    fun formatRupiah(amount: Long): String {
        val formatter = DecimalFormat("#,##0", groupingSymbols)
        val formatted = formatter.format(abs(amount))
        return if (amount < 0) "-Rp$formatted" else "Rp$formatted"
    }

    fun formatRupiah(amount: Double): String {
        if (!amount.isFinite()) return "Rp0"
        return formatRupiah(amount.roundToLong())
    }

    fun formatInputGrouping(digitsOnly: String): String {
        val clean = digitsOnly.filter { it.isDigit() }
        if (clean.isEmpty()) return ""
        val value = clean.toLongOrNull() ?: return clean
        val formatter = DecimalFormat("#,##0", groupingSymbols)
        return formatter.format(value)
    }

    fun parsePositivePrice(raw: String): ParsePriceResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return ParsePriceResult.Invalid("Masukkan harga barang terlebih dahulu.")
        }
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty() || trimmed.startsWith("-")) {
            return ParsePriceResult.Invalid("Harga harus berupa angka lebih dari Rp0.")
        }
        val parsed = digits.toLongOrNull()
            ?: return ParsePriceResult.Invalid("Nominal terlalu besar untuk diproses.")
        if (parsed <= 0L) {
            return ParsePriceResult.Invalid("Harga barang harus lebih dari Rp0.")
        }
        if (parsed > MAX_REASONABLE_IDR) {
            return ParsePriceResult.Invalid("Nominal maksimal adalah ${formatRupiah(MAX_REASONABLE_IDR)}.")
        }
        return ParsePriceResult.Valid(parsed)
    }

    fun parseNonNegativeAmount(raw: String, fieldLabel: String): ParseAmountResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ParseAmountResult.Valid(0L)
        if (trimmed.startsWith("-")) {
            return ParseAmountResult.Invalid("$fieldLabel tidak boleh negatif.")
        }
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) {
            return ParseAmountResult.Invalid("$fieldLabel harus berupa angka valid.")
        }
        val parsed = digits.toLongOrNull()
            ?: return ParseAmountResult.Invalid("$fieldLabel terlalu besar.")
        if (parsed > MAX_REASONABLE_IDR) {
            return ParseAmountResult.Invalid("$fieldLabel melebihi batas wajar.")
        }
        return ParseAmountResult.Valid(parsed)
    }

    fun sanitizeItemName(raw: String): String {
        val normalized = raw.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) return DEFAULT_UNNAMED_ITEM
        return normalized.take(MAX_ITEM_NAME_LENGTH)
    }

    fun truncateNameInput(raw: String): String {
        return if (raw.length > MAX_ITEM_NAME_LENGTH) raw.take(MAX_ITEM_NAME_LENGTH) else raw
    }

    fun validateProductUrl(raw: String): UrlValidationResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return UrlValidationResult.Valid("")
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            val host = uri.host
            if (scheme !in setOf("http", "https")) {
                UrlValidationResult.Invalid("Link hanya boleh menggunakan protokol http:// atau https://.")
            } else if (host.isNullOrBlank()) {
                UrlValidationResult.Invalid("Link produk tidak valid. Pastikan alamat situs lengkap.")
            } else {
                UrlValidationResult.Valid(trimmed)
            }
        } catch (_: Exception) {
            UrlValidationResult.Invalid("Format link produk tidak valid.")
        }
    }

    fun formatDecimal(value: Double, maxDecimals: Int = 1): String {
        if (!value.isFinite()) return "0"
        val pattern = if (maxDecimals <= 0) "#,##0" else "#,##0." + "#".repeat(maxDecimals)
        val formatter = DecimalFormat(pattern, groupingSymbols)
        return formatter.format(value)
    }

    fun formatPercent(ratioDecimal: Double): String {
        if (ratioDecimal.isInfinite() || ratioDecimal.isNaN()) return ">100%"
        val pct = ratioDecimal * 100.0
        return if (pct >= 100.0) {
            "${formatDecimal(pct, 0)}%"
        } else {
            "${formatDecimal(pct, 1)}%"
        }
    }

    fun formatCountdown(remainingMs: Long): String {
        val safeMs = remainingMs.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.ROOT, "%02dj %02dm %02dd", hours, minutes, seconds)
    }

    fun formatDateTime(epochMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", idLocale)
        return sdf.format(Date(epochMs))
    }

    fun formatIsoDate(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        return sdf.format(Date(epochMs))
    }
}

sealed class ParsePriceResult {
    data class Valid(val amount: Long) : ParsePriceResult()
    data class Invalid(val message: String) : ParsePriceResult()
}

sealed class ParseAmountResult {
    data class Valid(val amount: Long) : ParseAmountResult()
    data class Invalid(val message: String) : ParseAmountResult()
}

sealed class UrlValidationResult {
    data class Valid(val url: String) : UrlValidationResult()
    data class Invalid(val message: String) : UrlValidationResult()
}
