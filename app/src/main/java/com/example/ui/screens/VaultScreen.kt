package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.CooldownEngine
import com.example.domain.Formatters
import com.example.model.ItemStatus
import com.example.model.VaultItem
import com.example.ui.components.ClockTamperBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LabeledInputField
import com.example.ui.components.RupiahPreviewText
import com.example.ui.components.StatusBadge
import com.example.ui.components.StatusTone
import com.example.ui.components.WorthItCard
import com.example.ui.components.WorthItSecondaryButton
import com.example.ui.theme.BorderWarmGray
import com.example.ui.theme.DarkAmberText
import com.example.ui.theme.DarkCoralText
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.PaleSage
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmGray
import com.example.viewmodel.WorthItUiState

private enum class VaultFilter(val label: String) {
    ACTIVE("Aktif"),
    WAITING("Menunggu"),
    READY("Siap"),
    DONE("Selesai"),
    ARCHIVED("Arsip"),
    ALL("Semua")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VaultScreen(
    state: WorthItUiState,
    onAddManualVaultItem: (String, String, String, Long) -> Result<Unit>,
    onDecideVaultItem: (String, Boolean) -> Unit,
    onArchiveVaultItem: (String) -> Unit,
    onOpenProductUrl: (String) -> Unit,
    onStartCheckItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(VaultFilter.ACTIVE) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(state.appData.items, selectedFilter) {
        when (selectedFilter) {
            VaultFilter.ACTIVE -> state.appData.items.filter {
                it.status == ItemStatus.WAITING || it.status == ItemStatus.READY
            }
            VaultFilter.WAITING -> state.appData.items.filter { it.status == ItemStatus.WAITING }
            VaultFilter.READY -> state.appData.items.filter { it.status == ItemStatus.READY }
            VaultFilter.DONE -> state.appData.items.filter {
                it.status == ItemStatus.CANCELLED || it.status == ItemStatus.PURCHASED
            }
            VaultFilter.ARCHIVED -> state.appData.items.filter { it.status == ItemStatus.ARCHIVED }
            VaultFilter.ALL -> state.appData.items
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cooldown Vault",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepCharcoal
                    )
                    Text(
                        text = "Biarkan keinginan ini duduk sebentar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmGray
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen,
                        contentColor = SoftWhite
                    ),
                    modifier = Modifier.testTag("vault_add_manual_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah")
                }
            }
        }

        if (state.anyClockTamperDetected) {
            item {
                ClockTamperBanner()
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VaultFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PaleSage,
                            selectedLabelColor = DeepCharcoal
                        ),
                        modifier = Modifier.testTag("vault_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum ada barang di kategori ini",
                    description = "Simpan barang yang bikin tergoda ke Cooldown Vault selama 24–72 jam sebelum memutuskan membeli.",
                    ctaLabel = "Cek Barang Baru",
                    onCtaClick = onStartCheckItem
                )
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                VaultItemCard(
                    item = item,
                    nowMs = state.nowMs,
                    onCancelPurchase = { onDecideVaultItem(item.id, true) },
                    onStillNeed = { onDecideVaultItem(item.id, false) },
                    onOpenLink = { onOpenProductUrl(item.productUrl) },
                    onArchive = { onArchiveVaultItem(item.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showAddDialog) {
        AddManualVaultDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, priceDigits, url, durationMs ->
                onAddManualVaultItem(name, priceDigits, url, durationMs)
            }
        )
    }
}

@Composable
private fun VaultItemCard(
    item: VaultItem,
    nowMs: Long,
    onCancelPurchase: () -> Unit,
    onStillNeed: () -> Unit,
    onOpenLink: () -> Unit,
    onArchive: () -> Unit
) {
    val remainingMs = CooldownEngine.getRemainingTime(item, nowMs)
    val durationHours = item.durationMs / 3_600_000L

    WorthItCard(modifier = Modifier.testTag("vault_item_${item.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepCharcoal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Formatters.formatRupiah(item.price),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SageGreen
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            when (item.status) {
                ItemStatus.WAITING -> StatusBadge(
                    text = "Menunggu",
                    tone = StatusTone.NEUTRAL,
                    icon = Icons.Outlined.LockClock
                )
                ItemStatus.READY -> StatusBadge(
                    text = "Siap Diputuskan",
                    tone = StatusTone.WARNING
                )
                ItemStatus.CANCELLED -> StatusBadge(
                    text = "Dibatalkan",
                    tone = StatusTone.POSITIVE,
                    icon = Icons.Outlined.CheckCircle
                )
                ItemStatus.PURCHASED -> StatusBadge(
                    text = "Masih Butuh",
                    tone = StatusTone.NEUTRAL,
                    icon = Icons.Outlined.ShoppingBag
                )
                ItemStatus.ARCHIVED -> StatusBadge(
                    text = "Diarsipkan",
                    tone = StatusTone.NEUTRAL,
                    icon = Icons.Outlined.Archive
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Jeda $durationHours jam • Ditambahkan ${Formatters.formatDateTime(item.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray
        )

        if (item.clockTamperDetected) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Terdeteksi perubahan jam mundur pada perangkat untuk item ini.",
                style = MaterialTheme.typography.bodySmall,
                color = DarkAmberText
            )
        }

        when (item.status) {
            ItemStatus.WAITING -> {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarmCream,
                    border = BorderStroke(1.dp, BorderWarmGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Sisa waktu jeda",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmGray
                        )
                        Text(
                            text = Formatters.formatCountdown(remainingMs),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = SageGreen,
                            modifier = Modifier.testTag("vault_countdown_${item.id}")
                        )
                        if (item.productUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = WarmGray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Link produk terkunci hingga jeda selesai",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmGray
                                )
                            }
                        }
                    }
                }
            }

            ItemStatus.READY -> {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Masa jeda selesai. Setelah dipikirkan ulang, bagaimana keputusanmu?",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepCharcoal
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCancelPurchase,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("vault_cancel_${item.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreen,
                            contentColor = SoftWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DoNotDisturbOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Batal Beli")
                    }
                    OutlinedButton(
                        onClick = onStillNeed,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("vault_buy_${item.id}"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SageGreen)
                    ) {
                        Text("Masih Butuh", color = SageGreen)
                    }
                }
            }

            ItemStatus.CANCELLED -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Uangmu aman. Keputusanmu juga. Nilai barang tercatat di Savings Tracker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SageGreen
                )
            }

            ItemStatus.PURCHASED -> {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Kamu memilih melanjutkan pembelian. Catatan: status ini bukan verifikasi transaksi aktual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (item.productUrl.isNotBlank()) {
                        WorthItSecondaryButton(
                            text = "Buka Produk",
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            onClick = onOpenLink,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vault_open_url_${item.id}")
                        )
                    }
                    OutlinedButton(
                        onClick = onArchive,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("vault_archive_${item.id}"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderWarmGray)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Archive,
                            contentDescription = null,
                            tint = WarmGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Arsipkan", color = DeepCharcoal)
                    }
                }
            }

            ItemStatus.ARCHIVED -> {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Item diarsipkan dan tetap tersedia di riwayat keputusan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddManualVaultDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long) -> Result<Unit>
) {
    var name by remember { mutableStateOf("") }
    var priceDigits by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var durationMs by remember { mutableLongStateOf(CooldownEngine.DURATION_48H_MS) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoftWhite,
        title = {
            Text(
                text = "Tambah ke Cooldown Vault",
                style = MaterialTheme.typography.titleLarge,
                color = DeepCharcoal
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LabeledInputField(
                    label = "Nama Barang (Opsional)",
                    value = name,
                    onValueChange = { name = Formatters.truncateNameInput(it) },
                    placeholder = "Contoh: Sepatu Lari"
                )
                LabeledInputField(
                    label = "Harga Barang",
                    value = priceDigits,
                    onValueChange = {
                        priceDigits = it.filter { c -> c.isDigit() }.take(13)
                        errorMsg = null
                    },
                    placeholder = "350000",
                    prefixText = "Rp ",
                    keyboardType = KeyboardType.Number
                )
                RupiahPreviewText(digits = priceDigits)
                LabeledInputField(
                    label = "Link Produk (Opsional)",
                    value = url,
                    onValueChange = {
                        url = it
                        errorMsg = null
                    },
                    placeholder = "https://contoh.com/produk",
                    keyboardType = KeyboardType.Uri
                )
                Text(
                    text = "Durasi Jeda",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepCharcoal
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        CooldownEngine.DURATION_24H_MS to "24 Jam",
                        CooldownEngine.DURATION_48H_MS to "48 Jam",
                        CooldownEngine.DURATION_72H_MS to "72 Jam"
                    ).forEach { (dur, label) ->
                        FilterChip(
                            selected = durationMs == dur,
                            onClick = { durationMs = dur },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaleSage,
                                selectedLabelColor = DeepCharcoal
                            )
                        )
                    }
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkCoralText
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, priceDigits, url, durationMs).fold(
                        onSuccess = { onDismiss() },
                        onFailure = { errorMsg = it.message }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SageGreen,
                    contentColor = SoftWhite
                )
            ) {
                Text("Simpan ke Vault")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = WarmGray)
            }
        }
    )
}
