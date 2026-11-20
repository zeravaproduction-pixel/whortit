package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CooldownEngine
import com.example.domain.Formatters
import com.example.model.ItemStatus
import com.example.model.VaultItem
import com.example.ui.components.ClockTamperBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrganicHeaderBanner
import com.example.ui.components.StatusBadge
import com.example.ui.components.StatusTone
import com.example.ui.components.WorthItCard
import com.example.ui.components.WorthItPrimaryButton
import com.example.ui.theme.BorderWarmGray
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.PaleSage
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftApricot
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WarmGray
import com.example.viewmodel.WorthItUiState

@Composable
fun HomeScreen(
    state: WorthItUiState,
    onStartCheckItem: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToSavings: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeVaultItems = state.appData.items.filter {
        it.status == ItemStatus.WAITING || it.status == ItemStatus.READY
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OrganicHeaderBanner(
                title = "Sebelum checkout, cek dulu.",
                subtitle = "Pause. Think. Then Decide."
            )
        }

        if (state.anyClockTamperDetected) {
            item {
                ClockTamperBanner()
            }
        }

        item {
            WorthItCard {
                Text(
                    text = "Lagi mau checkout sesuatu?",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepCharcoal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ubah harga barang menjadi waktu kerja & kebutuhan harian, lalu uji alasanmu selama 60 detik.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray
                )
                Spacer(modifier = Modifier.height(14.dp))
                WorthItPrimaryButton(
                    text = "Cek Barang Sekarang",
                    icon = Icons.Outlined.Search,
                    onClick = onStartCheckItem,
                    modifier = Modifier.testTag("home_cta_check_item")
                )
            }
        }

        // Financial Profile snapshot card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_profile_status_card")
                    .clickable(onClick = onNavigateToSettings),
                shape = RoundedCornerShape(16.dp),
                color = if (state.isFinancialProfileConfigured) SoftWhite else SoftApricot.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, BorderWarmGray)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PaleSage,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = SageGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isFinancialProfileConfigured) {
                                "Dana Bebas Bulanan"
                            } else {
                                "Profil Finansial Belum Diatur"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepCharcoal
                        )
                        Text(
                            text = if (state.isFinancialProfileConfigured) {
                                val freeText = Formatters.formatRupiah(state.financialSnapshot.freeFunds.coerceAtLeast(0.0))
                                if (state.financialSnapshot.freeFunds <= 0.0) {
                                    "Rp0 (pengeluaran ≥ pemasukan)"
                                } else {
                                    "$freeText / bulan"
                                }
                            } else {
                                "Ketuk untuk mengisi pemasukan & pengeluaran wajib"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Buka Pengaturan",
                        tint = SageGreen
                    )
                }
            }
        }

        // Savings summary card
        item {
            WorthItCard(
                containerColor = SoftGreen.copy(alpha = 0.55f),
                borderColor = PaleSage,
                modifier = Modifier
                    .testTag("home_savings_card")
                    .clickable(onClick = onNavigateToSavings)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Savings,
                            contentDescription = null,
                            tint = SageGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nilai Terselamatkan",
                            style = MaterialTheme.typography.titleSmall,
                            color = DeepCharcoal
                        )
                    }
                    TextButton(onClick = onNavigateToSavings) {
                        Text("Detail", color = SageGreen)
                    }
                }
                Text(
                    text = Formatters.formatRupiah(state.savingsSummary.totalSaved),
                    style = MaterialTheme.typography.displaySmall,
                    color = SageGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.savingsSummary.cancelledCount} barang ditandai batal beli • \"Sedikit demi sedikit, uangmu kembali ke kendalimu.\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepCharcoal
                )
            }
        }

        // Active Cooldown Vault preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cooldown Vault Aktif (${activeVaultItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepCharcoal
                )
                TextButton(
                    onClick = onNavigateToVault,
                    modifier = Modifier.testTag("home_see_all_vault")
                ) {
                    Text("Lihat Vault", color = SageGreen)
                }
            }
        }

        if (activeVaultItems.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum ada barang yang dijeda",
                    description = "Saat ragu ingin membeli sesuatu, masukkan ke Cooldown Vault 24–72 jam agar keinginan itu sempat mendingin.",
                    ctaLabel = "Mulai Cek Barang",
                    onCtaClick = onStartCheckItem
                )
            }
        } else {
            items(activeVaultItems.take(3), key = { it.id }) { item ->
                HomeVaultPreviewCard(
                    item = item,
                    nowMs = state.nowMs,
                    onClick = onNavigateToVault
                )
            }
        }

        item {
            Text(
                text = "WorthIt? menyimpan data secara lokal di perangkat ini tanpa akun. Ini adalah alat bantu refleksi belanja, bukan nasihat keuangan profesional.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun HomeVaultPreviewCard(
    item: VaultItem,
    nowMs: Long,
    onClick: () -> Unit
) {
    val remaining = CooldownEngine.getRemainingTime(item, nowMs)
    WorthItCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = DeepCharcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Formatters.formatRupiah(item.price),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SageGreen
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (item.status == ItemStatus.READY) {
                StatusBadge(
                    text = "Siap Diputuskan",
                    tone = StatusTone.WARNING
                )
            } else {
                StatusBadge(
                    text = Formatters.formatCountdown(remaining),
                    tone = StatusTone.NEUTRAL,
                    icon = Icons.Outlined.LockClock
                )
            }
        }
    }
}
