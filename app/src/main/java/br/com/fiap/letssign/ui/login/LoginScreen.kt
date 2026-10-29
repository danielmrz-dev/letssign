package br.com.fiap.letssign.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.BuildConfig
import br.com.fiap.letssign.R
import br.com.fiap.letssign.data.model.UserRole
import br.com.fiap.letssign.ui.theme.LetsSignTheme
import br.com.fiap.letssign.ui.theme.LoginBorderLilac
import br.com.fiap.letssign.ui.theme.LoginButtonDisabled
import br.com.fiap.letssign.ui.theme.LoginDemoBg
import br.com.fiap.letssign.ui.theme.LoginDemoText
import br.com.fiap.letssign.ui.theme.SplashDotPurple
import br.com.fiap.letssign.ui.theme.SplashGradientBottom
import br.com.fiap.letssign.ui.theme.SplashGradientMiddle
import br.com.fiap.letssign.ui.theme.SplashGradientTop
import br.com.fiap.letssign.ui.theme.SplashIconPurple
import br.com.fiap.letssign.ui.theme.SplashShapeOverlay

/**
 * Composable stateful da LoginScreen. Observa o ViewModel e gerencia navegação.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToAdminHome: () -> Unit,
    onNavigateToSignerHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToHome -> {
                    if (event.role == UserRole.ADMIN) {
                        onNavigateToAdminHome()
                    } else {
                        onNavigateToSignerHome()
                    }
                }
            }
        }
    }

    LoginScreenContent(
        uiState = uiState,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onLoginClicked = viewModel::onLoginClicked,
        onFillDemoCredentials = viewModel::onFillDemoCredentials
    )
}

/**
 * Layout e visual stateless da Tela de Login.
 */
@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClicked: () -> Unit,
    onFillDemoCredentials: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SplashGradientTop,
                        SplashGradientMiddle,
                        SplashGradientBottom
                    )
                )
            )
            .clipToBounds()
    ) {
        // Formas decorativas no topo do cabeçalho
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-30).dp)
                .rotate(30f)
                .size(180.dp)
                .background(
                    color = SplashShapeOverlay,
                    shape = RoundedCornerShape(32.dp)
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Cabeçalho (cerca de 30% da altura da tela)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Wordmark "selo."
                val annotatedWordmark = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.White)) {
                        append(stringResource(R.string.splash_wordmark_main))
                    }
                    withStyle(style = SpanStyle(color = SplashDotPurple)) {
                        append(stringResource(R.string.splash_wordmark_dot))
                    }
                }

                Text(
                    text = annotatedWordmark,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Título
                Text(
                    text = stringResource(R.string.login_header_title),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtítulo
                Text(
                    text = stringResource(R.string.login_header_subtitle),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Painel branco deslizante/overlay ocupando o resto da tela
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .navigationBarsPadding()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Seção de formulários
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Campo E-mail
                        Text(
                            text = stringResource(R.string.login_label_email),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val isEmailError = uiState.emailError != null
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = onEmailChanged,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.login_placeholder_email),
                                    color = Color.Gray.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            isError = isEmailError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = LoginBorderLilac,
                                focusedBorderColor = SplashIconPurple,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )

                        if (isEmailError) {
                            Text(
                                text = stringResource(R.string.login_error_invalid_email),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Campo Senha
                        Text(
                            text = stringResource(R.string.login_label_password),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val isPasswordError = uiState.passwordError != null
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = onPasswordChanged,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.login_placeholder_password),
                                    color = Color.Gray.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            isError = isPasswordError,
                            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val icon = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                                val description = if (uiState.isPasswordVisible) {
                                    stringResource(R.string.login_cd_hide_password)
                                } else {
                                    stringResource(R.string.login_cd_show_password)
                                }
                                IconButton(onClick = onTogglePasswordVisibility) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = description,
                                        tint = Color.Gray
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { onLoginClicked() }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = LoginBorderLilac,
                                focusedBorderColor = SplashIconPurple,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )

                        if (isPasswordError) {
                            Text(
                                text = stringResource(R.string.login_error_empty_password),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Erro genérico de credencial
                        if (uiState.loginError != null) {
                            Text(
                                text = stringResource(R.string.login_error_invalid_credentials),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }

                        // Botão Entrar
                        Button(
                            onClick = onLoginClicked,
                            enabled = uiState.isSubmitEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SplashIconPurple,
                                disabledContainerColor = LoginButtonDisabled,
                                contentColor = Color.White,
                                disabledContentColor = Color.White.copy(alpha = 0.7f)
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.login_btn_submit),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Painel de Demonstração (visível apenas em compilação Debug)
                    if (BuildConfig.DEBUG) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = LoginDemoBg,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.login_demo_title),
                                color = LoginDemoText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Chip Admin
                                DemoChip(
                                    label = stringResource(R.string.login_demo_chip_admin),
                                    icon = Icons.Default.Shield,
                                    iconDescription = stringResource(R.string.login_cd_admin_icon),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onFillDemoCredentials("admin@letssign.com", "admin1234")
                                    }
                                )

                                // Chip Signatário (Maria)
                                DemoChip(
                                    label = stringResource(R.string.login_demo_chip_signer),
                                    icon = Icons.Default.Person,
                                    iconDescription = stringResource(R.string.login_cd_signer_icon),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onFillDemoCredentials("maria@studionorte.com.br", "maria1234")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chip customizado para seleção rápida de credenciais de demonstração.
 */
@Composable
private fun DemoChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .background(color = Color.White, shape = RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = LoginBorderLilac, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconDescription,
            tint = SplashIconPurple,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenNormalPreview() {
    LetsSignTheme {
        LoginScreenContent(
            uiState = LoginUiState(
                email = "admin@letssign.com",
                password = "••••••••"
            ),
            onEmailChanged = {},
            onPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onLoginClicked = {},
            onFillDemoCredentials = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenErrorPreview() {
    LetsSignTheme {
        LoginScreenContent(
            uiState = LoginUiState(
                email = "admin@letssign.com",
                password = "wrongpassword",
                loginError = "invalid_credentials"
            ),
            onEmailChanged = {},
            onPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onLoginClicked = {},
            onFillDemoCredentials = { _, _ -> }
        )
    }
}
