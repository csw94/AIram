package com.wakeuphero.core

import java.io.File
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.YearMonth

// 콘솔 프로그램 진입점
// 실행: ./gradlew run                      → schedule.txt 읽기
//       ./gradlew run --args="다른파일.txt" → 다른 파일 읽기
fun main(args: Array<String>) {
    // 윈도우 콘솔에서도 한글이 깨지지 않도록 UTF-8로 출력
    System.setOut(PrintStream(System.out, true, Charsets.UTF_8))

    val path = args.firstOrNull() ?: "schedule.txt"
    val file = File(path)
    if (!file.exists()) {
        println("근무표 파일을 찾을 수 없어요: ${file.absolutePath}")
        return
    }

    // 1. 입력: 파일 읽기 → 근무표로 바꾸기
    val result = parseSchedule(file.readLines(Charsets.UTF_8), defaultShiftTypes)
    if (result.errors.isNotEmpty()) {
        println("⚠️ 잘못된 줄은 건너뛰었어요")
        result.errors.forEach { println("  - $it") }
        println()
    }

    // 2. 출력: 근무가 있는 달마다 달력 출력
    val months = result.workDays.map { YearMonth.from(it.date) }.distinct().sorted()
    months.forEach { month ->
        println(formatCalendar(month, result.workDays, defaultShiftTypes))
        println()
    }

    // 3. 다음 알람
    val next = nextAlarm(LocalDateTime.now(), result.workDays, defaultShiftTypes, defaultAlarmRules)
    println(nextAlarmMessage(next))
}
