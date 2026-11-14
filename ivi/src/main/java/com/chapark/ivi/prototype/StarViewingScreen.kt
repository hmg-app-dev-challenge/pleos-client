package com.chapark.ivi.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chapark.ivi.design.*
import kotlin.random.Random

private data class Star(val x:Float,val y:Float,val radius:Float,val opacity:Float,val bright:Boolean)

/** Authored ambient scene, not an astronomical map. Static geometry has no frame timer. */
@Composable internal fun StarViewingScreen(placeName:String,onExit:()->Unit) {
    val p=LocalIviColors.current
    Column(Modifier.fillMaxSize().background(p.background).windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal=IviSpacing.section,vertical=IviSpacing.related).testTag("star-view-screen"),
        verticalArrangement=Arrangement.spacedBy(IviSpacing.related)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(IviSpacing.related)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("별 보기",style=IviType.title,color=p.ink)
                Text(placeName,style=IviType.label,color=p.tertiary)
            }
            IviButton("준비 화면으로",Modifier.testTag("star-view-exit"),size=IviButtonSize.MEDIUM,onClick=onExit)
        }
        StarField(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(32.dp)))
        Column(verticalArrangement=Arrangement.spacedBy(IviSpacing.control)) {
            Text("별빛 아래, 잠시 쉬어가요.",style=IviType.smallTitle,color=p.ink)
            Text("밤하늘을 담은 휴식 화면이에요. 실제 별자리·날씨 정보는 제공하지 않아요.",
                style=IviType.label,color=p.tertiary)
        }
    }
}

@Composable private fun StarField(modifier:Modifier) {
    val p=LocalIviColors.current
    val stars=remember {
        val random=Random(4817)
        List(240){index->
            Star(.025f+random.nextFloat()*.95f,.025f+random.nextFloat()*.95f,
                if(index%29==0)3.6f else 1.3f+random.nextFloat()*1.5f,
                if(index%29==0).95f else .32f+random.nextFloat()*.53f,index%29==0)
        } + List(150){
            val x=random.nextFloat()
            Star(.025f+x*.95f,(.78f-.56f*x+(random.nextFloat()-.5f)*.3f).coerceIn(.03f,.97f),
                .9f+random.nextFloat()*1.1f,.2f+random.nextFloat()*.4f,false)
        }
    }
    Canvas(modifier.testTag("star-field").semantics{contentDescription="밝기가 다른 별들이 펼쳐진 밤하늘 배경"}) {
        drawRect(Brush.verticalGradient(listOf(p.onFilled,p.background)))
        stars.forEach { star->
            val center=Offset(size.width*star.x,size.height*star.y)
            val radius=star.radius.dp.toPx()
            if(star.bright){
                drawCircle(Brush.radialGradient(listOf(p.ink.copy(alpha=.22f),p.ink.copy(alpha=0f)),center,radius*6),radius*6,center)
                val length=radius*3
                drawLine(p.ink.copy(alpha=.4f),center-Offset(length,0f),center+Offset(length,0f),1.dp.toPx())
                drawLine(p.ink.copy(alpha=.4f),center-Offset(0f,length),center+Offset(0f,length),1.dp.toPx())
            }
            drawCircle(p.ink.copy(alpha=star.opacity),radius,center)
        }
    }
}
