package com.chapark.app.data

import com.chapark.app.domain.Condition.*
import com.chapark.app.domain.Place

/** Fictional fixtures, deliberately without coordinates. Never send these to navigation. */
object DemoPlaces {
    val all = listOf(
        Place("demo-a", "숲가 쉼터", "숲 가까이에서 조용히 쉬어가는 곳", ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED, 12),
        Place("demo-b", "강변 쉼터", "강을 바라보며 준비한 식사를 즐기는 곳", ALLOWED, UNKNOWN, RESTRICTED, RESTRICTED, UNKNOWN, 12),
        Place("demo-c", "고요한 언덕", "선선한 밤공기와 함께 쉬어가는 곳", ALLOWED, ALLOWED, ALLOWED, RESTRICTED, RESTRICTED, 6),
    )
}
