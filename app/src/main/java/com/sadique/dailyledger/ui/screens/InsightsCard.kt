package com.sadique.dailyledger.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sadique.dailyledger.ai.*
@Composable fun InsightsCard(snapshot:SpendingSnapshot,cloudTips:List<SpendingTip>,status:String,enabled:Boolean,onEnable:()->Unit){
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)){
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Icon(Icons.Outlined.Lightbulb,null,tint=MaterialTheme.colorScheme.primary);Text("Is mahine ke mashwaray",style=MaterialTheme.typography.titleLarge);Text("Recorded expenses • through ${snapshot.through}",style=MaterialTheme.typography.labelMedium)
            snapshot.tips.forEach{tip->Column(verticalArrangement=Arrangement.spacedBy(3.dp)){Text(tip.title,style=MaterialTheme.typography.titleSmall);Text(tip.detail)}}
            HorizontalDivider()
            if(!enabled){Text("Cloud AI can add saving suggestions using your category totals. Account details and transaction notes are not included.",style=MaterialTheme.typography.bodySmall);TextButton(onClick=onEnable){Text("Set up AI suggestions")}}
            else{Text(status.ifBlank{"Cloud suggestions appear when records are available."},style=MaterialTheme.typography.bodySmall);cloudTips.forEach{tip->Text(tip.title,style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.primary);Text(tip.detail)}}
        }
    }
}
