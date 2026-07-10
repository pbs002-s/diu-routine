package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Quote(val text: String, val author: String)

val academicQuotes = listOf(
    Quote("Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
    Quote("The beautiful thing about learning is that no one can take it away from you.", "B.B. King"),
    Quote("Live as if you were to die tomorrow. Learn as if you were to live forever.", "Mahatma Gandhi"),
    Quote("The mind is not a vessel to be filled, but a fire to be kindled.", "Plutarch"),
    Quote("Success is not final, failure is not fatal: it is the courage to continue that counts.", "Winston Churchill"),
    Quote("It always seems impossible until it's done.", "Nelson Mandela"),
    Quote("Believe you can and you're halfway there.", "Theodore Roosevelt")
)

@Composable
fun MotivationalQuoteCard(modifier: Modifier = Modifier) {
    var activeQuote by remember { mutableStateOf(academicQuotes.random()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                var next = academicQuotes.random()
                while (next == activeQuote) {
                    next = academicQuotes.random()
                }
                activeQuote = next
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (MaterialTheme.colorScheme.primary == Color(0xFF006C4C)) {
                            Color(0xFFF0F5F0) // Light Theme Soft Green
                        } else {
                            Color(0xFF18231E) // Dark Theme Soft Green
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = "Quote Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Quote Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"${activeQuote.text}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "— ${activeQuote.author}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Tap to Refresh",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
