package com.example

import com.example.data.RoutineTextParser
import org.junit.Assert.*
import org.junit.Test

class RoutineTextParserTest {

    @Test
    fun testParseRoutineText_standardFormat() {
        val pastedText = """
            Sunday
            CSE 322 Software Engineering 08:30-10:00 Room 604 MC MAM
            Monday
            MAT 102 Linear Algebra 10:00-11:30 Room 302 AB KAS
        """.trimIndent()

        val parsed = RoutineTextParser.parseRoutineText(pastedText, "CSE", "A")
        assertEquals(2, parsed.size)
        
        assertEquals("Sunday", parsed[0].dayOfWeek)
        assertEquals("CSE 322", parsed[0].subjectCode)
        assertEquals("08:30", parsed[0].timeStart)
        assertEquals("10:00", parsed[0].timeEnd)
        assertEquals("604 MC", parsed[0].roomNo)
        assertEquals("MAM", parsed[0].teacherCode)

        assertEquals("Monday", parsed[1].dayOfWeek)
        assertEquals("MAT 102", parsed[1].subjectCode)
        assertEquals("10:00", parsed[1].timeStart)
        assertEquals("11:30", parsed[1].timeEnd)
        assertEquals("302 AB", parsed[1].roomNo)
        assertEquals("KAS", parsed[1].teacherCode)
    }

    @Test
    fun testParseRoutineText_abbreviationsAndTimes() {
        val pastedText = "Sun 8:30am - 10:00am CSE-221 Algorithms Lab-1 NSR"
        val parsed = RoutineTextParser.parseRoutineText(pastedText, "CSE", "A")
        
        assertTrue(parsed.isNotEmpty())
        assertEquals("Sunday", parsed[0].dayOfWeek)
        assertEquals("CSE 221", parsed[0].subjectCode)
        assertEquals("08:30", parsed[0].timeStart)
        assertEquals("10:00", parsed[0].timeEnd)
    }

    @Test
    fun testParseExamRoutineText_standardFormat() {
        val examText = """
            2026-07-20 (Monday)
            CSE 322 Software Engineering 10:00-13:00 Room 604 MC
            2026-07-22 (Wednesday)
            CSE 313 Compiler Design 13:30-16:30 Room 501 MC
        """.trimIndent()

        val parsed = RoutineTextParser.parseExamRoutineText(examText, "CSE", "A")
        assertEquals(2, parsed.size)
        assertEquals("2026-07-20", parsed[0].date)
        assertEquals("Monday", parsed[0].dayOfWeek)
        assertEquals("CSE 322", parsed[0].subjectCode)
        assertEquals("10:00", parsed[0].timeStart)
        assertEquals("13:00", parsed[0].timeEnd)
    }
}
