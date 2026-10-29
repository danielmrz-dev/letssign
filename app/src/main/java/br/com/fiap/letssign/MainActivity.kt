package br.com.fiap.letssign

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.fiap.letssign.core.security.PasswordHasherImpl
import br.com.fiap.letssign.data.local.database.AppDatabase
import br.com.fiap.letssign.data.repository.UserRepositoryImpl
import br.com.fiap.letssign.data.session.SessionManager
import br.com.fiap.letssign.domain.usecase.AuthenticateUserUseCase
import br.com.fiap.letssign.ui.login.LoginScreen
import br.com.fiap.letssign.ui.login.LoginViewModel
import br.com.fiap.letssign.ui.login.LoginViewModelFactory
import br.com.fiap.letssign.ui.placeholder.AdminHomePlaceholderScreen
import br.com.fiap.letssign.ui.placeholder.SignerHomePlaceholderScreen
import br.com.fiap.letssign.ui.splash.SplashScreen
import br.com.fiap.letssign.ui.splash.SplashViewModel
import br.com.fiap.letssign.ui.splash.SplashViewModelFactory
import br.com.fiap.letssign.ui.theme.LetsSignTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LetsSignTheme {
                LetsSignApp()
            }
        }
    }
}

/**
 * Definição das rotas de navegação do aplicativo selo.
 */
object AppRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val ADMIN_HOME = "admin_home"
    const val SIGNER_HOME = "signer_home"
}

@Composable
fun LetsSignApp() {
    val appContext = LocalContext.current.applicationContext
    val sessionManager = remember { SessionManager(appContext) }
    val passwordHasher = remember { PasswordHasherImpl() }
    val database = remember { AppDatabase.getInstance(appContext) }
    val userRepository = remember { UserRepositoryImpl(database.userDao(), passwordHasher) }
    val authenticateUserUseCase = remember { AuthenticateUserUseCase(userRepository, passwordHasher) }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.SPLASH
    ) {
        composable(AppRoutes.SPLASH) {
            val splashViewModel: SplashViewModel = viewModel(
                factory = SplashViewModelFactory(sessionManager)
            )

            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToLogin = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToAdminHome = {
                    navController.navigate(AppRoutes.ADMIN_HOME) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToSignerHome = {
                    navController.navigate(AppRoutes.SIGNER_HOME) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Rota da Tela 02 (Login) do aplicativo
        composable(AppRoutes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModelFactory(authenticateUserUseCase, sessionManager)
            )

            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToAdminHome = {
                    navController.navigate(AppRoutes.ADMIN_HOME) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignerHome = {
                    navController.navigate(AppRoutes.SIGNER_HOME) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // TODO: Substituir por rota da tela de Home Admin real quando for desenvolvida
        composable(AppRoutes.ADMIN_HOME) {
            AdminHomePlaceholderScreen(
                sessionManager = sessionManager,
                onNavigateToSplash = {
                    navController.navigate(AppRoutes.SPLASH) {
                        popUpTo(AppRoutes.ADMIN_HOME) { inclusive = true }
                    }
                }
            )
        }

        // TODO: Substituir por rota da tela de Home Signatário real quando for desenvolvida
        composable(AppRoutes.SIGNER_HOME) {
            SignerHomePlaceholderScreen(
                sessionManager = sessionManager,
                onNavigateToSplash = {
                    navController.navigate(AppRoutes.SPLASH) {
                        popUpTo(AppRoutes.SIGNER_HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
