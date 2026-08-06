package com.alkisstam.taskbar.ui.appmenu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorStateTest {

    private val errorText = "Error"

    private fun CalculatorState.digits(vararg ds: String): CalculatorState =
        ds.fold(this) { s, d -> s.digit(d) }

    @Test
    fun `addition evaluates`() {
        val s = CalculatorState().digit("5").operator('+').digit("3").equals(errorText)
        assertEquals("8", s.expression)
    }

    @Test
    fun `multiplication binds tighter than addition`() {
        val s = CalculatorState().digit("2").operator('+').digit("3").operator('×').digit("4").equals(errorText)
        assertEquals("14", s.expression)
    }

    @Test
    fun `division by zero shows Error`() {
        val s = CalculatorState().digit("5").operator('÷').digit("0").equals(errorText)
        assertEquals("Error", s.expression)
    }

    @Test
    fun `trailing operator dropped on equals`() {
        val s = CalculatorState().digit("7").operator('+').equals(errorText)
        assertEquals("7", s.expression)
    }

    @Test
    fun `percent alone divides by hundred`() {
        val s = CalculatorState().digits("5", "0").percent(errorText)
        assertEquals("0.5", s.expression)
    }

    @Test
    fun `percent after plus is relative to prefix`() {
        val s = CalculatorState().digits("2", "0", "0").operator('+').digits("1", "0").percent(errorText)
        assertEquals("200+20", s.expression)
    }

    @Test
    fun `digit after equals starts fresh expression`() {
        val s = CalculatorState().digit("5").operator('+').digit("3").equals(errorText).digit("9")
        assertEquals("9", s.expression)
    }

    @Test
    fun `operator replaces trailing operator`() {
        val s = CalculatorState().digit("5").operator('+').operator('-')
        assertEquals("5-", s.expression)
    }

    @Test
    fun `backspace on single char resets to zero`() {
        val s = CalculatorState().digit("5").backspace()
        assertEquals("0", s.expression)
    }

    @Test
    fun `second dot in operand ignored`() {
        val s = CalculatorState().digit("1").dot().dot().digit("5")
        assertEquals("1.5", s.expression)
    }

    @Test
    fun `unary applies to last operand only`() {
        val s = CalculatorState().digit("4").operator('+').digit("9").unary(errorText) { kotlin.math.sqrt(it) }
        assertEquals("4+3", s.expression)
    }

    @Test
    fun `preview null for single operand`() {
        assertNull(CalculatorState().digit("5").preview(errorText))
    }

    @Test
    fun `preview shows running total`() {
        assertEquals("5", CalculatorState().digit("2").operator('+').digit("3").preview(errorText))
    }

    @Test
    fun `double zero on empty display stays zero`() {
        assertEquals("0", CalculatorState().digit("00").expression)
    }

    @Test
    fun `subtraction below zero formats negative`() {
        val s = CalculatorState().digit("3").operator('-').digit("5").equals(errorText)
        assertEquals("-2", s.expression)
    }
}
