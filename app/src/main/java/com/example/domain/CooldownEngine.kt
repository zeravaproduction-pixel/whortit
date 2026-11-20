package com.example.domain

import com.example.model.ItemStatus
import com.example.model.VaultItem
import kotlin.math.max

object CooldownEngine {

    const val DURATION_24H_MS: Long = 24L * 60L * 60L * 1000L
    const val DURATION_48H_MS: Long = 48L * 60L * 60L * 1000L
    const val DURATION_72H_MS: Long = 72L * 60L * 60L * 1000L

    // Small tolerance (2 seconds) avoids false positives from millisecond clock jitter
    private const val BACKWARD_TOLERANCE_MS: Long = 2_000L

    fun createVaultItem(
        id: String,
        name: String,
        price: Long,
        productUrl: String,
        durationMs: Long,
        nowMs: Long
    ): VaultItem {
        val validDuration = when (durationMs) {
            DURATION_24H_MS, DURATION_48H_MS, DURATION_72H_MS -> durationMs
            else -> DURATION_48H_MS
        }
        return VaultItem(
            id = id,
            name = Formatters.sanitizeItemName(name),
            price = price,
            productUrl = productUrl.trim(),
            createdAt = nowMs,
            durationMs = validDuration,
            expiresAt = nowMs + validDuration,
            lastSeenAt = nowMs,
            status = ItemStatus.WAITING,
            clockTamperDetected = false
        )
    }

    fun getRemainingTime(item: VaultItem, nowMs: Long): Long {
        if (item.status != ItemStatus.WAITING) return 0L
        return max(0L, item.expiresAt - nowMs)
    }

    fun refreshVaultItems(items: List<VaultItem>, nowMs: Long): RefreshVaultResult {
        var changed = false
        var anyClockTamper = false

        val updated = items.map { item ->
            if (item.status == ItemStatus.WAITING) {
                val movedBackward = nowMs + BACKWARD_TOLERANCE_MS < item.lastSeenAt
                val nextTamper = item.clockTamperDetected || movedBackward
                if (nextTamper) anyClockTamper = true

                // Do NOT speed up timer or lower lastSeenAt if clock moved backward
                val nextLastSeen = max(item.lastSeenAt, nowMs)
                val nextStatus = if (!movedBackward && nowMs >= item.expiresAt) {
                    ItemStatus.READY
                } else {
                    ItemStatus.WAITING
                }

                if (nextTamper != item.clockTamperDetected ||
                    nextLastSeen != item.lastSeenAt ||
                    nextStatus != item.status
                ) {
                    changed = true
                    item.copy(
                        lastSeenAt = nextLastSeen,
                        status = nextStatus,
                        clockTamperDetected = nextTamper
                    )
                } else {
                    item
                }
            } else {
                if (item.clockTamperDetected) anyClockTamper = true
                item
            }
        }

        return RefreshVaultResult(
            items = updated,
            changed = changed,
            anyClockTamperDetected = anyClockTamper
        )
    }
}

data class RefreshVaultResult(
    val items: List<VaultItem>,
    val changed: Boolean,
    val anyClockTamperDetected: Boolean
)
