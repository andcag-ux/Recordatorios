package com.example.recordatorios.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.AppContainer
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/** Conecta el AuthViewModel con la pantalla de acceso. */
@Composable
fun AuthRoute(
    aviso: String?,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(
        factory = viewModelFactory { initializer { AuthViewModel(AppContainer.auth) } }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AuthScreen(
        uiState = uiState,
        aviso = aviso,
        onCambiarModo = viewModel::cambiarModo,
        onEmail = viewModel::cambiarEmail,
        onContrasena = viewModel::cambiarContrasena,
        onConfirmacion = viewModel::cambiarConfirmacion,
        onEnviar = viewModel::enviar,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    aviso: String?,
    onCambiarModo: (ModoAcceso) -> Unit,
    onEmail: (String) -> Unit,
    onContrasena: (String) -> Unit,
    onConfirmacion: (String) -> Unit,
    onEnviar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val esRegistro = uiState.modo == ModoAcceso.Registro
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Recordatorios") }) }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                if (esRegistro) "Creá tu cuenta" else "Iniciá sesión",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Tus recordatorios se guardan en el dispositivo y se sincronizan con tu cuenta.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Motivo de un cierre de sesión que la persona no pidió (sesión vencida, cuenta deshabilitada).
            if (aviso != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Text(aviso, modifier = Modifier.padding(12.dp))
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ModoAcceso.entries.forEachIndexed { i, modo ->
                    SegmentedButton(
                        selected = uiState.modo == modo,
                        onClick = { onCambiarModo(modo) },
                        enabled = !uiState.enCurso,
                        shape = SegmentedButtonDefaults.itemShape(i, ModoAcceso.entries.size)
                    ) {
                        Text(if (modo == ModoAcceso.IniciarSesion) "Ingresar" else "Registrarme")
                    }
                }
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmail,
                label = { Text("Correo electrónico") },
                singleLine = true,
                enabled = !uiState.enCurso,
                isError = uiState.errorEmail != null,
                supportingText = uiState.errorEmail?.let { msg -> @Composable { Text(msg) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.contrasena,
                onValueChange = onContrasena,
                label = { Text("Contraseña") },
                singleLine = true,
                enabled = !uiState.enCurso,
                isError = uiState.errorContrasena != null,
                supportingText = uiState.errorContrasena?.let { msg -> @Composable { Text(msg) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (esRegistro) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onEnviar() }),
                modifier = Modifier.fillMaxWidth()
            )
            if (esRegistro) {
                OutlinedTextField(
                    value = uiState.confirmacion,
                    onValueChange = onConfirmacion,
                    label = { Text("Repetir contraseña") },
                    singleLine = true,
                    enabled = !uiState.enCurso,
                    isError = uiState.errorConfirmacion != null,
                    supportingText = uiState.errorConfirmacion?.let { msg -> @Composable { Text(msg) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onEnviar() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.errorGeneral != null) {
                Text(
                    uiState.errorGeneral,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }

            Button(
                onClick = onEnviar,
                enabled = !uiState.enCurso,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.enCurso) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Text(if (esRegistro) "Crear cuenta" else "Ingresar")
                }
            }
            if (uiState.enCurso) {
                Text(
                    if (esRegistro) "Creando la cuenta…" else "Verificando tus datos…",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    RecordatoriosTheme {
        AuthScreen(
            uiState = AuthUiState(email = "ana@ejemplo.com", errorGeneral = "Correo o contraseña incorrectos."),
            aviso = "La sesión venció. Iniciá sesión nuevamente.",
            onCambiarModo = {}, onEmail = {}, onContrasena = {}, onConfirmacion = {}, onEnviar = {}
        )
    }
}
