package com.ictacademiccare.basecalculator.viewmodel

import androidx.lifecycle.ViewModel
import com.ictacademiccare.basecalculator.engine.BaseEngine
import com.ictacademiccare.basecalculator.engine.ExpressionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

data class MainCalculatorState(
    val activeBase: Int = 10,
    val binValue: String = "0",
    val octValue: String = "0",
    val decValue: String = "0",
    val hexValue: String = "0",
    val expressionDisplay: String = "",
    val errorMessage: String? = null,
    val currentNumber: String = "",
    val internalExpression: String = "",
    val justCalculated: Boolean = false
)

data class MoreBaseState(
    val fromBase: Int = 10,
    val toBase: Int = 16,
    val inputDisplay: String = "0",
    val resultDisplay: String = "",
    val expressionDisplay: String = "",
    val currentNumber: String = "",
    val internalExpression: String = "",
    val justCalculated: Boolean = false,
    val errorMessage: String? = null
)

class CalculatorViewModel : ViewModel() {

    private val _mainState = MutableStateFlow(MainCalculatorState())
    val mainState: StateFlow<MainCalculatorState> = _mainState.asStateFlow()

    private val _moreState = MutableStateFlow(MoreBaseState())
    val moreState: StateFlow<MoreBaseState> = _moreState.asStateFlow()

    fun selectMainBase(base: Int) {
        _mainState.update { it.copy(activeBase = base, errorMessage = null) }
    }

    fun onMainDigit(key: String) {
        val upperKey = key.uppercase()
        val state = _mainState.value
        val base = state.activeBase

        if (upperKey != ".") {
            if (!BaseEngine.isDigitAllowed(upperKey[0], base)) return
        }

        var current = state.currentNumber
        var expr = state.internalExpression
        var justCalc = state.justCalculated

        if (justCalc) {
            expr = ""
            current = ""
            justCalc = false
        }

        if (upperKey == ".") {
            if (current.contains(".")) return
            current = if (current.isEmpty()) "0." else "$current."
        } else {
            current = if (current == "0") upperKey else current + upperKey
        }

        val updatedDisplays = calculateAllBases(current, base)

        _mainState.update {
            it.copy(
                currentNumber = current,
                internalExpression = expr,
                justCalculated = justCalc,
                expressionDisplay = formatExpression(expr),
                binValue = updatedDisplays[2] ?: it.binValue,
                octValue = updatedDisplays[8] ?: it.octValue,
                decValue = updatedDisplays[10] ?: it.decValue,
                hexValue = updatedDisplays[16] ?: it.hexValue,
                errorMessage = null
            )
        }
    }

    fun onMainOperator(operator: String) {
        val state = _mainState.value
        val base = state.activeBase
        var expr = state.internalExpression
        var current = state.currentNumber

        if (current.isNotEmpty()) {
            try {
                val value = BaseEngine.baseToDecimal(current, base)
                expr += value.toPlainString()
                current = ""
            } catch (_: Exception) {
                return
            }
        }

        if (expr.isEmpty()) {
            if (operator == "-") expr = "-" else return
        } else {
            val lastChar = expr.last()
            if (lastChar in setOf('+', '-', '*', '/')) {
                expr = expr.dropLast(1) + operator
            } else {
                expr += operator
            }
        }

        _mainState.update {
            it.copy(
                internalExpression = expr,
                currentNumber = current,
                expressionDisplay = formatExpression(expr),
                justCalculated = false,
                errorMessage = null
            )
        }
    }

    fun onMainParenthesis(symbol: String) {
        val state = _mainState.value
        val base = state.activeBase
        var expr = state.internalExpression
        var current = state.currentNumber

        if (symbol == "(") {
            if (current.isNotEmpty()) {
                try {
                    val value = BaseEngine.baseToDecimal(current, base)
                    expr += value.toPlainString() + "*"
                    current = ""
                } catch (_: Exception) {
                    return
                }
            }
            expr += "("
        } else {
            if (current.isNotEmpty()) {
                try {
                    val value = BaseEngine.baseToDecimal(current, base)
                    expr += value.toPlainString()
                    current = ""
                } catch (_: Exception) {
                    return
                }
            }
            expr += ")"
        }

        _mainState.update {
            it.copy(
                internalExpression = expr,
                currentNumber = current,
                expressionDisplay = formatExpression(expr),
                errorMessage = null
            )
        }
    }

    fun onMainEqual() {
        val state = _mainState.value
        var expr = state.internalExpression
        val current = state.currentNumber
        val base = state.activeBase

        if (current.isNotEmpty()) {
            try {
                val value = BaseEngine.baseToDecimal(current, base)
                expr += value.toPlainString()
            } catch (_: Exception) {
                return
            }
        }

        if (expr.isEmpty()) return

        try {
            val result = ExpressionParser.calculate(expr, 10)
            val displays = calculateAllBases(result)
            val newCurrent = BaseEngine.decimalToBase(result, base)

            _mainState.update {
                it.copy(
                    binValue = displays[2] ?: "0",
                    octValue = displays[8] ?: "0",
                    decValue = displays[10] ?: "0",
                    hexValue = displays[16] ?: "0",
                    currentNumber = newCurrent,
                    expressionDisplay = "${formatExpression(expr)} =",
                    internalExpression = "",
                    justCalculated = true,
                    errorMessage = null
                )
            }
        } catch (e: ArithmeticException) {
            _mainState.update { it.copy(errorMessage = "ERROR: Division by zero") }
        } catch (e: Exception) {
            _mainState.update { it.copy(errorMessage = "ERROR: Invalid expression") }
        }
    }

    fun onMainBackspace() {
        val state = _mainState.value
        if (state.justCalculated) {
            onMainClear()
            return
        }

        if (state.currentNumber.isNotEmpty()) {
            var newCurrent = state.currentNumber.dropLast(1)
            if (newCurrent.isEmpty()) newCurrent = "0"
            val displays = calculateAllBases(newCurrent, state.activeBase)
            _mainState.update {
                it.copy(
                    currentNumber = if (newCurrent == "0") "" else newCurrent,
                    binValue = displays[2] ?: "0",
                    octValue = displays[8] ?: "0",
                    decValue = displays[10] ?: "0",
                    hexValue = displays[16] ?: "0",
                    errorMessage = null
                )
            }
        } else if (state.internalExpression.isNotEmpty()) {
            val newExpr = state.internalExpression.dropLast(1)
            _mainState.update {
                it.copy(
                    internalExpression = newExpr,
                    expressionDisplay = formatExpression(newExpr),
                    errorMessage = null
                )
            }
        }
    }

    fun onMainClear() {
        _mainState.update {
            it.copy(
                binValue = "0",
                octValue = "0",
                decValue = "0",
                hexValue = "0",
                expressionDisplay = "",
                internalExpression = "",
                currentNumber = "",
                justCalculated = false,
                errorMessage = null
            )
        }
    }

    fun onMainDirectInput(base: Int, text: String) {
        val clean = text.trim().uppercase()
        if (clean.isEmpty()) return
        try {
            val dec = BaseEngine.baseToDecimal(clean, base)
            val displays = calculateAllBases(dec)
            _mainState.update {
                it.copy(
                    activeBase = base,
                    currentNumber = clean,
                    internalExpression = "",
                    expressionDisplay = "",
                    justCalculated = false,
                    binValue = if (base == 2) clean else displays[2] ?: "0",
                    octValue = if (base == 8) clean else displays[8] ?: "0",
                    decValue = if (base == 10) clean else displays[10] ?: "0",
                    hexValue = if (base == 16) clean else displays[16] ?: "0",
                    errorMessage = null
                )
            }
        } catch (_: Exception) {}
    }

    private fun calculateAllBases(text: String, base: Int): Map<Int, String> {
        return try {
            val dec = BaseEngine.baseToDecimal(text, base)
            calculateAllBases(dec)
        } catch (_: Exception) {
            mapOf(2 to "0", 8 to "0", 10 to "0", 16 to "0")
        }
    }

    private fun calculateAllBases(decimal: BigDecimal): Map<Int, String> {
        return mapOf(
            2 to BaseEngine.decimalToBase(decimal, 2),
            8 to BaseEngine.decimalToBase(decimal, 8),
            10 to BaseEngine.decimalToBase(decimal, 10),
            16 to BaseEngine.decimalToBase(decimal, 16)
        )
    }

    private fun formatExpression(expr: String): String {
        return expr.replace("*", "×").replace("/", "÷").replace("-", "−")
    }

    // MORE BASE
    fun setFromBase(base: Int) {
        _moreState.update {
            it.copy(
                fromBase = base,
                inputDisplay = "0",
                resultDisplay = "",
                expressionDisplay = "",
                currentNumber = "",
                internalExpression = "",
                justCalculated = false,
                errorMessage = null
            )
        }
    }

    fun setToBase(base: Int) {
        _moreState.update { it.copy(toBase = base) }
        autoConvertMore()
    }

    fun onMoreDigit(key: String) {
        val upperKey = key.uppercase()
        val state = _moreState.value
        val base = state.fromBase

        if (upperKey != ".") {
            if (!BaseEngine.isDigitAllowed(upperKey[0], base)) return
        }

        var current = state.currentNumber
        var expr = state.internalExpression
        var justCalc = state.justCalculated

        if (justCalc) {
            expr = ""
            current = ""
            justCalc = false
        }

        if (upperKey == ".") {
            if (current.contains(".")) return
            current = if (current.isEmpty()) "0." else "$current."
        } else {
            current = if (current == "0") upperKey else current + upperKey
        }

        _moreState.update {
            it.copy(
                currentNumber = current,
                inputDisplay = current,
                internalExpression = expr,
                justCalculated = justCalc,
                errorMessage = null
            )
        }
        autoConvertMore()
    }

    fun onMoreOperator(operator: String) {
        val state = _moreState.value
        val base = state.fromBase
        var expr = state.internalExpression
        var current = state.currentNumber

        if (current.isNotEmpty()) {
            try {
                BaseEngine.baseToDecimal(current, base)
                expr += current
                current = ""
            } catch (_: Exception) {
                return
            }
        }

        if (expr.isEmpty()) {
            if (operator == "-") expr = "-" else return
        } else {
            val lastChar = expr.last()
            if (lastChar in setOf('+', '-', '*', '/')) {
                expr = expr.dropLast(1) + operator
            } else {
                expr += operator
            }
        }

        _moreState.update {
            it.copy(
                internalExpression = expr,
                currentNumber = "",
                inputDisplay = "0",
                expressionDisplay = formatExpression(expr),
                errorMessage = null
            )
        }
    }

    fun onMoreParenthesis(symbol: String) {
        val state = _moreState.value
        val base = state.fromBase
        var expr = state.internalExpression
        var current = state.currentNumber

        if (symbol == "(") {
            if (current.isNotEmpty()) {
                try {
                    BaseEngine.baseToDecimal(current, base)
                    expr += "$current*"
                    current = ""
                } catch (_: Exception) {
                    return
                }
            }
            expr += "("
        } else {
            if (current.isNotEmpty()) {
                try {
                    BaseEngine.baseToDecimal(current, base)
                    expr += current
                    current = ""
                } catch (_: Exception) {
                    return
                }
            }
            expr += ")"
        }

        _moreState.update {
            it.copy(
                internalExpression = expr,
                currentNumber = "",
                inputDisplay = "0",
                expressionDisplay = formatExpression(expr),
                errorMessage = null
            )
        }
    }

    fun onMoreEqual() {
        val state = _moreState.value
        var expr = state.internalExpression
        val current = state.currentNumber
        val fromBase = state.fromBase
        val toBase = state.toBase

        if (current.isNotEmpty()) {
            try {
                BaseEngine.baseToDecimal(current, fromBase)
                expr += current
            } catch (_: Exception) {
                return
            }
        }

        if (expr.isEmpty()) return

        try {
            val resultDecimal = ExpressionParser.calculate(expr, fromBase)
            val resultOutput = BaseEngine.decimalToBase(resultDecimal, toBase)

            _moreState.update {
                it.copy(
                    resultDisplay = resultOutput,
                    inputDisplay = resultOutput,
                    expressionDisplay = "${formatExpression(expr)} =",
                    internalExpression = "",
                    currentNumber = resultOutput,
                    justCalculated = true,
                    errorMessage = null
                )
            }
        } catch (e: ArithmeticException) {
            _moreState.update { it.copy(resultDisplay = "Division by zero", errorMessage = "Division by zero") }
        } catch (e: Exception) {
            _moreState.update { it.copy(resultDisplay = "Invalid expression", errorMessage = "Invalid expression") }
        }
    }

    fun onMoreConvert() {
        val state = _moreState.value
        val value = state.inputDisplay.trim()
        if (value.isEmpty()) return
        try {
            val result = BaseEngine.convertBase(value, state.fromBase, state.toBase)
            _moreState.update { it.copy(resultDisplay = result, errorMessage = null) }
        } catch (_: Exception) {
            _moreState.update { it.copy(resultDisplay = "Invalid Number") }
        }
    }

    fun onMoreBackspace() {
        val state = _moreState.value
        if (state.justCalculated) {
            onMoreClear()
            return
        }

        if (state.currentNumber.isNotEmpty()) {
            var newCurrent = state.currentNumber.dropLast(1)
            if (newCurrent.isEmpty()) newCurrent = "0"
            _moreState.update {
                it.copy(
                    currentNumber = if (newCurrent == "0") "" else newCurrent,
                    inputDisplay = newCurrent,
                    errorMessage = null
                )
            }
            autoConvertMore()
        } else if (state.internalExpression.isNotEmpty()) {
            val newExpr = state.internalExpression.dropLast(1)
            _moreState.update {
                it.copy(
                    internalExpression = newExpr,
                    expressionDisplay = formatExpression(newExpr),
                    errorMessage = null
                )
            }
        }
    }

    fun onMoreClear() {
        _moreState.update {
            it.copy(
                inputDisplay = "0",
                resultDisplay = "",
                expressionDisplay = "",
                internalExpression = "",
                currentNumber = "",
                justCalculated = false,
                errorMessage = null
            )
        }
    }

    fun onMoreDirectInput(text: String) {
        val state = _moreState.value
        val base = state.fromBase
        val upper = text.uppercase()

        val sb = StringBuilder()
        var hasDot = false
        for (c in upper) {
            if (c == '.' && !hasDot) {
                sb.append(c)
                hasDot = true
            } else if (BaseEngine.isDigitAllowed(c, base)) {
                sb.append(c)
            }
        }
        val valid = sb.toString()

        _moreState.update {
            it.copy(
                inputDisplay = valid,
                currentNumber = valid,
                internalExpression = "",
                justCalculated = false,
                errorMessage = null
            )
        }
        autoConvertMore()
    }

    private fun autoConvertMore() {
        val state = _moreState.value
        val text = state.currentNumber.ifEmpty { state.inputDisplay }.trim()
        if (text.isEmpty() || text == "0") {
            _moreState.update { it.copy(resultDisplay = if (text == "0") "0" else "") }
            return
        }
        try {
            val result = BaseEngine.convertBase(text, state.fromBase, state.toBase)
            _moreState.update { it.copy(resultDisplay = result, errorMessage = null) }
        } catch (_: Exception) {}
    }
}