package com.example.fin_ai.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.fin_ai.MainActivity
import com.example.fin_ai.R
import com.example.fin_ai.ui.util.Fmt

private val White = ColorProvider(Color.White, Color.White)
private val Muted = ColorProvider(Color(0xFFC4B5FD), Color(0xFFC4B5FD))
private val Green = ColorProvider(Color(0xFF34D399), Color(0xFF34D399))
private val Red = ColorProvider(Color(0xFFFCA5A5), Color(0xFFFCA5A5))

/** Home-screen widget: today's spend, month spend, budget left and the latest transaction. */
class FinWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = WidgetData.read(context)
        provideContent { Content(snap) }
    }

    @Composable
    private fun Content(snap: WidgetSnapshot?) {
        Column(
            GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Text("FinAI ⚡", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            if (snap == null) {
                Spacer(GlanceModifier.height(8.dp))
                Text("Open FinAI to sync your money here.", style = TextStyle(color = White, fontSize = 13.sp))
                return@Column
            }
            // Glance containers render at most 10 children, so content is grouped into sections.
            Column(GlanceModifier.fillMaxWidth().padding(top = 6.dp)) {
                Text("Spent today", style = TextStyle(color = Muted, fontSize = 11.sp))
                Text(Fmt.rupees(snap.todaySpent), style = TextStyle(color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(4.dp))
                Text("This month ${Fmt.rupees(snap.monthSpent)}", style = TextStyle(color = White, fontSize = 12.sp))
            }

            if (snap.budgetTotal > 0) Column(GlanceModifier.fillMaxWidth()) {
                Spacer(GlanceModifier.height(8.dp))
                val left = snap.budgetLeft
                Text(
                    if (left >= 0) "${Fmt.rupees(left)} budget left" else "${Fmt.rupees(-left)} over budget",
                    style = TextStyle(color = if (left >= 0) Green else Red, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (snap.budgetSpent / snap.budgetTotal).toFloat().coerceIn(0f, 1f),
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                    color = if (left >= 0) Green else Red,
                    backgroundColor = ColorProvider(Color(0x33FFFFFF), Color(0x33FFFFFF)),
                )
            }

            snap.lastTitle?.let { title ->
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title.take(22),
                        style = TextStyle(color = Muted, fontSize = 11.sp),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        snap.lastAmount.orEmpty(),
                        style = TextStyle(color = if (snap.lastIsCredit) Green else Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

class FinWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FinWidget()
}
