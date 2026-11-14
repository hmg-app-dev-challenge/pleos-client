package com.chapark.ivi.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chapark.app.domain.SolarDay
import com.chapark.ivi.design.*
import java.time.*
import kotlin.math.sin

/** Discrete minute updates: no animation, motion preferences need no separate bypass. */
@Composable internal fun SolarSky(date:LocalDate,zone:ZoneId,day:SolarDay,now:Instant){
    val rise=day.sunrise?.instant
    val set=day.sunset?.instant
    val today=now.atZone(zone).toLocalDate()==date
    val daytime=today && rise!=null && set!=null && now>=rise && now<set
    val fraction=if(daytime)Duration.between(rise,now).seconds.toFloat()/Duration.between(rise,set).seconds else 0f
    val p=LocalIviColors.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.surface).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text(if(rise==null || set==null)"해의 시각 확인 필요" else if(!today)"여행 날짜 · $date" else if(daytime)"해가 떠 있는 시간" else "해가 진 시간",style=IviType.smallTitle)
        Canvas(Modifier.fillMaxWidth().height(140.dp)){
            val top=if(daytime)Color(0xFF253A56) else Color(0xFF151824)
            val bottom=if(daytime)Color(0xFFB67D50) else Color(0xFF453326)
            drawRect(Brush.verticalGradient(listOf(top,bottom)))
            val horizon=size.height*.8f
            drawLine(Color(0xFFC9A97D).copy(alpha=.5f),Offset(0f,horizon),Offset(size.width,horizon),2.dp.toPx())
            if(daytime){
                val x=size.width*(.08f+.84f*fraction)
                val y=horizon-sin(Math.PI*fraction).toFloat()*size.height*.6f
                drawCircle(Color(0xFFFFD99A),14.dp.toPx(),Offset(x,y))
            }else {
                listOf(.13f to .25f,.38f to .42f,.64f to .2f,.83f to .36f).forEach{(x,y)->
                    drawCircle(Color(0xFFE5DAC4),2.dp.toPx(),Offset(size.width*x,size.height*y))
                }
            }
        }
        Text("해의 시각을 담은 화면 · 현장 풍경과 날씨는 별도로 확인해 주세요.",style=IviType.label,color=p.secondary)
        if(rise==null || set==null)Text("이 날짜에는 일출 또는 일몰 시각을 구할 수 없어요.",style=IviType.label)
    }
}
