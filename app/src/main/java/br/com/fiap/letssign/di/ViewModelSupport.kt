package br.com.fiap.letssign.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer não foi fornecido")
}

/** Cria o ViewModel da tela com as dependências do AppContainer. */
@Composable
inline fun <reified VM : ViewModel> seloViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = LocalAppContainer.current
    return viewModel(
        factory = viewModelFactory {
            initializer { create(container) }
        },
    )
}
