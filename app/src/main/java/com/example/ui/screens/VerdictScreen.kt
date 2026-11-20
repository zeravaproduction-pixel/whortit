package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.CooldownEngine
import com.example.domain.Formatters
import com.example.model.DecisionOutcome
import com.example.model.VerdictType
import com.example.share.ShareCardGenerator
import com.example.ui.components.LabeledInputField
import com.example.ui.components.StatusBadge
import com.example.ui.components.StatusTone
import com.example.ui.components.WorthItCard
import com.example.ui.components.WorthItPrimaryButton
import com.example.ui.components.WorthItSecondaryButton
import com.example.ui.theme.BorderWarmGray
import com.example.ui.theme.DarkAmberText
import com.example.ui.theme.DarkCoralText
import com.example.ui.theme.DarkGreenText
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.PaleSage
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftAmber
import com.example.ui.theme.SoftCoral
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmGray
import com.example.viewmodel.WorthItUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VerdictScreen(
    state: WorthItUiState,
    onAddToVault: (Long, String) -> Boolean,
    onCancelDirectly: () -> Boolean,
    onShowMessage: (String) -> Unit,
    onOpenVault: () -> Unit,
    onDoneToHome: () -> Unit
) {
    BackHandler(onBack = onDoneToHome)

    val context = LocalContext.current
    val evaluation = state.checkDraft.activeEvaluation
    var selectedDuration by remember { mutableLongStateOf(CooldownEngine.DURATION_48H_MS) }
    var vaultUrlInput by remember(state.checkDraft.productUrlInput) {
        mutableStateOf(state.checkDraft.productUrlInput)
    }

    Scaffold(
        containerColor = WarmCream,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hasil Evaluasi",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepCharcoal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDoneToHome) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali ke Dashboard",
                            tint = DeepCharcoal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmCream)
            )
        }
    ) { innerPadding ->
        if (evaluation == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Belum ada evaluasi aktif.", color = WarmGray)
                Spacer(modifier = Modifier.height(12.dp))
                WorthItPrimaryButton(text = "Kembali ke Dashboard", onClick = onDoneToHome)
            }
            return@Scaffold
        }

        val (statusBg, statusFg, statusIcon, statusTone) = when (evaluation.verdict) {
            VerdictType.WORTH_IT -> Quad(SoftGreen, DarkGreenText, Icons.Outlined.CheckCircle, StatusTone.POSITIVE)
            VerdictType.RECONSIDER -> Quad(SoftAmber, DarkAmberText, Icons.Outlined.WarningAmber, StatusTone.WARNING)
            VerdictType.LIKELY_IMPULSIVE -> Quad(SoftCoral, DarkCoralText, Icons.Outlined.ErrorOutline, StatusTone.DANGER)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Verdict Banner + Score Hero
            WorthItCard(
                    containerColor = statusBg,
                    borderColor = statusFg.copy(alpha = 0.25f)
                ) {
                    StatusBadge(
                        text = evaluation.verdict.title,
                        tone = statusTone,
                        icon = statusIcon,
                        modifier = Modifier.testTag("verdict_status_badge")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = evaluation.itemName,
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepCharcoal
                    )
                    Text(
                        text = Formatters.formatRupiah(evaluation.itemPrice),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusFg
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${evaluation.finalScore}",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = statusFg,
                            modifier = Modifier.testTag("verdict_final_score")
                        )
                        Text(
                            text = "/ 100 Final Score",
                            style = MaterialTheme.typography.titleSmall,
                            color = statusFg,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${evaluation.verdict.copywriting}\"",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = DeepCharcoal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Saran tindakan: ${evaluation.verdict.actionHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusFg
                    )
            }

            // Score breakdown
            WorthItCard {
                    Text(
                        text = "Rincian Skor & Tekanan Anggaran",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepCharcoal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScoreMetricTile(
                            label = "Behavioral",
                            value = "${evaluation.behavioralScore}/100",
                            modifier = Modifier.weight(1f)
                        )
                        ScoreMetricTile(
                            label = "Penalty",
                            value = "-${evaluation.financialPenalty}",
                            modifier = Modifier.weight(1f)
                        )
                        ScoreMetricTile(
                            label = "Rasio Dana",
                            value = Formatters.formatPercent(evaluation.priceRatio),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (evaluation.hardRuleTriggered && evaluation.hardRuleReason.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftCoral,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = DarkCoralText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = evaluation.hardRuleReason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkCoralText
                                )
                            }
                        }
            }

            // Reasons
            WorthItCard {
                    Text(
                        text = "Faktor Penentu",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepCharcoal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    evaluation.reasons.forEach { reason ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", color = SageGreen, fontWeight = FontWeight.Bold)
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeepCharcoal
                            )
                        }
                    }
            }

            // Cooldown Vault & Direct Decision
            WorthItCard {
                    if (state.checkDraft.linkedVaultItemId == null) {
                        Text(
                            text = "Beri Jeda di Cooldown Vault",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepCharcoal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Biarkan keinginan ini duduk sebentar. Link produk terkunci hingga timer selesai.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Pilih Durasi Jeda",
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepCharcoal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                CooldownEngine.DURATION_24H_MS to "24 Jam",
                                CooldownEngine.DURATION_48H_MS to "48 Jam (Default)",
                                CooldownEngine.DURATION_72H_MS to "72 Jam"
                            ).forEach { (durationMs, label) ->
                                FilterChip(
                                    selected = selectedDuration == durationMs,
                                    onClick = { selectedDuration = durationMs },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PaleSage,
                                        selectedLabelColor = DeepCharcoal
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LabeledInputField(
                            label = "Link Produk (Opsional)",
                            value = vaultUrlInput,
                            onValueChange = { vaultUrlInput = it },
                            placeholder = "https://contoh.com/produk",
                            keyboardType = KeyboardType.Uri
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        WorthItPrimaryButton(
                            text = "Masukkan ke Cooldown Vault",
                            icon = Icons.Outlined.LockClock,
                            enabled = !state.checkDraft.isSubmittingAction,
                            onClick = { onAddToVault(selectedDuration, vaultUrlInput) },
                            modifier = Modifier.testTag("verdict_add_vault_button")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        WorthItSecondaryButton(
                            text = "Saya Putuskan Batal Beli Sekarang",
                            icon = Icons.Outlined.DoNotDisturbOn,
                            enabled = !state.checkDraft.isSubmittingAction,
                            onClick = { onCancelDirectly() },
                            modifier = Modifier.testTag("verdict_cancel_now_button")
                        )
                    } else {
                        val isCancelled = evaluation.decision == DecisionOutcome.CANCELLED
                        StatusBadge(
                            text = if (isCancelled) "Sudah Dibatalkan & Masuk Savings" else "Sudah Masuk ke Cooldown Vault",
                            tone = StatusTone.POSITIVE
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isCancelled) {
                                "Uangmu aman. Keputusanmu juga."
                            } else {
                                "Biarkan keinginan ini duduk sebentar. Kamu bisa memantau countdown di tab Vault."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepCharcoal
                        )
                        if (!isCancelled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            WorthItSecondaryButton(
                                text = "Buka Cooldown Vault",
                                onClick = onOpenVault
                            )
                        }
                    }
            }

            // Share Card (Copy Text & Download/Share PNG)
            WorthItCard {
                    Text(
                        text = "Share Card (1080×1350 PNG & Teks)",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepCharcoal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bagikan ringkasan refleksi tanpa mengirim data ke server mana pun.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WorthItSecondaryButton(
                            text = "Copy Text",
                            icon = Icons.Outlined.ContentCopy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("verdict_copy_text_button"),
                            onClick = {
                                val text = ShareCardGenerator.buildShareText(evaluation)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("WorthIt Result", text))
                                onShowMessage("Ringkasan evaluasi disalin ke clipboard.")
                            }
                        )
                        WorthItSecondaryButton(
                            text = "Simpan PNG",
                            icon = Icons.Outlined.Download,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("verdict_download_png_button"),
                            onClick = {
                                val bitmap = ShareCardGenerator.renderShareCardBitmap(evaluation)
                                ShareCardGenerator.savePngToPictures(context, bitmap, evaluation.itemName)
                                    .fold(
                                        onSuccess = {
                                            onShowMessage("Share Card PNG (1080×1350) berhasil disimpan ke Pictures/WorthIt.")
                                        },
                                        onFailure = {
                                            onShowMessage("Gagal menyimpan PNG: ${it.message}. Gunakan Copy Text.")
                                        }
                                    )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    WorthItSecondaryButton(
                        text = "Bagikan Gambar Share Card",
                        icon = Icons.Outlined.Share,
                        onClick = {
                            val bitmap = ShareCardGenerator.renderShareCardBitmap(evaluation)
                            ShareCardGenerator.createShareablePngUri(context, bitmap).fold(
                                onSuccess = { uri ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, ShareCardGenerator.buildShareText(evaluation))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Share Card WorthIt?"))
                                },
                                onFailure = {
                                    onShowMessage("Gagal menyiapkan gambar Share Card. Gunakan Copy Text.")
                                }
                            )
                        }
                    )
            }

            // Methodology Disclaimer
            Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PaleSage.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, BorderWarmGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = SageGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Catatan Metodologis: Skor dan vonis adalah alat bantu refleksi berdasarkan data yang kamu masukkan, bukan diagnosis psikometrik atau rekomendasi keuangan profesional.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepCharcoal
                        )
                    }
                }
            }

            WorthItSecondaryButton(
                text = "Kembali ke Dashboard",
                onClick = onDoneToHome
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScoreMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = WarmCream,
        border = BorderStroke(1.dp, BorderWarmGray)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = WarmGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepCharcoal
            )
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
