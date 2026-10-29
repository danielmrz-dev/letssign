package br.com.fiap.letssign.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current != null) {
        if (current is FragmentActivity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}

/**
 * Pede biometria ou o PIN/padrão do aparelho antes de concluir a assinatura.
 * Se o aparelho não tiver nenhum bloqueio configurado, chama [onUnavailable] para a tela
 * pedir uma confirmação manual.
 */
fun requestSignatureConfirmation(
    activity: FragmentActivity,
    onConfirmed: () -> Unit,
    onUnavailable: () -> Unit,
    onError: (String) -> Unit,
) {
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val status = BiometricManager.from(activity).canAuthenticate(authenticators)
    if (status != BiometricManager.BIOMETRIC_SUCCESS) {
        onUnavailable()
        return
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onConfirmed()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                if (!cancelled) onError(errString.toString())
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Confirme sua assinatura")
        .setSubtitle("Use a biometria ou o PIN do aparelho")
        .setAllowedAuthenticators(authenticators)
        .build()
    prompt.authenticate(info)
}
