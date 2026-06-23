package com.example.fundsmanager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReceiptBadge(hasReceipt: Boolean, modifier: Modifier = Modifier) {
    val background = if (hasReceipt) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)
    val foreground = if (hasReceipt) Color(0xFF166534) else Color(0xFF9A3412)
    Text(
        text = receiptLabel(hasReceipt),
        modifier = modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = foreground,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun TypeBadge(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold
    )
}
