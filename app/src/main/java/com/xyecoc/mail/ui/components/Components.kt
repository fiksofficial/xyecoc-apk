package com.xyecoc.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xyecoc.mail.ui.model.MailRow

// Hoisted so the constant part of the avatar modifier isn't rebuilt every recomposition.
private val AvatarModifier = Modifier.size(44.dp).clip(CircleShape)

@Composable
fun MailListItem(
    mail: MailRow,
    onClick: () -> Unit,
    onStarClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (mail.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarChar = mail.displayName.firstOrNull()?.uppercaseChar() ?: '?'
            Box(
                modifier = AvatarModifier.background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarChar.toString(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mail.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (mail.read) FontWeight.Medium else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = mail.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = mail.displaySubject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (mail.read) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (mail.showSnippet) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mail.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (mail.tagName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val tagColor = mail.tagColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tagColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = mail.tagName,
                            color = tagColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onStarClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (mail.important) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Важное",
                        tint = if (mail.important) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
