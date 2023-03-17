package fusion.ai.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fusion.ai.R

class HandyWidget : GlanceAppWidget() {

    @Composable
    override fun Content() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(R.color.widget_background_color)
                .padding(10.dp)
        ) {
            Text(
                text = "Ask HandyAI",
                style = TextStyle(
                    color = ColorProvider(R.color.widget_text_color),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = GlanceModifier.padding(start = 5.dp)
            )

            Spacer(GlanceModifier.height(20.dp))

            Text(
                text = "The widget is in development, please check our Github for the latest update.",
                style = TextStyle(
                    color = ColorProvider(R.color.widget_text_color),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = GlanceModifier.padding(start = 5.dp)
            )
        }
    }
}

@Suppress("unused")
class ActionUpdate : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        HandyWidget().updateAll(context)
    }
}
