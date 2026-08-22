package com.xyecoc.mail.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xyecoc.mail.XyecocApp
import com.xyecoc.mail.ui.screens.auth.*
import com.xyecoc.mail.ui.screens.compose.ComposeScreen
import com.xyecoc.mail.ui.screens.inbox.InboxScreen
import com.xyecoc.mail.ui.screens.reader.ReaderScreen
import com.xyecoc.mail.ui.screens.settings.SettingsScreen

object Screen {
    const val Login = "login"
    const val Register = "register"
    const val ForgotPassword = "forgot_password"
    const val TwoFactor = "two_factor/{email}/{pass}"
    const val Inbox = "inbox"
    const val Reader = "reader/{mailId}"
    const val Compose = "compose?to={to}&subject={subject}&body={body}"
    const val Settings = "settings"
    const val Support = "support"

    fun createTwoFactor(email: String, pass: String) = "two_factor/$email/$pass"
    fun createReader(mailId: Long) = "reader/$mailId"
    fun createCompose(to: String = "", subject: String = "", body: String = ""): String {
        val encodedTo = Uri.encode(to)
        val encodedSubject = Uri.encode(subject)
        val encodedBody = Uri.encode(body)
        return "compose?to=$encodedTo&subject=$encodedSubject&body=$encodedBody"
    }
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val hasToken = !XyecocApp.instance.securePrefs.getToken().isNullOrBlank()
    val startDestination = if (hasToken) Screen.Inbox else Screen.Login

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword) },
                onNavigateToTwoFactor = { email, pass ->
                    navController.navigate(Screen.createTwoFactor(email, pass))
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Inbox) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Inbox) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TwoFactor,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("pass") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val pass = backStackEntry.arguments?.getString("pass") ?: ""
            TwoFactorScreen(
                email = email,
                pass = pass,
                onSuccess = {
                    navController.navigate(Screen.Inbox) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Inbox) {
            val context = androidx.compose.ui.platform.LocalContext.current
            InboxScreen(
                onMailClick = { mailId -> navController.navigate(Screen.createReader(mailId)) },
                onComposeClick = { navController.navigate(Screen.createCompose()) },
                onSettingsClick = { navController.navigate(Screen.Settings) },
                onSupportClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/m/cvuQusUtYzZk"))
                    context.startActivity(intent)
                }
            )
        }

        composable(
            route = Screen.Reader,
            arguments = listOf(navArgument("mailId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mailId = backStackEntry.arguments?.getLong("mailId") ?: 0L
            ReaderScreen(
                mailId = mailId,
                onNavigateBack = { navController.popBackStack() },
                onReplyClick = { to, subject, quotedBody ->
                    navController.navigate(Screen.createCompose(to = to, subject = subject, body = quotedBody))
                }
            )
        }

        composable(
            route = Screen.Compose,
            arguments = listOf(
                navArgument("to") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("subject") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("body") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val to = backStackEntry.arguments?.getString("to") ?: ""
            val subject = backStackEntry.arguments?.getString("subject") ?: ""
            val body = backStackEntry.arguments?.getString("body") ?: ""

            ComposeScreen(
                initialTo = to,
                initialSubject = subject,
                initialBody = body,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Screen.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

    }
}
