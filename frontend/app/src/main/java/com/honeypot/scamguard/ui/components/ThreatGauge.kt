package com.honeypot.scamguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.honeypot.scamguard.ui.theme.CyberGreen
import com.honeypot.scamguard.ui.theme.Navy600
import com.honeypot.scamguard.ui.theme.ThreatRed
import com.honeypot.scamguard.ui.theme.TextMuted
import com.honeypot.scamguard.ui.theme.WarningAmber

@Composable
fun ThreatGauge(score: Int, modifier: Modifier = Modifier) {
    val sweepAngle = (score / 100f) * 240f
    val gaugeColor = when {
        score >= 70 -> ThreatRed
        score >= 40 -> WarningAmber
        else -> CyberGreen
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(140.dp)) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background track
            drawArc(
                color = Navy600,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Active arc
            drawArc(
                color = gaugeColor,
                startAngle = 150f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = gaugeColor
            )
            Text(
                text = "THREAT",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
