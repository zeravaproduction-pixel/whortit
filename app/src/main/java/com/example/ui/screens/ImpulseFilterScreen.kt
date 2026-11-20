package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.BehavioralEngine
import com.example.domain.Formatters
import com.example.ui.components.WorthItCard
import com.example.ui.components.WorthItPrimaryButton
import com.example.ui.theme.BorderWarmGray
import com.example.ui.theme.DarkAmberText
import com.example.ui.theme.DarkGreenText
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.PaleSage
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftAmber
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmGray
import com.example.viewmodel.WorthItUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpulseFilterScreen(
    state: WorthItUiState,
    onAnswerQuestion: (Int, Boolean) -> Unit,
    onGoToQuestion: (Int) -> Unit,
    onSubmitFilter: () -> Unit,
    onBackToCheckItem: () -> Unit
) {
    val draft = state.checkDraft
    val qIndex = draft.currentQuestionIndex.coerceIn(0, BehavioralEngine.QUESTIONS.lastIndex)
    val question = BehavioralEngine.QUESTIONS[qIndex]
    val selectedAnswer = draft.answers.getOrNull(qIndex)
    val allAnswered = draft.answers.all { it != null }

    fun handleBack() {
        if (qIndex > 0) {
            onGoToQuestion(qIndex - 1)
        } else {
            onBackToCheckItem()
        }
    }

    BackHandler(onBack = ::handleBack)

    Scaffold(
        containerColor = WarmCream,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "60-Second Impulse Filter",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepCharcoal
                        )
                        Text(
                            text = "${Formatters.sanitizeItemName(draft.itemNameInput)} • ${
                                draft.parsedPriceOrNull?.let { Formatters.formatRupiah(it) } ?: ""
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = ::handleBack,
                        modifier = Modifier.testTag("filter_back_button")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Progress & non-blocking 60s framing indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pertanyaan ${qIndex + 1}/5",
                    style = MaterialTheme.typography.labelLarge,
                    color = SageGreen,
                    modifier = Modifier.testTag("filter_progress_label")
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PaleSage
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = SageGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (draft.framingRemainingSeconds > 0) {
                                "Jeda ${draft.framingRemainingSeconds}d"
                            } else {
                                "Santai saja, tanpa batas paksa"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepCharcoal
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (qIndex + 1) / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = SageGreen,
                trackColor = PaleSage
            )

            // Question Step Dots (allows jumping back/editing answered questions before calculation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BehavioralEngine.QUESTIONS.forEachIndexed { idx, _ ->
                    val ans = draft.answers.getOrNull(idx)
                    val isCurrent = idx == qIndex
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable { onGoToQuestion(idx) },
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isCurrent -> SageGreen
                            ans != null -> PaleSage
                            else -> SoftWhite
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) SageGreen else BorderWarmGray
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (ans) {
                                    true -> "${idx + 1} ✓"
                                    false -> "${idx + 1} ✕"
                                    null -> "${idx + 1}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCurrent) SoftWhite else DeepCharcoal
                            )
                        }
                    }
                }
            }

            WorthItCard {
                Text(
                    text = "Sekarang jawab dengan jujur.",
                    style = MaterialTheme.typography.labelLarge,
                    color = SageGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.headlineSmall,
                    color = DeepCharcoal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Bobot pertanyaan ini: +${question.weight} poin jika Ya",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Helper: Concrete example (PRD 3.2)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TipsAndUpdates,
                            contentDescription = null,
                            tint = DarkGreenText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Contoh alasan konkret",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkGreenText
                            )
                            Text(
                                text = question.concreteExample,
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkGreenText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Helper: Weak example (PRD 3.2)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftAmber,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = DarkAmberText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Alasan yang kurang kuat",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkAmberText
                            )
                            Text(
                                text = question.weakExample,
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkAmberText
                            )
                        }
                    }
                }
            }

            // Ya / Tidak answer choices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val yesSelected = selectedAnswer == true
                Button(
                    onClick = {
                        onAnswerQuestion(qIndex, true)
                        if (qIndex < 4) onGoToQuestion(qIndex + 1)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("filter_yes_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (yesSelected) 2.dp else 1.dp,
                        color = if (yesSelected) SageGreen else BorderWarmGray
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (yesSelected) SageGreen else SoftWhite,
                        contentColor = if (yesSelected) SoftWhite else DeepCharcoal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ya", style = MaterialTheme.typography.titleMedium)
                }

                val noSelected = selectedAnswer == false
                Button(
                    onClick = {
                        onAnswerQuestion(qIndex, false)
                        if (qIndex < 4) onGoToQuestion(qIndex + 1)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("filter_no_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (noSelected) 2.dp else 1.dp,
                        color = if (noSelected) DarkAmberText else BorderWarmGray
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (noSelected) SoftAmber else SoftWhite,
                        contentColor = if (noSelected) DarkAmberText else DeepCharcoal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tidak", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Navigation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = ::handleBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderWarmGray)
                ) {
                    Text(
                        text = if (qIndex == 0) "Ubah Barang" else "Sebelumnya",
                        color = DeepCharcoal
                    )
                }

                if (qIndex < 4) {
                    OutlinedButton(
                        onClick = { onGoToQuestion(qIndex + 1) },
                        enabled = selectedAnswer != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (selectedAnswer != null) SageGreen else BorderWarmGray)
                    ) {
                        Text(
                            text = "Berikutnya",
                            color = if (selectedAnswer != null) SageGreen else WarmGray
                        )
                    }
                }
            }

            if (qIndex == 4 || allAnswered) {
                WorthItPrimaryButton(
                    text = "Lihat Skor & Vonis",
                    enabled = allAnswered && !draft.isSubmittingAction,
                    onClick = onSubmitFilter,
                    modifier = Modifier.testTag("filter_submit_button")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
