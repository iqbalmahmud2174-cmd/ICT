package com.ictacademiccare.basecalculator

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.ictacademiccare.basecalculator.ui.screens.MainCalculatorScreen
import com.ictacademiccare.basecalculator.ui.screens.MoreBaseScreen
import com.ictacademiccare.basecalculator.ui.theme.BgDarkNavy
import com.ictacademiccare.basecalculator.ui.theme.ICTAcademicCareTheme
import com.ictacademiccare.basecalculator.viewmodel.CalculatorViewModel

enum class Screen {
    MAIN,
    MORE_BASE
}

class MainActivity : ComponentActivity() {

    private val viewModel: CalculatorViewModel by viewModels()
    private var currentScreen = mutableStateOf(Screen.MAIN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ICTAcademicCareTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = BgDarkNavy
                ) {
                    when (currentScreen.value) {
                        Screen.MAIN -> {
                            MainCalculatorScreen(
                                viewModel = viewModel,
                                onNavigateToMoreBase = { currentScreen.value = Screen.MORE_BASE }
                            )
                        }
                        Screen.MORE_BASE -> {
                            MoreBaseScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen.value = Screen.MAIN }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null || event.action != KeyEvent.ACTION_DOWN) {
            return super.onKeyDown(keyCode, event)
        }

        val char = event.unicodeChar.toChar()
        val isMoreScreen = currentScreen.value == Screen.MORE_BASE

        when (keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (isMoreScreen) viewModel.onMoreEqual() else viewModel.onMainEqual()
                return true
            }
            KeyEvent.KEYCODE_DEL -> {
                if (isMoreScreen) viewModel.onMoreBackspace() else viewModel.onMainBackspace()
                return true
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                if (isMoreScreen) viewModel.onMoreClear() else viewModel.onMainClear()
                return true
            }
        }

        if (char in '0'..'9' || char in 'a'..'j' || char in 'A'..'J' || char == '.') {
            val keyStr = char.uppercaseChar().toString()
            if (isMoreScreen) viewModel.onMoreDigit(keyStr) else viewModel.onMainDigit(keyStr)
            return true
        }

        when (char) {
            '+' -> {
                if (isMoreScreen) viewModel.onMoreOperator("+") else viewModel.onMainOperator("+")
                return true
            }
            '-' -> {
                if (isMoreScreen) viewModel.onMoreOperator("-") else viewModel.onMainOperator("-")
                return true
            }
            '*' -> {
                if (isMoreScreen) viewModel.onMoreOperator("*") else viewModel.onMainOperator("*")
                return true
            }
            '/' -> {
                if (isMoreScreen) viewModel.onMoreOperator("/") else viewModel.onMainOperator("/")
                return true
            }
            '(' -> {
                if (isMoreScreen) viewModel.onMoreParenthesis("(") else viewModel.onMainParenthesis("(")
                return true
            }
            ')' -> {
                if (isMoreScreen) viewModel.onMoreParenthesis(")") else viewModel.onMainParenthesis(")")
                return true
            }
            '=' -> {
                if (isMoreScreen) viewModel.onMoreEqual() else viewModel.onMainEqual()
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }
}