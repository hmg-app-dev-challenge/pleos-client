@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package com.chapark.ivi.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.chapark.ivi.R

// Pleos Foundations 21:1056. IVI values, deliberately separate from phone adaptations.
private val asta = FontFamily(
    Font(R.font.asta_sans, FontWeight.Normal, variationSettings=FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.asta_sans, FontWeight.Bold, variationSettings=FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.asta_sans, FontWeight.ExtraBold, variationSettings=FontVariation.Settings(FontVariation.weight(800))))
data class IviColors(val background: Color, val surface: Color, val ink: Color, val filled: Color, val filledPressed: Color, val onFilled: Color) {
    val isDark get() = background.luminance() < .5f
    val basic get() = ink.copy(alpha=.05f)
    val basicPressed get() = ink.copy(alpha=.1f)
    val secondary get() = ink.copy(alpha=.84f)
    val tertiary get() = ink.copy(alpha=.64f)
    val quaternary get() = ink.copy(alpha=.32f)
    val divider get() = ink.copy(alpha=.1f)
    val basicDisabled get() = if (isDark) Color(0xFF131417).copy(alpha=.2f) else Color.White.copy(alpha=.2f)
}
private val light = IviColors(Color(0xFFF7F8FA),Color.White,Color(0xFF131417),Color(0xFF44464E),Color(0xFF313236),Color.White)
private val dark = IviColors(Color(0xFF1D1E21),Color(0xFF313236),Color.White,Color(0xFFE0E1E6),Color(0xFFC7C9CE),Color(0xFF131417))
val LocalIviColors = staticCompositionLocalOf { light }
object IviType {
    private fun style(size:Int,line:Int,weight:FontWeight) = TextStyle(fontFamily=asta,fontSize=size.sp,lineHeight=line.sp,fontWeight=weight)
    val headline = style(60,80,FontWeight.ExtraBold)
    val title = style(40,52,FontWeight.Bold)
    val smallTitle = style(32,40,FontWeight.Bold)
    val body = style(30,38,FontWeight.Normal)
    val bodyStrong = style(30,38,FontWeight.Bold)
    val smallBody = style(26,34,FontWeight.Normal)
    val label = style(24,28,FontWeight.Normal)
}
@Composable fun IviTheme(isDark:Boolean, nightMode:Boolean=false, content:@Composable ()->Unit) {
    val colors = if(nightMode || isDark) dark else light
    CompositionLocalProvider(LocalIviColors provides colors, LocalContentColor provides colors.ink) {
        MaterialTheme(colorScheme=(if(colors.isDark) darkColorScheme() else lightColorScheme()).copy(
            background=colors.background,onBackground=colors.ink, surface=colors.surface,onSurface=colors.ink,
            primary=colors.filled,onPrimary=colors.onFilled,onSurfaceVariant=colors.secondary,
            surfaceVariant=colors.surface,surfaceContainer=colors.surface,outline=colors.tertiary,outlineVariant=colors.divider),
            typography=Typography(bodyLarge=IviType.body,bodyMedium=IviType.smallBody,labelLarge=IviType.smallTitle), content=content)
    }
}
enum class IviButtonSize { LARGE, MEDIUM }
/** Figma 16:293/339 Primary; 16:500/546 Secondary; 16:569 Medium. No optional icons. */
@Composable fun IviButton(text:String, modifier:Modifier=Modifier, filled:Boolean=false, enabled:Boolean=true, size:IviButtonSize=IviButtonSize.LARGE, onClick:()->Unit) {
    val p=LocalIviColors.current; val interaction=remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val medium=size==IviButtonSize.MEDIUM
    val shape=RoundedCornerShape(if(medium)24.dp else 32.dp)
    val color=if(!enabled) {if(filled)p.basic else p.basicDisabled} else if(filled) {if(pressed)p.filledPressed else p.filled} else if(pressed)p.basicPressed else p.basic
    Box(modifier.widthIn(min=if(medium)120.dp else 140.dp).heightIn(min=if(medium)80.dp else 100.dp).clip(shape).background(color)
        .then(if(!enabled)Modifier.border(2.dp,p.divider,shape) else Modifier)
        .clickable(interactionSource=interaction,indication=null,enabled=enabled,role=Role.Button,onClick=onClick)
        // Figma's disabled border contributes 2px per side to intrinsic width.
        .padding(horizontal=if(enabled)0.dp else 2.dp)
        .padding(horizontal=if(medium)32.dp else 40.dp,vertical=16.dp),contentAlignment=Alignment.Center) {
        Text(text,style=if(medium)IviType.bodyStrong else IviType.smallTitle,textAlign=TextAlign.Center,maxLines=1,overflow=TextOverflow.Ellipsis,color=if(!enabled)p.quaternary else if(filled)p.onFilled else p.ink)
    }
}
