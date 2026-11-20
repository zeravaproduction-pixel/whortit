package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.StatusBadge
import com.example.ui.components.StatusTone
import com.example.ui.components.WorthItCard
import com.example.ui.components.WorthItPrimaryButton
import com.example.ui.components.WorthItSecondaryButton
import com.example.ui.theme.DarkAmberText
import com.example.ui.theme.DarkCoralText
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.PaleSage
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftAmber
import com.example.ui.theme.SoftCoral
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmGray
import com.example.viewmodel.WorthItUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckItemScreen(
    state: WorthItUiState,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSaveQuickProfile: (IncomeType, IncomePeriod, String, String, String, String, String, String, String) -> Result<Unit>,
    onUpdateComparators: (String, String, String, String, String) -> Result<Unit>,
    onProceedToFilter: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val draft = state.checkDraft
    val reality = state.liveRealityCheck

    var showComparatorEditor by remember { mutableStateOf(false) }
    var lunchInput by remember(state.appData.settings.lunchPrice) {
        mutableStateOf(state.appData.settings.lunchPrice.toString())
    }
    var coffeeInput by remember(state.appData.settings.coffeePrice) {
        mutableStateOf(state.appData.settings.coffeePrice.toString())
    }
    var internetInput by remember(state.appData.settings.internetPrice) {
        mutableStateOf(state.appData.settings.internetPrice.toString())
    }
    var customDailyInput by remember(state.appData.settings.customDailyAllowance) {
        mutableStateOf(state.appData.settings.customDailyAllowance?.toString() ?: "")
    }
    var customHourlyInput by remember(state.appData.settings.customHourlyWage) {
        mutableStateOf(state.appData.settings.customHourlyWage?.toString() ?: "")
    }
    var comparatorError by remember { mutableStateOf<String?>(null) }

    var showProfileEditor by remember(state.isFinancialProfileConfigured) {
        mutableStateOf(!state.isFinancialProfileConfigured)
    }
    var selectedType by remember(state.appData.settings.incomeType) {
        mutableStateOf(state.appData.settings.incomeType)
    }
    var selectedPeriod by remember(state.appData.settings.incomePeriod) {
        mutableStateOf(state.appData.settings.incomePeriod)
    }
    var incomeDigits by remember(state.appData.settings.incomeAmount) {
        mutableStateOf(if (state.appData.settings.incomeAmount > 0L) state.appData.settings.incomeAmount.toString() else "")
    }
    var expensesDigits by remember(state.appData.settings.mandatoryExpenses) {
        mutableStateOf(if (state.appData.settings.mandatoryExpenses > 0L) state.appData.settings.mandatoryExpenses.toString() else "")
    }
    var quickProfileError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = WarmCream,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cek Barang",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepCharcoal
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("check_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali",
                            tint = DeepCharcoal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmCream)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Input Card
            WorthItCard {
                Text(
                    text = "Apa yang lagi bikin kamu pengen belanja?",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepCharcoal
                )
                Spacer(modifier = Modifier.height(14.dp))
                LabeledInputField(
                    label = "Nama Barang (Opsional)",
                    value = draft.itemNameInput,
                    onValueChange = onNameChange,
                    placeholder = "Contoh: Wireless Earbuds",
                    helperText = "${draft.itemNameInput.length}/80 karakter • Kosongkan jika ingin dicatat sebagai \"Barang tanpa nama\"",
                    fieldModifier = Modifier.testTag("check_item_name_input")
                )
                Spacer(modifier = Modifier.height(14.dp))
                LabeledInputField(
                    label = "Berapa yang harus kamu keluarkan?",
                    value = draft.priceDigitsInput,
                    onValueChange = onPriceChange,
                    placeholder = "250000",
                    prefixText = "Rp ",
                    keyboardType = KeyboardType.Number,
                    errorText = draft.priceError,
                    fieldModifier = Modifier.testTag("check_item_price_input")
                )
                Spacer(modifier = Modifier.height(4.dp))
                RupiahPreviewText(digits = draft.priceDigitsInput)
                Spacer(modifier = Modifier.height(14.dp))
                LabeledInputField(
                    label = "Link Produk (Opsional)",
                    value = draft.productUrlInput,
                    onValueChange = onUrlChange,
                    placeholder = "https://contoh.com/produk",
                    helperText = "Hanya menerima link http:// atau https://",
                    errorText = draft.urlError,
                    keyboardType = KeyboardType.Uri,
                    fieldModifier = Modifier.testTag("check_item_url_input")
                )
            }

            // Reality Check Converter Card
            WorthItCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reality Check Converter",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepCharcoal
                            )
                            Text(
                                text = "Konversi harga ke waktu dan kebutuhan harian",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmGray
                            )
                        }
                        TextButton(
                            onClick = { showComparatorEditor = !showComparatorEditor },
                            modifier = Modifier.testTag("check_toggle_comparators")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = SageGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showComparatorEditor) "Tutup" else "Ubah", color = SageGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (reality == null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WarmCream,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Masukkan harga barang di atas untuk melihat setara berapa hari uang saku, jam kerja, porsi makan, kopi, dan kuota internet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmGray,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        Text(
                            text = Formatters.formatRupiah(reality.price),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = SageGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RealityRow(
                                icon = Icons.Outlined.Today,
                                label = "Hari uang saku",
                                value = reality.allowanceDays?.let {
                                    "${Formatters.formatDecimal(it, 1)} hari"
                                } ?: "Atur pemasukan / uang saku harian"
                            )
                            RealityRow(
                                icon = Icons.Outlined.Schedule,
                                label = "Jam kerja",
                                value = reality.workHours?.let {
                                    "${Formatters.formatDecimal(it, 1)} jam"
                                } ?: "Atur pemasukan / upah per jam"
                            )
                            RealityRow(
                                icon = Icons.Outlined.Fastfood,
                                label = "Makan siang (${Formatters.formatRupiah(state.appData.settings.lunchPrice)})",
                                value = reality.lunchPortions?.let {
                                    "${Formatters.formatDecimal(it, 1)} porsi"
                                } ?: "-"
                            )
                            RealityRow(
                                icon = Icons.Outlined.Coffee,
                                label = "Kopi (${Formatters.formatRupiah(state.appData.settings.coffeePrice)})",
                                value = reality.coffeeCups?.let {
                                    "${Formatters.formatDecimal(it, 1)} gelas"
                                } ?: "-"
                            )
                            RealityRow(
                                icon = Icons.Outlined.Wifi,
                                label = "Kuota internet (${Formatters.formatRupiah(state.appData.settings.internetPrice)})",
                                value = reality.internetPackages?.let {
                                    "${Formatters.formatDecimal(it, 1)} paket"
                                } ?: "-"
                            )
                        }
                    }

                    AnimatedVisibility(visible = showComparatorEditor) {
                        Column(
                            modifier = Modifier.padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LabeledInputField(
                                label = "Harga Makan Siang (Rp)",
                                value = lunchInput,
                                onValueChange = { lunchInput = it.filter { c -> c.isDigit() } },
                                placeholder = "15000",
                                keyboardType = KeyboardType.Number
                            )
                            LabeledInputField(
                                label = "Harga Kopi (Rp)",
                                value = coffeeInput,
                                onValueChange = { coffeeInput = it.filter { c -> c.isDigit() } },
                                placeholder = "20000",
                                keyboardType = KeyboardType.Number
                            )
                            LabeledInputField(
                                label = "Harga Kuota Internet (Rp)",
                                value = internetInput,
                                onValueChange = { internetInput = it.filter { c -> c.isDigit() } },
                                placeholder = "50000",
                                keyboardType = KeyboardType.Number
                            )
                            LabeledInputField(
                                label = "Uang Saku Harian Kustom (Opsional)",
                                value = customDailyInput,
                                onValueChange = { customDailyInput = it.filter { c -> c.isDigit() } },
                                placeholder = "Otomatis dari pemasukan / 30",
                                keyboardType = KeyboardType.Number
                            )
                            LabeledInputField(
                                label = "Upah per Jam Kustom (Opsional)",
                                value = customHourlyInput,
                                onValueChange = { customHourlyInput = it.filter { c -> c.isDigit() } },
                                placeholder = "Otomatis dari pemasukan / 160 jam",
                                keyboardType = KeyboardType.Number
                            )
                            if (comparatorError != null) {
                                Text(
                                    text = comparatorError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkCoralText
                                )
                            }
                            WorthItSecondaryButton(
                                text = "Terapkan Pembanding",
                                onClick = {
                                    val res = onUpdateComparators(
                                        lunchInput,
                                        coffeeInput,
                                        internetInput,
                                        customDailyInput,
                                        customHourlyInput
                                    )
                                    res.fold(
                                        onSuccess = {
                                            comparatorError = null
                                            showComparatorEditor = false
                                        },
                                        onFailure = { comparatorError = it.message }
                                    )
                                }
                            )
                        }
                    }
                }

            // Financial Profile Confirmation Card
            WorthItCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profil Finansial",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepCharcoal
                        )
                        if (state.isFinancialProfileConfigured) {
                            TextButton(onClick = { showProfileEditor = !showProfileEditor }) {
                                Text(if (showProfileEditor) "Selesai" else "Ubah", color = SageGreen)
                            }
                        }
                    }

                    if (!state.isFinancialProfileConfigured) {
                        StatusBadge(
                            text = "Perlu diisi sebelum lanjut",
                            tone = StatusTone.WARNING
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Isi pemasukan dan pengeluaran wajib agar WorthIt? dapat menilai dampak harga terhadap dana bebasmu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray
                        )
                    } else {
                        val snap = state.financialSnapshot
                        val price = draft.parsedPriceOrNull
                        val ratio = if (price != null) {
                            FinancialCalculator.calculatePriceRatio(price, snap.freeFunds)
                        } else null

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pemasukan bulanan: ${Formatters.formatRupiah(snap.monthlyIncome)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepCharcoal
                        )
                        Text(
                            text = "Pengeluaran wajib: ${Formatters.formatRupiah(snap.monthlyExpenses)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmGray
                        )
                        Text(
                            text = "Dana bebas (D): ${
                                if (snap.freeFunds <= 0.0) "Rp0 (pengeluaran ≥ pemasukan)"
                                else Formatters.formatRupiah(snap.freeFunds)
                            }",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (snap.freeFunds <= 0.0) DarkCoralText else SageGreen
                        )

                        if (ratio != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            when {
                                snap.freeFunds <= 0.0 -> {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SoftCoral,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Dana bebasmu nol atau negatif. Vonis hijau akan diblokir secara otomatis.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DarkCoralText,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                                ratio > 1.0 -> {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SoftCoral,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Peringatan: Harga barang melebihi 100% dana bebas bulananmu (${Formatters.formatPercent(ratio)}).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DarkCoralText,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                                ratio > 0.75 -> {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SoftAmber,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Harga barang memakan ${Formatters.formatPercent(ratio)} dari dana bebas (>75%). Vonis hijau tidak diizinkan.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DarkAmberText,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "Rasio terhadap dana bebas: ${Formatters.formatPercent(ratio)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SageGreen
                                    )
                                }
                            }
                        }
                    }

                    if (draft.profileError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = draft.profileError,
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkCoralText
                        )
                    }

                    AnimatedVisibility(visible = showProfileEditor) {
                        Column(
                            modifier = Modifier.padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Jenis Pemasukan",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepCharcoal
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IncomeType.entries.forEach { type ->
                                    FilterChip(
                                        selected = selectedType == type,
                                        onClick = { selectedType = type },
                                        label = { Text(type.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PaleSage,
                                            selectedLabelColor = DeepCharcoal
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "Periode Input",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepCharcoal
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IncomePeriod.entries.forEach { period ->
                                    FilterChip(
                                        selected = selectedPeriod == period,
                                        onClick = { selectedPeriod = period },
                                        label = { Text(period.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PaleSage,
                                            selectedLabelColor = DeepCharcoal
                                        )
                                    )
                                }
                            }
                            LabeledInputField(
                                label = "Pemasukan (${selectedPeriod.label})",
                                value = incomeDigits,
                                onValueChange = { incomeDigits = it.filter { c -> c.isDigit() } },
                                placeholder = "1200000",
                                prefixText = "Rp ",
                                keyboardType = KeyboardType.Number,
                                fieldModifier = Modifier.testTag("quick_profile_income_input")
                            )
                            LabeledInputField(
                                label = "Pengeluaran Wajib (${selectedPeriod.label})",
                                value = expensesDigits,
                                onValueChange = { expensesDigits = it.filter { c -> c.isDigit() } },
                                placeholder = "300000",
                                prefixText = "Rp ",
                                keyboardType = KeyboardType.Number,
                                fieldModifier = Modifier.testTag("quick_profile_expenses_input")
                            )
                            if (quickProfileError != null) {
                                Text(
                                    text = quickProfileError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkCoralText
                                )
                            }
                            WorthItSecondaryButton(
                                text = "Simpan Profil Finansial",
                                modifier = Modifier.testTag("quick_profile_save_button"),
                                onClick = {
                                    val res = onSaveQuickProfile(
                                        selectedType,
                                        selectedPeriod,
                                        incomeDigits,
                                        expensesDigits.ifBlank { "0" },
                                        state.appData.settings.lunchPrice.toString(),
                                        state.appData.settings.coffeePrice.toString(),
                                        state.appData.settings.internetPrice.toString(),
                                        state.appData.settings.customDailyAllowance?.toString() ?: "",
                                        state.appData.settings.customHourlyWage?.toString() ?: ""
                                    )
                                    res.fold(
                                        onSuccess = {
                                            quickProfileError = null
                                            showProfileEditor = false
                                        },
                                        onFailure = { quickProfileError = it.message }
                                    )
                                }
                            )
                        }
                    }
                }

            WorthItPrimaryButton(
                text = "Lanjut ke Impulse Filter (5 Pertanyaan)",
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                onClick = onProceedToFilter,
                modifier = Modifier.testTag("check_proceed_filter_button")
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RealityRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WarmCream,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SageGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepCharcoal
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = SageGreen
            )
        }
    }
}
