package com.wakeuphero.core

// 입력 칸 자동 정리: 숫자만 쳐도 구분 기호를 끼워 넣는다 (사용자가 기호를 직접 쳐도 결과는 같음)

// "0700" → "07:00" (숫자 4개까지, 세 번째 숫자부터 콜론)
fun formatTimeInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(4)
    return if (digits.length <= 2) digits else digits.substring(0, 2) + ":" + digits.substring(2)
}

// "20261201" → "2026-12-01" (숫자 8개까지, 연 4자리·월 2자리 뒤에 하이픈)
fun formatDateInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 4 || i == 6) append('-')
            append(c)
        }
    }
}
