package com.voyagerfiles.data.remote.cloud

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.voyagerfiles.BuildConfig
import com.voyagerfiles.security.AndroidCredentialCipher
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Public-client authorization code + PKCE in the system browser. No embedded secrets. */
class CloudOAuth(context: Context, val cloud: CloudService) {
    private val prefs = context.getSharedPreferences("finderdex_cloud_auth", Context.MODE_PRIVATE)
    private val cipher = AndroidCredentialCipher("finderdex_cloud_oauth_v1")
    private val service = AuthorizationService(context)
    private var state = runCatching {
        prefs.getString(cloud.name, null)?.let { AuthState.jsonDeserialize(cipher.decrypt(it)) }
    }.getOrNull() ?: AuthState()
    private var generation = 0
    val configured: Boolean get() = clientId.isNotBlank()
    val authorized: Boolean get() = state.isAuthorized
    private val clientId get() = when (cloud) {
        CloudService.GOOGLE_DRIVE -> BuildConfig.DRIVE_CLIENT_ID
        CloudService.ONEDRIVE -> BuildConfig.ONEDRIVE_CLIENT_ID
    }
    fun authorizationIntent(): Intent {
        check(configured) { "BLOCKED_EXTERNAL: OAuth application registration required" }
        val microsoft = cloud == CloudService.ONEDRIVE
        val config = AuthorizationServiceConfiguration(
            Uri.parse(if (microsoft) "https://login.microsoftonline.com/common/oauth2/v2.0/authorize" else "https://accounts.google.com/o/oauth2/v2/auth"),
            Uri.parse(if (microsoft) "https://login.microsoftonline.com/common/oauth2/v2.0/token" else "https://oauth2.googleapis.com/token"),
        )
        val request = AuthorizationRequest.Builder(config, clientId, ResponseTypeValues.CODE,
            Uri.parse(BuildConfig.OAUTH_REDIRECT))
            .setScope(if (microsoft) "openid offline_access Files.ReadWrite" else "https://www.googleapis.com/auth/drive")
            .setPrompt("select_account")
            .apply { if (!microsoft) setAdditionalParameters(mapOf("access_type" to "offline")) }
            .build() // AppAuth generates state and a fresh S256 PKCE verifier.
        return service.getAuthorizationRequestIntent(request)
    }
    suspend fun complete(intent: Intent) = suspendCancellableCoroutine<Unit> { continuation ->
        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)
        if (response == null || error != null) {
            continuation.resumeWithException(error ?: IllegalStateException("Authorization cancelled"))
        } else {
            val epoch = generation
            continuation.invokeOnCancellation { generation++ }
            val next = AuthState(response, null)
            service.performTokenRequest(response.createTokenExchangeRequest()) { token, failure ->
                if (epoch == generation && token != null && failure == null) {
                    next.update(token, null)
                    runCatching { state = next; persist() }.fold(
                        onSuccess = { if (continuation.isActive) continuation.resume(Unit) },
                        onFailure = { disconnect(); if (continuation.isActive) continuation.resumeWithException(it) })
                } else if (continuation.isActive) continuation.resumeWithException(
                    failure ?: IllegalStateException("Authorization cancelled"))
            }
        }
    }
    suspend fun token(): String? = suspendCancellableCoroutine { continuation ->
        if (!state.isAuthorized) continuation.resume(null)
        else {
            val epoch = generation
            state.performActionWithFreshTokens(service) { access, _, failure ->
                val storageError = if (epoch == generation) runCatching { persist() }.exceptionOrNull() else null
                if (continuation.isActive) {
                    if (storageError != null) { continuation.resumeWithException(storageError); return@performActionWithFreshTokens }
                    if (failure != null) continuation.resumeWithException(failure)
                    else continuation.resume(access.takeIf { epoch == generation })
                }
            }
        }
    }
    private fun persist() { prefs.edit().putString(cloud.name, cipher.encrypt(state.jsonSerializeString())).apply() }
    fun disconnect() { generation++; state = AuthState(); prefs.edit().remove(cloud.name).apply() }
    fun dispose() { service.dispose() }
}
