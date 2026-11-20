package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.Formatters
import com.example.ui.theme.BorderWarmGray
import com.example.ui.theme.DarkAmberText
import com.example.ui.theme.DarkCoralText
import com.example.ui.theme.DarkGreenText
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.PaleSage
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftAmber
import com.example.ui.theme.SoftApricot
import com.example.ui.theme.SoftCoral
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmGray

enum class StatusTone {
    POSITIVE,
    WARNING,
    DANGER,
    NEUTRAL
}

@Composable
fun WorthItCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SoftWhite,
    borderColor: Color = BorderWarmGray,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    tone: StatusTone,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val (bg, fg, defaultIcon) = when (tone) {
        StatusTone.POSITIVE -> Triple(SoftGreen, DarkGreenText, Icons.Outlined.CheckCircle)
        StatusTone.WARNING -> Triple(SoftAmber, DarkAmberText, Icons.Outlined.WarningAmber)
        StatusTone.DANGER -> Triple(SoftCoral, DarkCoralText, Icons.Outlined.ErrorOutline)
        StatusTone.NEUTRAL -> Triple(PaleSage, DeepCharcoal, Icons.Outlined.AccessTime)
    }
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon ?: defaultIcon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LabeledInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    helperText: String? = null,
    errorText: String? = null,
    prefixText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = DeepCharcoal
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray
                )
            },
            prefix = prefixText?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = DeepCharcoal
                    )
                }
            },
            isError = errorText != null,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SoftWhite,
                unfocusedContainerColor = SoftWhite,
                errorContainerColor = SoftWhite,
                focusedBorderColor = SageGreen,
                unfocusedBorderColor = BorderWarmGray,
                errorBorderColor = DarkCoralText,
                focusedTextColor = DeepCharcoal,
                unfocusedTextColor = DeepCharcoal,
                cursorColor = SageGreen
            )
        )
        if (errorText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = DarkCoralText
            )
        } else if (helperText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray
            )
        }
    }
}

@Composable
fun RupiahPreviewText(digits: String) {
    val parsed = digits.toLongOrNull()
    if (parsed != null && parsed > 0L) {
        Text(
            text = "Terbaca: ${Formatters.formatRupiah(parsed)}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = SageGreen
        )
    }
}

@Composable
fun WorthItPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SageGreen,
            contentColor = SoftWhite,
            disabledContainerColor = PaleSage,
            disabledContentColor = WarmGray
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun WorthItSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (enabled) SageGreen else BorderWarmGray),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SageGreen,
            disabledContentColor = WarmGray
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    WorthItCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SoftApricot),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PauseCircle,
                    contentDescription = null,
                    tint = SageGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = DeepCharcoal
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray
            )
            if (ctaLabel != null && onCtaClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                WorthItSecondaryButton(text = ctaLabel, onClick = onCtaClick)
            }
        }
    }
}

@Composable
fun LoadingStateView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SageGreen)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Menyiapkan ruang berpikirmu...",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray
            )
        }
    }
}

@Composable
fun StorageErrorBanner(
    message: String,
    onExportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    WorthItCard(
        containerColor = SoftCoral,
        borderColor = DarkCoralText.copy(alpha = 0.25f)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = DarkCoralText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Peringatan Penyimpanan Lokal",
                    style = MaterialTheme.typography.titleSmall,
                    color = DarkCoralText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkCoralText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onExportClick) {
                        Text("Ekspor Backup JSON", color = DarkCoralText)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Tutup", color = WarmGray)
                    }
                }
            }
        }
    }
}

@Composable
fun ClockTamperBanner() {
    WorthItCard(
        containerColor = SoftAmber,
        borderColor = DarkAmberText.copy(alpha = 0.25f)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = DarkAmberText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Perubahan Jam Perangkat Terdeteksi",
                    style = MaterialTheme.typography.titleSmall,
                    color = DarkAmberText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Waktu perangkat terdeteksi bergerak mundur. Timer tetap mengacu pada waktu absolut dan berfungsi sebagai alat bantu kebiasaan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkAmberText
                )
            }
        }
    }
}

@Composable
fun OrganicHeaderBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SoftWhite)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = SoftApricot.copy(alpha = 0.65f),
                radius = size.height * 0.85f,
                center = Offset(size.width * 0.92f, size.height * 0.15f)
            )
            drawCircle(
                color = PaleSage.copy(alpha = 0.55f),
                radius = size.height * 0.55f,
                center = Offset(size.width * 0.78f, size.height * 0.95f)
            )
        }
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = PaleSage
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PauseCircle,
                        contentDescription = null,
                        tint = SageGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WorthIt?",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SageGreen
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = DeepCharcoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray
            )
        }
    }
}
