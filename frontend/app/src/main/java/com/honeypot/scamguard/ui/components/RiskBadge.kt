package com.honeypot.scamguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.honeypot.scamguard.domain.model.ThreatLevel
import com.honeypot.scamguard.ui.theme.CyberGreen
import com.honeypot.scamguard.ui.theme.CyberGreenSurface
import com.honeypot.scamguard.ui.theme.SafeGreen
import com.honeypot.scamguard.ui.theme.ThreatRed
import com.honeypot.scamguard.ui.theme.ThreatRedDim
import com.honeypot.scamguard.ui.theme.WarningAmber
import com.honeypot.scamguard.ui.theme.WarningAmberDim
import com.honeypot.scamguard.ui.theme.InfoBlue
import com.honeypot.scamguard.ui.theme.InfoBlueDim

@Composable
fun RiskBadge(level: ThreatLevel, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (level) {
        ThreatLevel.CRITICAL -> ThreatRedDim to ThreatRed
        ThreatLevel.HIGH -> WarningAmberDim to WarningAmber
        ThreatLevel.MEDIUM -> InfoBlueDim to InfoBlue
        ThreatLevel.LOW -> CyberGreenSurface to CyberGreen
        ThreatLevel.NONE -> CyberGreenSurface to SafeGreen
    }
    Text(
        text = level.label.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
