package com.twinscalev3.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.twinscalev3.ui.screen.ChatScreen
import com.twinscalev3.ui.screen.ComparisonScreen
import com.twinscalev3.ui.screen.GrowthScreen
import com.twinscalev3.ui.screen.RoomScreen
import com.twinscalev3.ui.screen.SettingsScreen
import com.twinscalev3.ui.screen.SplashScreen
import com.twinscalev3.viewmodel.AppViewModel

@Composable
fun TwinScaleNavGraph(vm: AppViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "splash") {
        composable("splash") { SplashScreen { nav.navigate("room") { popUpTo("splash") { inclusive = true } } } }
        composable("room") { RoomScreen(vm) { nav.navigate("growth") } }
        composable("growth") { GrowthScreen(vm, onChat = { nav.navigate("chat") }, onCompare = { nav.navigate("compare") }, onSettings = { nav.navigate("settings") }) }
        composable("chat") { ChatScreen(vm) }
        composable("compare") { ComparisonScreen(vm) }
        composable("settings") { SettingsScreen(vm) }
    }
}
