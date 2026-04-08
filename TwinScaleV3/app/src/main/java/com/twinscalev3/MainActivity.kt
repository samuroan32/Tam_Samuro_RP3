package com.twinscalev3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twinscalev3.ui.navigation.TwinScaleNavGraph
import com.twinscalev3.ui.theme.TwinScaleTheme
import com.twinscalev3.viewmodel.AppViewModel
import com.twinscalev3.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TwinScaleTheme {
                val vm: AppViewModel = viewModel(factory = AppViewModelFactory())
                TwinScaleNavGraph(vm)
            }
        }
    }
}
