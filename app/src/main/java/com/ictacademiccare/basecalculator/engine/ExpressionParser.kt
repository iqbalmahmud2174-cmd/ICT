package com.ictacademiccare.basecalculator.engine

import java.math.BigDecimal

class ExpressionParser(
    private val rawExpression: String,
    private val base: Int
) {
    private val tokens: List<String> = tokenize(rawExpression)
    private var position: Int = 0

    private fun tokenize(input: String): List<String> {
        val expression = input.uppercase()
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")

        val tokenList = mutableListOf<String>()
        var i = 0

        while (i < expression.length) {
            val ch = expression[i]

            if (ch.isWhitespace()) {
                i++
                continue
            }

            if (ch in "+-*/()") {
                tokenList.add(ch.toString())
                i++
                continue
            }

            if (BaseEngine.DIGITS.contains(ch) || ch == '.') {
                val start = i
                var dotCount = 0

                while (i < expression.length) {
                    val c = expression[i]
                    if (BaseEngine.DIGITS.contains(c)) {
                        i++
                    } else if (c == '.') {
                        dotCount++
                        if (dotCount > 1) {
                            throw IllegalArgumentException("Invalid decimal point in number")
                        }
                        i++
                    } else {
                        break
                    }
                }

                val number = expression.substring(start, i)
                BaseEngine.baseToDecimal(number, base)
                tokenList.add(number)
                continue
            }

            throw IllegalArgumentException("Invalid character: $ch")
        }

        return tokenList
    }

    private fun current(): String? = if (position < tokens.size) tokens[position] else null

    private fun eat(): String? {
        val token = current()
        position++
        return token
    }

    fun parse(): BigDecimal {
        if (tokens.isEmpty()) {
            throw IllegalArgumentException("Empty expression")
        }
        val result = parseExpression()
        if (current() != null) {
            throw IllegalArgumentException("Unexpected token: ${current()}")
        }
        return result
    }

    private fun parseExpression(): BigDecimal {
        var result = parseTerm()

        while (true) {
            val token = current()
            if (token == "+") {
                eat()
                result = result.add(parseTerm(), BaseEngine.MATH_CONTEXT)
            } else if (token == "-") {
                eat()
                result = result.subtract(parseTerm(), BaseEngine.MATH_CONTEXT)
            } else {
                break
            }
        }
        return result
    }

    private fun parseTerm(): BigDecimal {
        var result = parseFactor()

        while (true) {
            val token = current()
            if (token == "*") {
                eat()
                result = result.multiply(parseFactor(), BaseEngine.MATH_CONTEXT)
            } else if (token == "/") {
                eat()
                val divisor = parseFactor()
                if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                    throw ArithmeticException("Division by zero")
                }
                result = result.divide(divisor, BaseEngine.MATH_CONTEXT)
            } else {
                break
            }
        }
        return result
    }

    private fun parseFactor(): BigDecimal {
        val token = current() ?: throw IllegalArgumentException("Expected number or expression")

        if (token == "-") {
            eat()
            return parseFactor().negate()
        }

        if (token == "+") {
            eat()
            return parseFactor()
        }

        if (token == "(") {
            eat()
            val result = parseExpression()
            if (current() != ")") {
                throw IllegalArgumentException("Missing closing parenthesis ')'")
            }
            eat()
            return result
        }

        eat()
        return BaseEngine.baseToDecimal(token, base)
    }

    companion object {
        fun calculate(expression: String, base: Int): BigDecimal {
            val parser = ExpressionParser(expression, base)
            return parser.parse()
        }
    }
}