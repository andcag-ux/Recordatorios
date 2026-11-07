package com.example.recordatorios.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Autenticación con Firebase (correo y contraseña).
 *
 * - La sesión la persiste el SDK de Firebase: al reabrir la app sigue activa.
 *   La app NO guarda contraseñas ni tokens propios (ni en DataStore ni en Room).
 * - Al iniciar, si hay sesión guardada se verifica contra el servicio; si la
 *   cuenta fue deshabilitada o borrada, la sesión se cierra con un motivo.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val scope: CoroutineScope
) : AuthRepository {

    private val _estado = MutableStateFlow<EstadoAutenticacion>(EstadoAutenticacion.Verificando)
    override val estado: StateFlow<EstadoAutenticacion> = _estado.asStateFlow()

    /**
     * Motivo del último cierre de sesión que no pidió la persona. Se conserva
     * hasta el próximo ingreso o cierre voluntario: el listener de Firebase puede
     * dispararse más de una vez y todas esas veces debe mostrar el mismo aviso.
     */
    @Volatile
    private var motivoCierre: String? = null

    /** true solo cuando la persona eligió "Cerrar sesión". */
    @Volatile
    private var cierreVoluntario = false

    /** Si había una sesión (guardada o iniciada) que todavía no se cerró. */
    @Volatile
    private var habiaSesion = auth.currentUser != null

    init {
        // El listener se llama al registrarse y en cada cambio de sesión.
        auth.addAuthStateListener { fa ->
            val usuario = fa.currentUser
            _estado.value = if (usuario != null) {
                habiaSesion = true
                EstadoAutenticacion.Activa(usuario.aSesion())
            } else {
                // La sesión desapareció sin que la persona la cerrara: el propio SDK
                // de Firebase la invalida si al renovar el token la cuenta está
                // deshabilitada, eliminada o con la sesión revocada.
                if (habiaSesion && !cierreVoluntario && motivoCierre == null) {
                    motivoCierre = "Tu sesión ya no es válida (la cuenta pudo ser deshabilitada " +
                        "o eliminada). Iniciá sesión nuevamente."
                }
                habiaSesion = false
                EstadoAutenticacion.SinSesion(motivoCierre)
            }
        }
        if (auth.currentUser != null) scope.launch { verificarSesion() }
    }

    override suspend fun registrar(email: String, contrasena: String): ResultadoAuth = ejecutar {
        auth.createUserWithEmailAndPassword(email.trim(), contrasena).await()
    }

    override suspend fun iniciarSesion(email: String, contrasena: String): ResultadoAuth = ejecutar {
        auth.signInWithEmailAndPassword(email.trim(), contrasena).await()
    }

    override fun cerrarSesion() {
        motivoCierre = null
        cierreVoluntario = true
        auth.signOut()
    }

    override suspend fun verificarSesion() {
        val usuario = auth.currentUser ?: return
        try {
            usuario.reload().await()
            // Fuerza a renovar el token: falla si la sesión fue revocada.
            usuario.getIdToken(true).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            invalidar(mensajeErrorAuth(e.errorCode))
        } catch (e: FirebaseAuthException) {
            if (e.errorCode == "ERROR_USER_TOKEN_EXPIRED" || e.errorCode == "ERROR_INVALID_USER_TOKEN") {
                invalidar(mensajeErrorAuth(e.errorCode))
            }
        } catch (e: FirebaseNetworkException) {
            // Sin red no se puede verificar: se conserva la sesión y se trabaja offline.
        } catch (e: Exception) {
            // Otro error transitorio: se conserva la sesión.
        }
    }

    /** Cierra la sesión dejando registrado el motivo para mostrarlo en la pantalla de acceso. */
    private fun invalidar(motivo: String) {
        motivoCierre = motivo
        auth.signOut()
    }

    private suspend fun ejecutar(operacion: suspend () -> Unit): ResultadoAuth =
        try {
            motivoCierre = null // un ingreso nuevo descarta el aviso anterior
            cierreVoluntario = false
            operacion()
            ResultadoAuth.Exito
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthException) {
            ResultadoAuth.Error(mensajeErrorAuth(e.errorCode))
        } catch (e: FirebaseNetworkException) {
            ResultadoAuth.Error(mensajeErrorAuth("SIN_CONEXION"))
        } catch (e: Exception) {
            val codigo = if (e.javaClass.simpleName.contains("TooManyRequests")) "ERROR_TOO_MANY_REQUESTS" else null
            ResultadoAuth.Error(mensajeErrorAuth(codigo))
        }

    private fun FirebaseUser.aSesion() = SesionUsuario(uid = uid, email = email)
}
