package com.wakeuphero.core

// S8-2: 사진 글자 인식(OCR) 결과를 표의 줄로 묶는다
// OCR은 글자 덩어리와 그 위치(상자)를 주지만, 표의 한 줄을 한 줄로 돌려준다는 보장이 없다.
// 그래서 상자들의 높이(세로 위치)로 다시 줄을 만들고, 줄 안에서는 왼쪽부터 늘어놓는다.
// 결과는 붙여넣기 글자와 같은 모양이라 parseRosterText로 그대로 해석한다.

// 글자 상자 하나 (화면 좌표: 오른쪽·아래로 갈수록 커짐)
data class TextBox(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Double get() = (left + right) / 2.0
    val centerY: Double get() = (top + bottom) / 2.0
    val height: Int get() = bottom - top
}

fun rowsFromBoxes(boxes: List<TextBox>): String =
    groupRows(boxes).joinToString("\n") { row -> row.joinToString(" ") { it.text } }

// 상자들을 줄로 묶는다: 위 줄부터, 줄 안에서는 왼쪽부터
fun groupRows(boxes: List<TextBox>): List<List<TextBox>> {
    if (boxes.isEmpty()) return emptyList()
    // 같은 줄로 볼 거리: 보통 글자 높이의 절반 (중앙값이라 아주 큰 글자·작은 글자에 덜 흔들림)
    val tolerance = boxes.map { it.height }.sorted()[boxes.size / 2].coerceAtLeast(1) / 2.0

    // 위에서부터 보면서, 지금 줄의 평균 높이와 가까우면 같은 줄, 멀면 새 줄
    // (평균을 따라가므로 사진이 조금 기울어 줄이 서서히 내려가도 같은 줄로 묶인다)
    val rows = mutableListOf<MutableList<TextBox>>()
    for (box in boxes.sortedBy { it.centerY }) {
        val row = rows.lastOrNull()
        if (row != null && box.centerY - row.map { it.centerY }.average() <= tolerance) row += box
        else rows += mutableListOf(box)
    }
    return rows.map { row -> row.sortedBy { it.left } }
}
