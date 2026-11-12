package com.chapark.ivi.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Development catalog; product navigation will be implemented separately. */
@Composable fun ButtonCatalog(onBack: () -> Unit) {
    var dark by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(0) }
    IviTheme(dark) {
        Surface(Modifier.fillMaxSize(), color=LocalIviColors.current.background) {
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(48.dp),
                verticalArrangement=Arrangement.spacedBy(32.dp)) {
                Text("Pleos 버튼 검수", style=IviType.headline)
                Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                    IviButton("진단으로", size=IviButtonSize.MEDIUM, onClick=onBack)
                    IviButton(if(dark)"Light 보기" else "Dark 보기", size=IviButtonSize.MEDIUM, filled=true) { dark=!dark }
                }
                Text("눌림은 터치 중 확인 · 실행 횟수 $count · 비활성 버튼은 실행되지 않음", style=IviType.smallBody)
                for(size in IviButtonSize.entries) {
                    Text(if(size==IviButtonSize.LARGE)"Large · 100 / 32" else "Medium · 80 / 24",style=IviType.title)
                    for(filled in listOf(false,true)) {
                        Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                            Text(if(filled)"Secondary" else "Primary",style=IviType.label)
                            // Wrap catalog items without scaling IVI controls down.
                            BoxWithConstraints {
                                if(maxWidth>=600.dp) Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                                    IviButton("Label",filled=filled,size=size) { count++ }
                                    IviButton("Label",filled=filled,size=size,enabled=false) { count++ }
                                } else Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                                    IviButton("Label",filled=filled,size=size) { count++ }
                                    IviButton("Label",filled=filled,size=size,enabled=false) { count++ }
                                }
                            }
                        }
                    }
                }
                IviButton("긴 문구가 들어가는 버튼은 한 줄로 표시됩니다",Modifier.width(360.dp),filled=true) { count++ }
            }
        }
    }
}
