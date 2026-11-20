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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.Formatters
import com.example.domain.SavingsAndAchievementsEngine
import com.example.model.EvaluationRecord
import com.example.model.ItemStatus
import com.example.model.VerdictType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.StatusTone
import com.example.ui.components.WorthItCard
import com.example.ui.theme.BorderWarmGray
import com.example.ui.theme.DarkGreenText
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.PaleSage
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftApricot
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmGray
import com.example.viewmodel.WorthItUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavingsScreen(
    state: WorthItUiState,
    onStartCheckItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Savings & Badges, 1 = Decision Journal
    val summary = state.savingsSummary
    val cancelledItems = state.appData.items.filter { it.status == ItemStatus.CANCELLED }
    val unlockedMap = state.appData.achievements.associateBy { it.id }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Savings & Riwayat",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepCharcoal
                )
                Text(
                    text = "Sedikit demi sedikit, uangmu kembali ke kendalimu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray
                )
            }
        }

        // Hero Savings Card
        item {
            WorthItCard(
                containerColor = SoftGreen.copy(alpha = 0.6f),
                borderColor = PaleSage
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
                        text = "Total Nilai Pembelian Dibatalkan",
                        style = MaterialTheme.typography.titleSmall,
                        color = DarkGreenText
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = Formatters.formatRupiah(summary.totalSaved),
                    style = MaterialTheme.typography.displaySmall,
                    color = SageGreen,
                    modifier = Modifier.testTag("savings_total_amount")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftWhite.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = WarmGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nominal ini merupakan nilai barang yang ditandai batal beli olehmu (hanya status cancelled), bukan uang yang diverifikasi masuk ke rekening tabungan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray
                        )
                    }
                }
            }
        }

        // Section switcher
        item {
            TabRow(
                selectedTabIndex = selectedSection,
                containerColor = SoftWhite,
                contentColor = SageGreen,
                indicator = { tabPositions ->
                    if (selectedSection < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSection]),
                            color = SageGreen
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = { Text("Savings & Lencana") },
                    modifier = Modifier.testTag("savings_tab_overview")
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = { Text("Jurnal (${state.appData.evaluations.size})") },
                    modifier = Modifier.testTag("savings_tab_journal")
                )
            }
        }

        if (selectedSection == 0) {
            // Stats grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        label = "Pembatalan",
                        value = "${summary.cancelledCount} item",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Rata-rata",
                        value = Formatters.formatRupiah(summary.averageSaved),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (summary.cancellationRatePercent != null) {
                item {
                    WorthItCard {
                        Text(
                            text = "Rasio Pembatalan Keputusan Vault",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${Formatters.formatDecimal(summary.cancellationRatePercent, 1)}% barang yang selesai dijeda akhirnya dibatalkan.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DeepCharcoal
                        )
                    }
                }
            }

            // Equivalency conversion
            item {
                WorthItCard {
                    Text(
                        text = "Konversi Kebutuhan",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepCharcoal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EquivalentChip(
                            icon = Icons.Outlined.Fastfood,
                            text = "${Formatters.formatDecimal(summary.lunchPortions ?: 0.0, 1)} porsi makan"
                        )
                        EquivalentChip(
                            icon = Icons.Outlined.Coffee,
                            text = "${Formatters.formatDecimal(summary.coffeeCups ?: 0.0, 1)} gelas kopi"
                        )
                        EquivalentChip(
                            icon = Icons.Outlined.Wifi,
                            text = "${Formatters.formatDecimal(summary.internetPackages ?: 0.0, 1)} paket kuota"
                        )
                    }
                }
            }

            // Achievements (PRD 4.7 & p.11)
            item {
                Text(
                    text = "Apresiasi Perjalananmu (${unlockedMap.size}/${SavingsAndAchievementsEngine.ALL_ACHIEVEMENTS.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepCharcoal
                )
            }

            items(SavingsAndAchievementsEngine.ALL_ACHIEVEMENTS, key = { it.id }) { def ->
                val record = unlockedMap[def.id]
                val isUnlocked = record != null
                WorthItCard(
                    containerColor = if (isUnlocked) SoftWhite else WarmCream.copy(alpha = 0.7f),
                    borderColor = if (isUnlocked) SageGreen.copy(alpha = 0.4f) else BorderWarmGray
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUnlocked) PaleSage else BorderWarmGray,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Outlined.AutoAwesome else Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (isUnlocked) SageGreen else WarmGray,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = def.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isUnlocked) DeepCharcoal else WarmGray
                            )
                            Text(
                                text = def.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmGray
                            )
                            if (record != null) {
                                Text(
                                    text = "Tercapai pada ${Formatters.formatDateTime(record.unlockedAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SageGreen
                                )
                            }
                        }
                    }
                }
            }

            // Cancelled history
            item {
                Text(
                    text = "Riwayat Barang Dibatalkan (${cancelledItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepCharcoal
                )
            }

            if (cancelledItems.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Belum ada pembelian yang dibatalkan",
                        description = "Saat kamu memutuskan Batal Beli dari Vault atau setelah evaluasi, nilainya akan tercatat di sini.",
                        ctaLabel = "Cek Barang Pertama",
                        onCtaClick = onStartCheckItem
                    )
                }
            } else {
                items(cancelledItems, key = { it.id }) { item ->
                    WorthItCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepCharcoal
                                )
                                Text(
                                    text = Formatters.formatDateTime(item.lastSeenAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmGray
                                )
                            }
                            Text(
                                text = Formatters.formatRupiah(item.price),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SageGreen
                            )
                        }
                    }
                }
            }
        } else {
            // Pattern Insights (PRD 5.2: gated until sample size >= 5)
            item {
                val insights = state.patternInsights
                WorthItCard(
                    containerColor = SoftApricot.copy(alpha = 0.45f),
                    borderColor = BorderWarmGray
                ) {
                    Text(
                        text = "Insight Pola Belanja",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepCharcoal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (!insights.hasEnoughData) {
                        Text(
                            text = "Data saat ini: ${insights.totalEvaluations}/${insights.minimumRequired} evaluasi. Selesaikan minimal ${insights.minimumRequired} evaluasi agar WorthIt? dapat membaca pola tanpa menebak dari sampel yang terlalu kecil.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepCharcoal
                        )
                    } else {
                        Text(
                            text = "Rata-rata skor akhir: ${Formatters.formatDecimal(insights.averageFinalScore, 1)}/100",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DeepCharcoal
                        )
                        Text(
                            text = "Proporsi impulsif: ${Formatters.formatDecimal(insights.impulsiveRatePercent, 0)}% dari ${insights.totalEvaluations} evaluasi",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray
                        )
                        if (insights.mostCommonWeakQuestion != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pola utama: ${insights.mostCommonWeakQuestion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SageGreen
                            )
                        }
                    }
                }
            }

            if (state.appData.evaluations.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Jurnal keputusan masih kosong",
                        description = "Setiap evaluasi yang kamu selesaikan akan tersimpan lengkap beserta snapshot kondisi finansial saat itu.",
                        ctaLabel = "Mulai Evaluasi",
                        onCtaClick = onStartCheckItem
                    )
                }
            } else {
                items(state.appData.evaluations, key = { it.id }) { eval ->
                    DecisionJournalCard(eval = eval)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    WorthItCard(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = WarmGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = DeepCharcoal
        )
    }
}

@Composable
private fun EquivalentChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = PaleSage
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SageGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = DeepCharcoal
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecisionJournalCard(eval: EvaluationRecord) {
    val tone = when (eval.verdict) {
        VerdictType.WORTH_IT -> StatusTone.POSITIVE
        VerdictType.RECONSIDER -> StatusTone.WARNING
        VerdictType.LIKELY_IMPULSIVE -> StatusTone.DANGER
    }
    WorthItCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eval.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    color = DeepCharcoal
                )
                Text(
                    text = Formatters.formatRupiah(eval.itemPrice),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SageGreen
                )
            }
            StatusBadge(text = "${eval.finalScore}/100", tone = tone)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${eval.verdict.title} • ${eval.decision.label}",
            style = MaterialTheme.typography.labelMedium,
            color = DeepCharcoal
        )
        Text(
            text = Formatters.formatDateTime(eval.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            eval.answers.forEachIndexed { idx, ans ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (ans) SoftGreen else WarmCream,
                    border = BorderStroke(1.dp, BorderWarmGray)
                ) {
                    Text(
                        text = "Q${idx + 1}: ${if (ans) "Ya" else "Tidak"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepCharcoal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Behavioral ${eval.behavioralScore} • Penalty -${eval.financialPenalty} • Rasio ${Formatters.formatPercent(eval.priceRatio)}",
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray
        )
        Text(
            text = "Snapshot dana bebas saat evaluasi: ${Formatters.formatRupiah(eval.financialSnapshot.freeFunds.coerceAtLeast(0.0))}/bln",
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray
        )
    }
}
