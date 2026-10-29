package br.com.fiap.letssign

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import br.com.fiap.letssign.di.LocalAppContainer
import br.com.fiap.letssign.navigation.SeloNavHost
import br.com.fiap.letssign.ui.theme.SeloTheme

/** FragmentActivity é necessária para o BiometricPrompt (confirmação por biometria ou PIN). */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SeloApplication).container
        setContent {
            SeloTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    SeloNavHost()
                }
            }
        }
    }
}
