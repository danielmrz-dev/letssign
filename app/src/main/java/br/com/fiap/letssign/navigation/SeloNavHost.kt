package br.com.fiap.letssign.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.fiap.letssign.core.model.DocumentStatus
import br.com.fiap.letssign.core.model.SignerStatus
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.model.UserRole
import br.com.fiap.letssign.feature.auth.ui.LoginScreen
import br.com.fiap.letssign.feature.auth.ui.SplashScreen
import br.com.fiap.letssign.feature.documents.ui.home.AdminHomeScreen
import br.com.fiap.letssign.feature.documents.ui.wizard.DocumentWizardScreen
import br.com.fiap.letssign.feature.signers.ui.SignerFormScreen
import br.com.fiap.letssign.feature.signers.ui.SignersScreen
import br.com.fiap.letssign.feature.signing.ui.SignDocumentScreen
import br.com.fiap.letssign.feature.signing.ui.SignatureCaptureScreen
import br.com.fiap.letssign.feature.signing.ui.SignerHomeScreen
import br.com.fiap.letssign.feature.tracking.ui.TrackingScreen
import br.com.fiap.letssign.feature.viewer.PdfViewerScreen
import br.com.fiap.letssign.feature.viewer.ViewerKind

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val ADMIN_HOME = "admin/home"
    const val SIGNERS = "admin/signers"
    const val SIGNER_FORM = "admin/signers/form?signerId={signerId}"
    const val WIZARD = "admin/wizard?documentId={documentId}"
    const val TRACKING = "admin/tracking/{documentId}"
    const val VIEWER = "viewer/{kind}/{id}?mode={mode}"
    const val SIGNER_HOME = "signer/home"
    const val SIGN = "signer/sign/{linkId}"
    const val CAPTURE = "signer/capture/{linkId}"

    fun signerForm(signerId: String?) = if (signerId == null) "admin/signers/form" else "admin/signers/form?signerId=$signerId"
    fun wizard(documentId: String?) = if (documentId == null) "admin/wizard" else "admin/wizard?documentId=$documentId"
    fun tracking(documentId: String) = "admin/tracking/$documentId"
    fun viewer(kind: ViewerKind, id: String, mode: String = "final") = "viewer/${kind.name}/$id?mode=$mode"
    fun sign(linkId: String) = "signer/sign/$linkId"
    fun capture(linkId: String) = "signer/capture/$linkId"

    fun homeFor(user: User) = if (user.role == UserRole.ADMIN) ADMIN_HOME else SIGNER_HOME
}

const val SIGNATURE_RESULT_KEY = "signature_path"

@Composable
fun SeloNavHost(navController: NavHostController = rememberNavController()) {
    fun resetTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onNavigate = { user -> resetTo(if (user == null) Routes.LOGIN else Routes.homeFor(user)) })
        }

        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = { user -> resetTo(Routes.homeFor(user)) })
        }

        // ---------------- Admin ----------------
        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(
                onNewDocument = { navController.navigate(Routes.wizard(null)) },
                onOpenDocument = { summary ->
                    if (summary.status == DocumentStatus.DRAFT) {
                        navController.navigate(Routes.wizard(summary.id))
                    } else {
                        navController.navigate(Routes.tracking(summary.id))
                    }
                },
                onOpenSigners = { navController.navigate(Routes.SIGNERS) },
                onLoggedOut = { resetTo(Routes.LOGIN) },
            )
        }

        composable(Routes.SIGNERS) {
            SignersScreen(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.signerForm(null)) },
                onEdit = { id -> navController.navigate(Routes.signerForm(id)) },
            )
        }

        composable(
            route = Routes.SIGNER_FORM,
            arguments = listOf(navArgument("signerId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            SignerFormScreen(
                signerId = entry.arguments?.getString("signerId"),
                onDone = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.WIZARD,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            DocumentWizardScreen(
                documentId = entry.arguments?.getString("documentId"),
                onExit = { navController.popBackStack() },
                onOpenTracking = { id ->
                    navController.navigate(Routes.tracking(id)) {
                        popUpTo(Routes.ADMIN_HOME)
                    }
                },
                onGoHome = { navController.popBackStack(Routes.ADMIN_HOME, inclusive = false) },
                onPreview = { id -> navController.navigate(Routes.viewer(ViewerKind.OWNER, id, mode = "preview")) },
                onRegisterSigner = { navController.navigate(Routes.signerForm(null)) },
            )
        }

        composable(Routes.TRACKING) { entry ->
            val documentId = entry.arguments?.getString("documentId").orEmpty()
            TrackingScreen(
                documentId = documentId,
                onBack = { navController.popBackStack() },
                onOpenPdf = { navController.navigate(Routes.viewer(ViewerKind.OWNER, documentId)) },
                onEditNewVersion = {
                    navController.navigate(Routes.wizard(documentId)) {
                        popUpTo(Routes.ADMIN_HOME)
                    }
                },
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType; defaultValue = "final" },
            ),
        ) { entry ->
            PdfViewerScreen(
                kind = ViewerKind.from(entry.arguments?.getString("kind")),
                id = entry.arguments?.getString("id").orEmpty(),
                preview = entry.arguments?.getString("mode") == "preview",
                onBack = { navController.popBackStack() },
            )
        }

        // ---------------- Signatário ----------------
        composable(Routes.SIGNER_HOME) {
            SignerHomeScreen(
                onOpen = { received ->
                    if (received.status == SignerStatus.PENDING) {
                        navController.navigate(Routes.sign(received.signerLinkId))
                    } else {
                        navController.navigate(Routes.viewer(ViewerKind.SIGNER, received.signerLinkId))
                    }
                },
                onLoggedOut = { resetTo(Routes.LOGIN) },
            )
        }

        composable(Routes.SIGN) { entry ->
            val linkId = entry.arguments?.getString("linkId").orEmpty()
            val signaturePath by entry.savedStateHandle
                .getStateFlow<String?>(SIGNATURE_RESULT_KEY, null)
                .collectAsStateWithLifecycle()
            SignDocumentScreen(
                linkId = linkId,
                signaturePath = signaturePath,
                onBack = { navController.popBackStack() },
                onCaptureSignature = { navController.navigate(Routes.capture(linkId)) },
            )
        }

        composable(Routes.CAPTURE) { entry ->
            SignatureCaptureScreen(
                linkId = entry.arguments?.getString("linkId").orEmpty(),
                onCancel = { navController.popBackStack() },
                onConfirmed = { path ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(SIGNATURE_RESULT_KEY, path)
                    navController.popBackStack()
                },
            )
        }
    }
}
