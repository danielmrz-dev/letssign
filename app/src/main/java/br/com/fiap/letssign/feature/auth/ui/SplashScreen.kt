package br.com.fiap.letssign.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.core.database.DatabaseSeeder
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.di.seloViewModel
import br.com.fiap.letssign.ui.components.SeloAppMark
import br.com.fiap.letssign.ui.components.SeloWordmark
import br.com.fiap.letssign.ui.components.SplashBackground
import br.com.fiap.letssign.ui.theme.SeloFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface SplashState {
    data object Checking : SplashState
    data class Ready(val user: User?) : SplashState
}

class SplashViewModel(
    private val seeder: DatabaseSeeder,
    private val session: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow<SplashState>(SplashState.Checking)
    val state: StateFlow<SplashState> = _state

    init {
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            seeder.ensureSeeded()
            val user = runCatching { session.restore() }.getOrNull()
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < MIN_SPLASH_MS) delay(MIN_SPLASH_MS - elapsed)
            _state.value = SplashState.Ready(user)
        }
    }

    private companion object {
        const val MIN_SPLASH_MS = 900L
    }
}

/** Tela 01 · Verifica a sessão salva e redireciona para a home do perfil ou para o login. */
@Composable
fun SplashScreen(onNavigate: (User?) -> Unit) {
    val viewModel = seloViewModel { SplashViewModel(it.seeder, it.session) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val ready = state as? SplashState.Ready ?: return@LaunchedEffect
        onNavigate(ready.user)
    }

    SplashBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SeloAppMark(size = 88.dp)
            Spacer(Modifier.height(24.dp))
            SeloWordmark(fontSize = 46.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Assinatura eletrônica no seu bolso",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Verificando sessão…", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
            Text(
                "em parceria com LetsSign",
                color = Color.White.copy(alpha = 0.65f),
                fontFamily = SeloFonts.Mono,
                fontSize = 12.sp,
            )
        }
    }
}
