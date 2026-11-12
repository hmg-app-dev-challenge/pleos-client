@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.chapark.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.chapark.app.R
import com.caverock.androidsvg.SVG

// Original semantic colors; dimensions below are the phone adaptation, not IVI tokens.
data class PleosPalette(val background: Color, val surface: Color, val ink: Color,
    val filled: Color, val pressed: Color, val onFilled: Color, val green: Color,
    val selected: Color, val off: Color, val knobOff: Color) {
    val subtle get() = ink.copy(alpha = .05f)
    val divider get() = ink.copy(alpha = .1f)
    val secondary get() = ink.copy(alpha = .64f)
}
val LocalPleos = staticCompositionLocalOf { PleosPalette(Color(0xFFF7F8FA), Color.White, Color(0xFF131417), Color(0xFF44464E), Color(0xFF313236), Color.White, Color(0xFF02C265), Color.White, Color(0x4D131417), Color.White) }
private val asta = FontFamily(Font(R.font.asta_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))), Font(R.font.asta_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))), Font(R.font.asta_sans, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))))
@Composable fun PleosTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) PleosPalette(Color(0xFF1D1E21), Color(0xFF313236), Color.White, Color(0xFFE0E1E6), Color(0xFFC7C9CE), Color(0xFF131417), Color(0xFF00E074), Color(0x33FFFFFF), Color(0x1AFFFFFF), Color(0xFFA4A8AE)) else LocalPleos.current
    fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(fontFamily = asta, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)
    val type = Typography(displayLarge=style(40,52,FontWeight.ExtraBold), headlineLarge=style(30,40,FontWeight.ExtraBold), headlineMedium=style(28,36,FontWeight.Bold), headlineSmall=style(24,32,FontWeight.Bold), titleLarge=style(22,28,FontWeight.Bold), titleMedium=style(18,24,FontWeight.Bold), titleSmall=style(16,22,FontWeight.Bold), bodyLarge=style(16,24), bodyMedium=style(15,22), bodySmall=style(13,18), labelLarge=style(16,22,FontWeight.Bold), labelMedium=style(13,18), labelSmall=style(12,16))
    CompositionLocalProvider(LocalPleos provides p, LocalContentColor provides p.ink) {
        MaterialTheme(colorScheme=(if(dark) darkColorScheme() else lightColorScheme()).copy(background=p.background, surface=p.surface, onSurface=p.ink, onBackground=p.ink, primary=p.filled, onPrimary=p.onFilled, surfaceContainer=p.subtle, onSurfaceVariant=p.secondary, outlineVariant=p.divider, error=if(dark) Color(0xFFFF4C28) else Color(0xFFFE3D16)), typography=type, content=content)
    }
}
@Composable fun PleosButton(text: String, modifier: Modifier = Modifier, filled: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) {
    val p=LocalPleos.current
    val interaction=remember { MutableInteractionSource() }; val pressed by interaction.collectIsPressedAsState()
    val bg=if(!enabled) p.subtle else if(filled) { if(pressed) p.pressed else p.filled } else p.ink.copy(alpha=if(pressed) .1f else .05f)
    Box(modifier.fillMaxWidth().heightIn(min=56.dp).clip(RoundedCornerShape(18.dp)).background(bg).clickable(interactionSource=interaction, indication=null, enabled=enabled, role=Role.Button, onClick=onClick).padding(horizontal=20.dp, vertical=16.dp), contentAlignment=Alignment.Center) {
        Text(text, style=MaterialTheme.typography.labelLarge, color=if(!enabled) p.secondary else if(filled) p.onFilled else p.ink, textAlign=TextAlign.Center)
    }
}
@Composable fun PleosSwitch(checked: Boolean, onCheckedChange: (Boolean)->Unit, modifier: Modifier=Modifier) {
    val p=LocalPleos.current
    Box(modifier.size(60.dp,48.dp).toggleable(checked, role=Role.Switch, onValueChange=onCheckedChange), contentAlignment=Alignment.Center) {
        Box(Modifier.size(54.dp,34.dp).background(if(checked)p.green else p.off,RoundedCornerShape(11.dp)).padding(5.dp), contentAlignment=if(checked) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(Modifier.size(24.dp).shadow(2.dp,RoundedCornerShape(6.dp)).background(if(checked)Color.White else p.knobOff,RoundedCornerShape(6.dp)))
        }
    }
}
@Composable fun PleosSegments(labels: List<String>, selected: Int, onSelect:(Int)->Unit) {
    val p=LocalPleos.current
    Row(Modifier.fillMaxWidth().selectableGroup().background(p.subtle,RoundedCornerShape(16.dp)).padding(4.dp)) {
        labels.forEachIndexed { index, text ->
            Box(Modifier.weight(1f).heightIn(min=48.dp).clip(RoundedCornerShape(12.dp)).background(if(index==selected)p.selected else Color.Transparent).selectable(index==selected,role=Role.Tab,onClick={onSelect(index)}).padding(vertical=12.dp),contentAlignment=Alignment.Center) {
                Text(text,style=MaterialTheme.typography.labelLarge)
            }
        }
    }
}
@Composable fun PleosCheck(checked:Boolean, onClick:()->Unit, modifier:Modifier=Modifier) {
    val p=LocalPleos.current; val context=LocalContext.current
    val bitmap=remember { val svg=context.assets.open("figma/check.svg").use { SVG.getFromInputStream(it) }; android.graphics.Bitmap.createBitmap(88,88,android.graphics.Bitmap.Config.ARGB_8888).also { svg.renderToCanvas(android.graphics.Canvas(it)) }.asImageBitmap() }
    Box(modifier.size(48.dp).toggleable(checked,role=Role.Checkbox,onValueChange={onClick()}),contentAlignment=Alignment.Center) {
        Box(Modifier.size(28.dp).background(if(checked)p.green else Color.Transparent,RoundedCornerShape(7.dp)).border(if(checked)0.dp else 2.dp,if(checked)Color.Transparent else p.ink.copy(alpha=.2f),RoundedCornerShape(7.dp)),contentAlignment=Alignment.Center) {
            if(checked) Image(bitmap,contentDescription=null,modifier=Modifier.size(26.dp))
        }
    }
}
