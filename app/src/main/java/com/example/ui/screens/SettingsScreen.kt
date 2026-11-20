package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.UploadFile
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.FinancialCalculator
import com.example.domain.Formatters
import com.example.model.IncomePeriod
import com.example.model.IncomeType
import com.example.ui.components.LabeledInputField
import com.example.ui.components.RupiahPreviewText
import com.example.ui.components.WorthItCard
import com.example.ui.components.WorthItPrimaryButton
import com.example.ui.components.WorthItSecondaryButton
import com.example.ui.theme.BorderWarmGray
import com.example.ui.theme.DarkCoralText
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.PaleSage
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftCoral
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmGray
import com.example.viewmodel.WorthItUiState
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: WorthItUiState,
    onSaveSettings: (IncomeType, IncomePeriod, String, String, String, String, String, String, String) -> Result<Unit>,
    onBuildExportJson: () -> String,
    onPrepareImportJson: (String) -> Result<*>,
    onDismissImportPreview: () -> Unit,
    onConfirmImportReplace: () -> Unit,
    onConfirmImportMerge: () -> Unit,
    onRestorePreReplaceBackup: () -> Unit,
    onDeleteAllData: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings = state.appData.settings

    var incomeType by remember(settings.incomeType) { mutableStateOf(settings.incomeType) }
    var incomePeriod by remember(settings.incomePeriod) { mutableStateOf(settings.incomePeriod) }
    var incomeDigits by remember(settings.incomeAmount) {
        mutableStateOf(if (settings.incomeAmount > 0L) settings.incomeAmount.toString() else "")
    }
    var expensesDigits by remember(settings.mandatoryExpenses) {
        mutableStateOf(if (settings.mandatoryExpenses > 0L) settings.mandatoryExpenses.toString() else "0")
    }
    var lunchDigits by remember(settings.lunchPrice) { mutableStateOf(settings.lunchPrice.toString()) }
    var coffeeDigits by remember(settings.coffeePrice) { mutableStateOf(settings.coffeePrice.toString()) }
    var internetDigits by remember(settings.internetPrice) { mutableStateOf(settings.internetPrice.toString()) }
    var customDailyDigits by remember(settings.customDailyAllowance) {
        mutableStateOf(settings.customDailyAllowance?.toString() ?: "")
    }
    var customHourlyDigits by remember(settings.customHourlyWage) {
        mutableStateOf(settings.customHourlyWage?.toString() ?: "")
    }
    var settingsError by remember { mutableStateOf<String?>(null) }

    var showReplaceFinalConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = onBuildExportJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                onShowMessage("Backup JSON berhasil diekspor.")
            } catch (e: Exception) {
                onShowMessage("Gagal mengekspor JSON: ${e.message}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val raw = readTextFromUri(context, uri)
            if (raw == null) {
                onShowMessage("Gagal membaca file backup yang dipilih.")
            } else {
                onPrepareImportJson(raw)
            }
        }
    }

    // Live normalized monthly preview
    val previewMonthlyIncome = FinancialCalculator.normalizeToMonthly(
        (incomeDigits.toLongOrNull() ?: 0L).toDouble(),
        incomePeriod
    )
    val previewMonthlyExpenses = FinancialCalculator.normalizeToMonthly(
        (expensesDigits.toLongOrNull() ?: 0L).toDouble(),
        incomePeriod
    )
    val previewFreeFunds = previewMonthlyIncome - previewMonthlyExpenses

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepCharcoal
                )
                Text(
                    text = "Profil finansial, harga pembanding, dan perlindungan data lokal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray
                )
            }
        }

        // Financial Profile Section
        item {
            WorthItCard {
                Text(
                    text = "Profil Finansial Pribadi",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepCharcoal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Masukkan pemasukan dan pengeluaran wajib pada periode yang sama. Semua nilai dinormalisasi otomatis ke basis bulanan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Jenis Pemasukan", style = MaterialTheme.typography.labelLarge, color = DeepCharcoal)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IncomeType.entries.forEach { type ->
                        FilterChip(
                            selected = incomeType == type,
                            onClick = { incomeType = type },
                            label = { Text(type.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaleSage,
                                selectedLabelColor = DeepCharcoal
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Periode Pemasukan & Pengeluaran Wajib", style = MaterialTheme.typography.labelLarge, color = DeepCharcoal)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IncomePeriod.entries.forEach { period ->
                        FilterChip(
                            selected = incomePeriod == period,
                            onClick = { incomePeriod = period },
                            label = { Text(period.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaleSage,
                                selectedLabelColor = DeepCharcoal
                            ),
                            modifier = Modifier.testTag("settings_period_${period.jsonKey}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LabeledInputField(
                    label = "Jumlah Pemasukan (${incomePeriod.label})",
                    value = incomeDigits,
                    onValueChange = {
                        incomeDigits = it.filter { c -> c.isDigit() }.take(13)
                        settingsError = null
                    },
                    placeholder = "1200000",
                    prefixText = "Rp ",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier,
                    fieldModifier = Modifier.testTag("settings_income_input")
                )
                RupiahPreviewText(digits = incomeDigits)

                Spacer(modifier = Modifier.height(12.dp))
                LabeledInputField(
                    label = "Pengeluaran Wajib (${incomePeriod.label})",
                    value = expensesDigits,
                    onValueChange = {
                        expensesDigits = it.filter { c -> c.isDigit() }.take(13)
                        settingsError = null
                    },
                    placeholder = "300000",
                    prefixText = "Rp ",
                    helperText = "Kost, makan pokok, cicilan wajib, transportasi rutin.",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier,
                    fieldModifier = Modifier.testTag("settings_expenses_input")
                )
                RupiahPreviewText(digits = expensesDigits)

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarmCream,
                    border = BorderStroke(1.dp, BorderWarmGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Setara Bulanan",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmGray
                        )
                        Text(
                            text = "Pemasukan: ${Formatters.formatRupiah(previewMonthlyIncome)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepCharcoal
                        )
                        Text(
                            text = "Pengeluaran wajib: ${Formatters.formatRupiah(previewMonthlyExpenses)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepCharcoal
                        )
                        Text(
                            text = "Dana Bebas (D): ${
                                if (previewFreeFunds <= 0.0) "Rp0 (pengeluaran ≥ pemasukan)"
                                else Formatters.formatRupiah(previewFreeFunds)
                            }",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (previewFreeFunds <= 0.0) DarkCoralText else SageGreen
                        )
                    }
                }
            }
        }

        // Comparator Prices
        item {
            WorthItCard {
                Text(
                    text = "Harga Pembanding Reality Check",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepCharcoal
                )
                Spacer(modifier = Modifier.height(12.dp))
                LabeledInputField(
                    label = "Makan Siang",
                    value = lunchDigits,
                    onValueChange = { lunchDigits = it.filter { c -> c.isDigit() }.take(10) },
                    placeholder = "15000",
                    prefixText = "Rp ",
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(10.dp))
                LabeledInputField(
                    label = "Kopi",
                    value = coffeeDigits,
                    onValueChange = { coffeeDigits = it.filter { c -> c.isDigit() }.take(10) },
                    placeholder = "20000",
                    prefixText = "Rp ",
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(10.dp))
                LabeledInputField(
                    label = "Paket Kuota Internet",
                    value = internetDigits,
                    onValueChange = { internetDigits = it.filter { c -> c.isDigit() }.take(10) },
                    placeholder = "50000",
                    prefixText = "Rp ",
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(10.dp))
                LabeledInputField(
                    label = "Uang Saku Harian Kustom (Opsional)",
                    value = customDailyDigits,
                    onValueChange = { customDailyDigits = it.filter { c -> c.isDigit() }.take(10) },
                    placeholder = "Kosong = pemasukan bulanan / 30",
                    prefixText = "Rp ",
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(10.dp))
                LabeledInputField(
                    label = "Upah per Jam Kustom (Opsional)",
                    value = customHourlyDigits,
                    onValueChange = { customHourlyDigits = it.filter { c -> c.isDigit() }.take(10) },
                    placeholder = "Kosong = pemasukan bulanan / 160 jam",
                    prefixText = "Rp ",
                    keyboardType = KeyboardType.Number
                )

                if (settingsError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = settingsError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkCoralText
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                WorthItPrimaryButton(
                    text = "Simpan Pengaturan",
                    modifier = Modifier.testTag("settings_save_button"),
                    onClick = {
                        onSaveSettings(
                            incomeType,
                            incomePeriod,
                            incomeDigits,
                            expensesDigits.ifBlank { "0" },
                            lunchDigits,
                            coffeeDigits,
                            internetDigits,
                            customDailyDigits,
                            customHourlyDigits
                        ).fold(
                            onSuccess = { settingsError = null },
                            onFailure = { settingsError = it.message }
                        )
                    }
                )
            }
        }

        // Export / Import JSON & Delete Data
        item {
            WorthItCard {
                Text(
                    text = "Backup & Transfer Data (JSON)",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepCharcoal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Semua data disimpan secara lokal di perangkat ini tanpa akun atau server. Menghapus data aplikasi atau cache situs dapat menghapus riwayatmu—ekspor JSON secara berkala.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray
                )
                state.appData.metadata.lastBackupAt?.let { lastBackup ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Backup terakhir: ${Formatters.formatDateTime(lastBackup)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SageGreen
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorthItSecondaryButton(
                        text = "Ekspor JSON",
                        icon = Icons.Outlined.Download,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_export_json_button"),
                        onClick = {
                            val fileName = "worthit-backup-${Formatters.formatIsoDate(System.currentTimeMillis())}.json"
                            exportLauncher.launch(fileName)
                        }
                    )
                    WorthItSecondaryButton(
                        text = "Impor JSON",
                        icon = Icons.Outlined.UploadFile,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_import_json_button"),
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    )
                }

                if (state.hasPreReplaceBackup) {
                    Spacer(modifier = Modifier.height(10.dp))
                    WorthItSecondaryButton(
                        text = "Pulihkan Backup Sebelum Replace",
                        icon = Icons.Outlined.Restore,
                        onClick = onRestorePreReplaceBackup
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showDeleteAllConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_delete_all_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DarkCoralText.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkCoralText)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Semua Data Lokal")
                }
            }
        }

        // Disclaimer & App Info (PRD Section 16)
        item {
            WorthItCard(containerColor = WarmCream) {
                Text(
                    text = "Tentang & Product Disclaimer",
                    style = MaterialTheme.typography.titleSmall,
                    color = DeepCharcoal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "WorthIt? (v${state.appData.appVersion}) adalah alat bantu refleksi keputusan belanja, bukan penasihat keuangan profesional. Skor tidak menentukan secara mutlak apakah barang harus dibeli. Aplikasi tidak memverifikasi saldo bank, tidak mengakses marketplace, tidak memblokir checkout, dan tidak menjamin timer anti-manipulasi tanpa sumber waktu tepercaya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Import Preview Dialog (Choose Merge or Replace)
    val preview = state.importPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = onDismissImportPreview,
            containerColor = SoftWhite,
            title = {
                Text("Pratinjau Impor JSON", color = DeepCharcoal)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "File backup valid ditemukan:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepCharcoal
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WarmCream,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("• Item Vault: ${preview.itemsCount}", style = MaterialTheme.typography.bodySmall, color = DeepCharcoal)
                            Text("• Evaluasi: ${preview.evaluationsCount}", style = MaterialTheme.typography.bodySmall, color = DeepCharcoal)
                            Text("• Lencana: ${preview.achievementsCount}", style = MaterialTheme.typography.bodySmall, color = DeepCharcoal)
                        }
                    }
                    Text(
                        text = "Pilih Merge untuk menggabungkan tanpa duplikasi ID, atau Replace untuk mengganti seluruh data aktif.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showReplaceFinalConfirm = true },
                        border = BorderStroke(1.dp, DarkCoralText)
                    ) {
                        Text("Replace", color = DarkCoralText)
                    }
                    Button(
                        onClick = onConfirmImportMerge,
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreen, contentColor = SoftWhite)
                    ) {
                        Text("Merge")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissImportPreview) {
                    Text("Batal", color = WarmGray)
                }
            }
        )
    }

    if (showReplaceFinalConfirm) {
        AlertDialog(
            onDismissRequest = { showReplaceFinalConfirm = false },
            containerColor = SoftWhite,
            title = { Text("Konfirmasi Replace Data", color = DarkCoralText) },
            text = {
                Text(
                    text = "Seluruh data aktif akan diganti dengan isi file backup ini. WorthIt? akan otomatis menyimpan backup data lama sebelum Replace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepCharcoal
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReplaceFinalConfirm = false
                        onConfirmImportReplace()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCoralText, contentColor = SoftWhite)
                ) {
                    Text("Ya, Replace Semua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceFinalConfirm = false }) {
                    Text("Batal", color = WarmGray)
                }
            }
        )
    }

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            containerColor = SoftWhite,
            title = { Text("Hapus Semua Data?", color = DarkCoralText) },
            text = {
                Text(
                    text = "Tindakan ini akan menghapus seluruh riwayat evaluasi, Cooldown Vault, Savings, dan pengaturan lokalmu di perangkat ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepCharcoal
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllConfirm = false
                        onDeleteAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCoralText, contentColor = SoftWhite)
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text("Batal", color = WarmGray)
                }
            }
        )
    }
}

private fun readTextFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        }
    } catch (_: Exception) {
        null
    }
}
