package com.example.valentinesgarage.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.valentinesgarage.ui.auth.AuthState
import com.example.valentinesgarage.ui.auth.AuthViewModel
import com.example.valentinesgarage.ui.auth.SignInScreen
import com.example.valentinesgarage.ui.checkin.CheckInScreen
import com.example.valentinesgarage.ui.reports.ManagerReportScreen
import com.example.valentinesgarage.ui.taskboard.TaskBoardScreen
import com.example.valentinesgarage.ui.taskboard.TaskListScreen

private object Route { const val CHECK_IN = "check_in"; const val TASKS = "tasks"; const val TASK_DETAIL = "tasks/{vehicleId}"; const val REPORTS = "reports" }

@Composable
fun GarageApp(authViewModel: AuthViewModel = hiltViewModel()) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentAuthState = authState
    when (currentAuthState) {
        AuthState.Loading -> Surface(modifier = Modifier.fillMaxSize()) { CircularProgressIndicator(modifier = Modifier.fillMaxSize().then(Modifier)) }
        AuthState.SignedOut -> SignInScreen(onSignIn = authViewModel::signIn)
        is AuthState.Error -> Surface(modifier = Modifier.fillMaxSize()) { Text("${currentAuthState.message}\nPlease restart the app.", style = MaterialTheme.typography.bodyLarge) }
        is AuthState.SignedIn -> AuthenticatedGarageApp(currentAuthState.user, authViewModel::signOut)
    }
}

@Composable
private fun AuthenticatedGarageApp(user: com.example.valentinesgarage.domain.model.GarageUser, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = if (user.role.name == "MANAGER") Route.REPORTS else Route.TASKS) {
        composable(Route.CHECK_IN) { CheckInScreen(user, onBack = { navController.popBackStack() }) }
        composable(Route.TASKS) { TaskListScreen(user, onOpenVehicle = { navController.navigate("tasks/$it") }, onCheckIn = { navController.navigate(Route.CHECK_IN) }, onReports = { navController.navigate(Route.REPORTS) }, onSignOut = onSignOut) }
        composable(Route.TASK_DETAIL) { TaskBoardScreen(user, onBack = { navController.popBackStack() }) }
        composable(Route.REPORTS) { ManagerReportScreen(user, onTasks = { navController.navigate(Route.TASKS) }, onCheckIn = { navController.navigate(Route.CHECK_IN) }, onSignOut = onSignOut) }
    }
}
