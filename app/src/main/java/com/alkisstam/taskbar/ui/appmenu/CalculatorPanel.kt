package com.alkisstam.taskbar.ui.appmenu

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.ui.theme.grain
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sqrt

// '-' kept last so it isn't parsed as a character-class range (e.g. "+-×" == range '+'..'×').
private const val OPERATORS = "+×÷^-"

private fun isOperator(token: String) = token.length == 1 && token[0] in OPERATORS

private fun tokenize(expr: String): List<String> =
    Regex("(?<=[$OPERATORS])|(?=[$OPERATORS])").split(expr).filter { it.isNotEmpty() }

/** Drops a trailing operator so the token list always ends on a complete operand. */
private fun completeTokens(tokens: List<String>): List<String> =
    if (tokens.isNotEmpty() && isOperator(tokens.last())) tokens.dropLast(1) else tokens

private fun evaluate(tokens: List<String>): Double {
    if (tokens.isEmpty()) return 0.0
    val nums = mutableListOf(tokens[0].toDoubleOrNull() ?: 0.0)
    val ops = mutableListOf<Char>()
    var idx = 1
    while (idx < tokens.size - 1) {
        ops.add(tokens[idx].firstOrNull() ?: '+')
        nums.add(tokens[idx + 1].toDoubleOrNull() ?: 0.0)
        idx += 2
    }
    var i = 0
    while (i < ops.size) {
        if (ops[i] == '×' || ops[i] == '÷' || ops[i] == '^') {
            nums[i] = compute(nums[i], ops[i], nums[i + 1])
            nums.removeAt(i + 1)
            ops.removeAt(i)
        } else {
            i++
        }
    }
    var total = nums[0]
    for (j in ops.indices) total = compute(total, ops[j], nums[j + 1])
    return total
}

private data class CalculatorState(
    val expression: String = "0",
    val justEvaluated: Boolean = false
) {
    private fun tokens() = tokenize(expression)

    fun digit(d: String): CalculatorState {
        if (justEvaluated) return copy(expression = if (d == "00") "0" else d, justEvaluated = false)
        val lastToken = tokens().lastOrNull() ?: ""
        return when {
            expression == "0" -> copy(expression = if (d == "00") "0" else d)
            isOperator(lastToken) -> copy(expression = expression + if (d == "00") "0" else d)
            lastToken == "0" -> copy(expression = expression.dropLast(1) + if (d == "00") "0" else d)
            else -> copy(expression = expression + d)
        }
    }

    fun dot(): CalculatorState {
        if (justEvaluated) return copy(expression = "0.", justEvaluated = false)
        val lastToken = tokens().lastOrNull() ?: ""
        return when {
            isOperator(lastToken) -> copy(expression = expression + "0.")
            lastToken.contains('.') -> this
            else -> copy(expression = expression + ".")
        }
    }

    fun backspace(): CalculatorState {
        if (justEvaluated) return copy(expression = "0", justEvaluated = false)
        if (expression.length <= 1) return copy(expression = "0")
        return copy(expression = expression.dropLast(1).ifEmpty { "0" })
    }

    fun percent(): CalculatorState {
        val tokens = tokens().toMutableList()
        if (tokens.isEmpty()) return this
        val lastIdx = tokens.size - 1
        val lastNum = tokens[lastIdx].toDoubleOrNull() ?: return this
        val opIdx = lastIdx - 1
        val op = tokens.getOrNull(opIdx)?.firstOrNull()
        val prefix = if (opIdx > 0) evaluate(tokens.subList(0, opIdx)) else 0.0
        val result = if (op == '+' || op == '-') prefix * (lastNum / 100.0) else lastNum / 100.0
        tokens[lastIdx] = formatResult(result)
        return copy(expression = tokens.joinToString(""), justEvaluated = false)
    }

    fun operator(op: Char): CalculatorState {
        val lastToken = tokens().lastOrNull() ?: ""
        val base = if (isOperator(lastToken)) expression.dropLast(1) else expression
        return copy(expression = base + op, justEvaluated = false)
    }

    fun equals(): CalculatorState {
        val safe = completeTokens(tokens())
        if (safe.isEmpty()) return this
        return copy(expression = formatResult(evaluate(safe)), justEvaluated = true)
    }

    fun unary(fn: (Double) -> Double): CalculatorState {
        val tokens = tokens().toMutableList()
        if (tokens.isEmpty()) return this
        val lastIdx = tokens.size - 1
        val lastNum = tokens[lastIdx].toDoubleOrNull() ?: return this
        tokens[lastIdx] = formatResult(fn(lastNum))
        return copy(expression = tokens.joinToString(""), justEvaluated = false)
    }

    fun insertPi(): CalculatorState {
        val piStr = formatResult(Math.PI)
        if (justEvaluated || expression == "0") return copy(expression = piStr, justEvaluated = false)
        val tokens = tokens()
        val lastToken = tokens.lastOrNull() ?: ""
        return if (isOperator(lastToken)) {
            copy(expression = expression + piStr)
        } else {
            copy(expression = tokens.dropLast(1).joinToString("") + piStr)
        }
    }

    /** Live running total shown under the expression, or null while there's nothing to preview yet. */
    fun preview(): String? {
        val tokens = tokens()
        if (tokens.size <= 1) return null
        val safe = completeTokens(tokens)
        if (safe.isEmpty()) return null
        return formatResult(evaluate(safe))
    }
}

private fun compute(a: Double, op: Char, b: Double): Double = when (op) {
    '+' -> a + b
    '-' -> a - b
    '×' -> a * b
    '÷' -> if (b == 0.0) Double.NaN else a / b
    '^' -> Math.pow(a, b)
    else -> b
}

private fun formatResult(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Error"
    if (value == value.toLong().toDouble() && abs(value) < 1e15) {
        return value.toLong().toString()
    }
    return String.format(java.util.Locale.US, "%.10f", value).trimEnd('0').trimEnd('.')
}

private fun sinDeg(v: Double) = kotlin.math.sin(Math.toRadians(v))
private fun cosDeg(v: Double) = kotlin.math.cos(Math.toRadians(v))
private fun tanDeg(v: Double) = kotlin.math.tan(Math.toRadians(v))

@Composable
fun CalculatorPanel(
    panelOutlineEnabled: Boolean = true,
    translucentMode: Boolean = false,
    translucentAlpha: Float = 0.80f,
    cornerRadiusDp: Float = 16f,
    dockWidthFraction: Float = 0.98f,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(CalculatorState()) }
    var sciExpanded by rememberSaveable { mutableStateOf(false) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val glassBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val cornerShape = RoundedCornerShape(cornerRadiusDp.dp)

    Surface(
        shape = cornerShape,
        color = if (translucentMode) surfaceColor.copy(alpha = translucentAlpha) else surfaceColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = if (translucentMode) 0.dp else 3.dp,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth(dockWidthFraction)
            .then(if (panelOutlineEnabled) Modifier.border(1.dp, TaskbarOutlineGreen, cornerShape) else Modifier)
            .then(if (translucentMode && !panelOutlineEnabled) Modifier.border(1.dp, glassBorderColor, cornerShape) else Modifier)
            .clip(cornerShape)
            .grain(enabled = translucentMode)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = state.expression,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            val preview = state.preview()
            Text(
                text = preview.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            var dragAccum by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAccum = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                dragAccum += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                if (dragAccum < -40f) sciExpanded = true
                                else if (dragAccum > 40f) sciExpanded = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sciExpanded) {
                    SciRow(
                        keys = listOf("sin", "cos", "tan", "√"),
                        onClick = { label ->
                            state = when (label) {
                                "sin" -> state.unary(::sinDeg)
                                "cos" -> state.unary(::cosDeg)
                                "tan" -> state.unary(::tanDeg)
                                else -> state.unary(::sqrt)
                            }
                        }
                    )
                    SciRow(
                        keys = listOf("ln", "log", "^", "π"),
                        onClick = { label ->
                            state = when (label) {
                                "ln" -> state.unary(::ln)
                                "log" -> state.unary(::log10)
                                "^" -> state.operator('^')
                                else -> state.insertPi()
                            }
                        }
                    )
                }
            }

            Box(modifier = Modifier.height(4.dp))

            val digitColor = MaterialTheme.colorScheme.surfaceVariant
            val onDigitColor = MaterialTheme.colorScheme.onSurfaceVariant
            val opColor = MaterialTheme.colorScheme.secondaryContainer
            val onOpColor = MaterialTheme.colorScheme.onSecondaryContainer
            val equalsColor = MaterialTheme.colorScheme.primary
            val onEqualsColor = MaterialTheme.colorScheme.onPrimary

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcRow {
                    CalcButton("AC", digitColor, onDigitColor, Modifier.weight(1f)) { state = CalculatorState() }
                    CalcButton("%", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.percent() }
                    CalcIconButton(Icons.AutoMirrored.Filled.Backspace, digitColor, onDigitColor, Modifier.weight(1f)) { state = state.backspace() }
                    CalcButton("÷", opColor, onOpColor, Modifier.weight(1f)) { state = state.operator('÷') }
                }
                CalcRow {
                    CalcButton("7", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("7") }
                    CalcButton("8", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("8") }
                    CalcButton("9", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("9") }
                    CalcButton("×", opColor, onOpColor, Modifier.weight(1f)) { state = state.operator('×') }
                }
                CalcRow {
                    CalcButton("4", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("4") }
                    CalcButton("5", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("5") }
                    CalcButton("6", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("6") }
                    CalcButton("−", opColor, onOpColor, Modifier.weight(1f)) { state = state.operator('-') }
                }
                CalcRow {
                    CalcButton("1", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("1") }
                    CalcButton("2", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("2") }
                    CalcButton("3", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("3") }
                    CalcButton("+", opColor, onOpColor, Modifier.weight(1f)) { state = state.operator('+') }
                }
                CalcRow {
                    CalcButton("00", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("00") }
                    CalcButton("0", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.digit("0") }
                    CalcButton(".", digitColor, onDigitColor, Modifier.weight(1f)) { state = state.dot() }
                    CalcButton("=", equalsColor, onEqualsColor, Modifier.weight(1f)) { state = state.equals() }
                }
            }
        }
    }
}

@Composable
private fun CalcRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun SciRow(keys: List<String>, onClick: (String) -> Unit) {
    val sciColor = MaterialTheme.colorScheme.tertiaryContainer
    val onSciColor = MaterialTheme.colorScheme.onTertiaryContainer
    CalcRow {
        keys.forEach { label ->
            CalcButton(label, sciColor, onSciColor, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium) { onClick(label) }
        }
    }
}

@Composable
private fun CalcButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = style, color = contentColor)
    }
}

@Composable
private fun CalcIconButton(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = "Backspace", tint = contentColor)
    }
}
