package com.ictacademiccare.basecalculator.engine

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

object BaseEngine {

    const val DIGITS = "0123456789ABCDEFGHIJ"
    val MATH_CONTEXT = MathContext(100, RoundingMode.HALF_UP)

    private val BASE_NAMES = mapOf(
        2 to "Binary",
        3 to "Ternary",
        4 to "Base-4",
        5 to "Base-5",
        6 to "Base-6",
        7 to "Base-7",
        8 to "Octal",
        9 to "Base-9",
        10 to "Decimal",
        11 to "Base-11",
        12 to "Base-12",
        13 to "Base-13",
        14 to "Base-14",
        15 to "Base-15",
        16 to "Hexadecimal",
        17 to "Base-17",
        18 to "Base-18",
        19 to "Base-19",
        20 to "Base-20"
    )

    fun getBaseName(base: Int): String {
        return BASE_NAMES[base] ?: "Base-$base"
    }

    fun digitValue(ch: Char): Int {
        val upper = ch.uppercaseChar()
        val index = DIGITS.indexOf(upper)
        if (index < 0) {
            throw IllegalArgumentException("Invalid digit: $ch")
        }
        return index
    }

    fun digitChar(n: Int): Char {
        if (n < 0 || n >= DIGITS.length) {
            throw IllegalArgumentException("Invalid digit value: $n")
        }
        return DIGITS[n]
    }

    fun isDigitAllowed(ch: Char, base: Int): Boolean {
        val upper = ch.uppercaseChar()
        val index = DIGITS.indexOf(upper)
        return index in 0 until base
    }

    fun baseToDecimal(text: String, base: Int): BigDecimal {
        var str = text.trim().uppercase()
        if (str.isEmpty()) {
            throw IllegalArgumentException("Empty number")
        }

        var negative = false
        if (str.startsWith("-")) {
            negative = true
            str = str.substring(1)
        } else if (str.startsWith("+")) {
            str = str.substring(1)
        }

        if (str.isEmpty()) {
            throw IllegalArgumentException("Invalid number")
        }

        val dotCount = str.count { it == '.' }
        if (dotCount > 1) {
            throw IllegalArgumentException("Invalid decimal point")
        }

        val integerPart: String
        val fractionPart: String
        if (str.contains('.')) {
            val parts = str.split('.')
            integerPart = if (parts[0].isEmpty()) "0" else parts[0]
            fractionPart = if (parts.size > 1) parts[1] else ""
        } else {
            integerPart = str
            fractionPart = ""
        }

        for (ch in integerPart) {
            if (!isDigitAllowed(ch, base)) {
                throw IllegalArgumentException("$ch is invalid for base $base")
            }
        }
        for (ch in fractionPart) {
            if (!isDigitAllowed(ch, base)) {
                throw IllegalArgumentException("$ch is invalid for base $base")
            }
        }

        val baseBigDecimal = BigDecimal(base)
        var intResult = BigDecimal.ZERO
        for (ch in integerPart) {
            intResult = intResult.multiply(baseBigDecimal, MATH_CONTEXT)
                .add(BigDecimal(digitValue(ch)), MATH_CONTEXT)
        }

        var fracResult = BigDecimal.ZERO
        var power = BigDecimal.ONE
        for (ch in fractionPart) {
            power = power.multiply(baseBigDecimal, MATH_CONTEXT)
            val digitVal = BigDecimal(digitValue(ch))
            val term = digitVal.divide(power, MATH_CONTEXT)
            fracResult = fracResult.add(term, MATH_CONTEXT)
        }

        var result = intResult.add(fracResult, MATH_CONTEXT)
        if (negative) {
            result = result.negate()
        }
        return result
    }

    fun decimalToBase(number: BigDecimal, base: Int, precision: Int = 60): String {
        if (number.compareTo(BigDecimal.ZERO) == 0) {
            return "0"
        }

        val negative = number.signum() < 0
        val positiveNumber = if (negative) number.abs() else number

        val integerPartBig = positiveNumber.toBigInteger()
        val integerText = if (integerPartBig == BigInteger.ZERO) {
            "0"
        } else {
            val digits = StringBuilder()
            var n = integerPartBig
            val baseBig = BigInteger.valueOf(base.toLong())
            while (n > BigInteger.ZERO) {
                val remainder = n.remainder(baseBig).toInt()
                digits.append(digitChar(remainder))
                n = n.divide(baseBig)
            }
            digits.reverse().toString()
        }

        var fraction = positiveNumber.subtract(BigDecimal(integerPartBig), MATH_CONTEXT)
        val result = StringBuilder(integerText)

        if (fraction.compareTo(BigDecimal.ZERO) != 0) {
            val baseBigDecimal = BigDecimal(base)
            val fractionDigits = StringBuilder()

            for (i in 0 until precision) {
                if (fraction.compareTo(BigDecimal.ZERO) == 0) break

                fraction = fraction.multiply(baseBigDecimal, MATH_CONTEXT)
                val digitInt = fraction.toInt()
                fraction = fraction.subtract(BigDecimal(digitInt), MATH_CONTEXT)
                fractionDigits.append(digitChar(digitInt))
            }

            while (fractionDigits.isNotEmpty() && fractionDigits.last() == '0') {
                fractionDigits.deleteCharAt(fractionDigits.length - 1)
            }

            if (fractionDigits.isNotEmpty()) {
                result.append(".").append(fractionDigits)
            }
        }

        return if (negative) "-$result" else result.toString()
    }

    fun convertBase(value: String, fromBase: Int, toBase: Int): String {
        val decimal = baseToDecimal(value, fromBase)
        return decimalToBase(decimal, toBase)
    }
}