package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.WateeraApp
import com.example.ui.state.WateeraViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: WateeraViewModel by viewModels {
        val app = application as WateeraApplication
        WateeraViewModel.Factory(app.habitRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WateeraApp(viewModel = viewModel)
        }
    }
}
