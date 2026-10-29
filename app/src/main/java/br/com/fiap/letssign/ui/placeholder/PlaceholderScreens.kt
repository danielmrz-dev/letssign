package br.com.fiap.letssign.ui.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.R
import br.com.fiap.letssign.data.model.UserRole
import br.com.fiap.letssign.data.session.SessionManager
import kotlinx.coroutines.launch

// TODO: Substituir por tela de Login real quando for implementada
@Composable
fun LoginPlaceholderScreen(
    sessionManager: SessionManager,
    onNavigateToSplash: () -> Unit
) {
    PlaceholderBaseScreen(
        title = stringResource(R.string.placeholder_login_title),
        sessionManager = sessionManager,
        onNavigateToSplash = onNavigateToSplash
    )
}

// TODO: Substituir por tela de Home Admin real quando for implementada
@Composable
fun AdminHomePlaceholderScreen(
    sessionManager: SessionManager,
    onNavigateToSplash: () -> Unit
) {
    PlaceholderBaseScreen(
        title = stringResource(R.string.placeholder_admin_home_title),
        sessionManager = sessionManager,
        onNavigateToSplash = onNavigateToSplash
    )
}

// TODO: Substituir por tela de Home Signatário real quando for implementada
@Composable
fun SignerHomePlaceholderScreen(
    sessionManager: SessionManager,
    onNavigateToSplash: () -> Unit
) {
    PlaceholderBaseScreen(
        title = stringResource(R.string.placeholder_signer_home_title),
        sessionManager = sessionManager,
        onNavigateToSplash = onNavigateToSplash
    )
}

/**
 * Componente base para exibições placeholder simples das rotas de destino da Splash.
 */
@Composable
private fun PlaceholderBaseScreen(
    title: String,
    sessionManager: SessionManager,
    onNavigateToSplash: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val session by sessionManager.sessionFlow.collectAsState(initial = null)

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            val sessionInfoText = if (session?.isValid == true) {
                stringResource(R.string.placeholder_session_info, "${session?.userId} (${session?.role?.name})")
            } else {
                stringResource(R.string.placeholder_no_session)
            }

            Text(
                text = sessionInfoText,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        sessionManager.saveSession("admin_user_01", UserRole.ADMIN)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.placeholder_btn_login_as_admin))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        sessionManager.saveSession("signer_user_01", UserRole.SIGNATARIO)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.placeholder_btn_login_as_signer))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        sessionManager.clearSession()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.placeholder_btn_clear_session))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNavigateToSplash,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.placeholder_btn_back_to_splash))
            }
        }
    }
}
