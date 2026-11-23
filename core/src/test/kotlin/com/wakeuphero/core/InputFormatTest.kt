package com.wakeuphero.core

import kotlin.test.Test
import kotlin.test.assertEquals

class InputFormatTest {
    @Test
    fun `시각은 숫자만 쳐도 두 자리 뒤에 콜론이 들어간다`() {
        assertEquals("07:00", formatTimeInput("0700"))
        assertEquals("07:3", formatTimeInput("073"))
        assertEquals("07", formatTimeInput("07")) // 콜론은 세 번째 숫자부터
        assertEquals("", formatTimeInput(""))
    }

    @Test
    fun `시각은 콜론을 직접 쳐도 같고 숫자 4개까지만 받는다`() {
        assertEquals("07:00", formatTimeInput("07:00"))
        assertEquals("12:34", formatTimeInput("123456"))
        assertEquals("1", formatTimeInput("a1 "))
    }

    @Test
    fun `날짜는 숫자만 쳐도 연 월 뒤에 하이픈이 들어간다`() {
        assertEquals("2026-12-01", formatDateInput("20261201"))
        assertEquals("2026", formatDateInput("2026"))
        assertEquals("2026-1", formatDateInput("20261"))
        assertEquals("2026-12", formatDateInput("202612"))
        assertEquals("2026-12-0", formatDateInput("2026120"))
    }

    @Test
    fun `날짜는 하이픈을 직접 쳐도 같고 숫자 8개까지만 받는다`() {
        assertEquals("2026-12-01", formatDateInput("2026-12-01"))
        assertEquals("2026-12-01", formatDateInput("2026120199"))
    }

    @Test
    fun `자동으로 고친 글은 그대로 읽힌다`() {
        assertEquals(java.time.LocalTime.of(7, 0), parseTimeText(formatTimeInput("0700")))
        assertEquals(java.time.LocalDate.of(2026, 12, 1), parseDateText(formatDateInput("20261201")))
    }
}
