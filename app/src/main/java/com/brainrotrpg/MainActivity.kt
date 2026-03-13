package com.brainrotrpg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.brainrotrpg.ui.theme.BrainRotRPGTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrainRotRPGTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BrainRotNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BrainRotNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val startDestination = if (UsagePermissionHelper.hasUsagePermission(context)) "avatar" else "permission"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("permission") {
            PermissionScreen(
                onPermissionGranted = {
                    navController.navigate("avatar") {
                        popUpTo("permission") { inclusive = true }
                    }
                }
            )
        }
        composable("avatar") {
            val db = DatabaseProvider.getDatabase(context)
            val playerStatsDao = db.playerStatsDao()
            val roomObjectDao = db.roomObjectDao()
            val avatarViewModel: AvatarViewModel = viewModel(
                factory = AvatarViewModel.factory(playerStatsDao)
            )
            val roomObjectViewModel: RoomObjectViewModel = viewModel(
                factory = RoomObjectViewModel.factory(roomObjectDao, playerStatsDao)
            )
            AvatarScreen(
                avatarViewModel = avatarViewModel,
                roomObjectViewModel = roomObjectViewModel
            )
        }
    }
}
