package com.chapark.ivi.design

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.caverock.androidsvg.SVG

val IviColors.green:Color get()=if(ink==Color.White)Color(0xFF00E074) else Color(0xFF02C265)
private val IviColors.selected:Color get()=if(ink==Color.White)Color.White.copy(alpha=.2f) else Color.White

/** 17:429/435: 80×48 track, 8 padding, 32 knob. Row adds an 80dp touch target. */
@Composable fun IviToggleRow(text:String,checked:Boolean,onChange:(Boolean)->Unit) {
    val p=LocalIviColors.current
    Row(Modifier.fillMaxWidth().heightIn(min=100.dp).toggleable(checked,interactionSource=remember{MutableInteractionSource()},indication=null,role=Role.Switch,onValueChange=onChange).padding(vertical=16.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(24.dp)) {
        Text(text,Modifier.weight(1f),style=IviType.body)
        Box(Modifier.size(80.dp,48.dp).background(if(checked)p.green else p.ink.copy(alpha=if(p.ink==Color.White).1f else .3f),RoundedCornerShape(16.dp)).padding(8.dp),
            contentAlignment=if(checked)Alignment.CenterEnd else Alignment.CenterStart) {
            Box(Modifier.size(32.dp).shadow(8.dp,RoundedCornerShape(8.dp),ambientColor=Color.Black.copy(alpha=.08f),spotColor=Color.Black.copy(alpha=.08f)).background(if(checked || p.ink!=Color.White)Color.White else Color(0xFFA4A8AE),RoundedCornerShape(8.dp)))
        }
    }
}
@Composable fun IviCheckRow(text:String,checked:Boolean,detail:String?=null,onChange:()->Unit) {
    val p=LocalIviColors.current; val context=LocalContext.current
    val icon=remember {
        val svg=context.assets.open("figma/check.svg").use { SVG.getFromInputStream(it) }
        android.graphics.Bitmap.createBitmap(44,44,android.graphics.Bitmap.Config.ARGB_8888).also { svg.renderToCanvas(android.graphics.Canvas(it)) }.asImageBitmap()
    }
    Row(Modifier.fillMaxWidth().heightIn(min=96.dp).toggleable(checked,interactionSource=remember{MutableInteractionSource()},indication=null,role=Role.Checkbox,onValueChange={onChange()}).padding(vertical=12.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
        Box(Modifier.size(64.dp),contentAlignment=Alignment.Center) {
            Box(Modifier.size(48.dp).background(if(checked)p.green else Color.Transparent,RoundedCornerShape(12.dp))
                .then(if(!checked)Modifier.border(4.dp,p.ink.copy(alpha=.2f),RoundedCornerShape(12.dp))else Modifier),contentAlignment=Alignment.Center) {
                if(checked) Image(icon,null,Modifier.size(44.dp))
            }
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(text,style=IviType.body)
            detail?.let { Text(it,style=IviType.label,color=p.tertiary) }
        }
    }
}
/** 18:3106 / 18:3061: Large solid; width responds to the app window, never scales type. */
@Composable fun IviSegments(labels:List<String>,selected:Int,onSelect:(Int)->Unit) {
    val p=LocalIviColors.current
    Row(Modifier.widthIn(max=880.dp).fillMaxWidth().height(100.dp).selectableGroup().background(p.basic,RoundedCornerShape(32.dp)).padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
        labels.forEachIndexed { i,label ->
            if(i>0) Box(Modifier.width(2.dp).height(36.dp).background(if(i==selected || i-1==selected)Color.Transparent else p.divider))
            Box(Modifier.weight(1f).fillMaxHeight().then(if(i==selected)Modifier.shadow(8.dp,RoundedCornerShape(24.dp),ambientColor=Color.Black.copy(alpha=.16f),spotColor=Color.Black.copy(alpha=.16f))else Modifier)
                .clip(RoundedCornerShape(24.dp)).background(if(i==selected)p.selected else Color.Transparent)
                .selectable(i==selected,interactionSource=remember{MutableInteractionSource()},indication=null,role=Role.Tab,onClick={onSelect(i)}).padding(horizontal=12.dp),contentAlignment=Alignment.Center) {
                Text(label,style=IviType.smallTitle,color=if(i==selected)p.ink else p.secondary,textAlign=TextAlign.Center,maxLines=1)
            }
        }
    }
}
/** Source 18:1697. Header icon/close are off; two source text buttons are used. */
@Composable fun IviConfirm(title:String,description:String,confirm:String,onConfirm:()->Unit,onDismiss:()->Unit) {
    val p=LocalIviColors.current
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.widthIn(max=808.dp).fillMaxWidth(.92f).heightIn(max=1152.dp).clip(RoundedCornerShape(48.dp))
            .background(p.surface).border(2.dp,p.basic,RoundedCornerShape(48.dp)).verticalScroll(rememberScrollState()).padding(48.dp),
            verticalArrangement=Arrangement.spacedBy(40.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(24.dp)) {
                Text(title,style=IviType.title.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.ExtraBold),textAlign=TextAlign.Center)
                Text(description,style=IviType.body,color=p.secondary,textAlign=TextAlign.Center)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                IviButton(confirm,Modifier.weight(1f),filled=true,onClick=onConfirm)
                IviButton("취소",Modifier.weight(1f),onClick=onDismiss)
            }
        }
    }
}
