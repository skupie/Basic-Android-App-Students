package com.basic.studentportal.data.api

import com.basic.studentportal.data.local.TokenDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore,
    private val authEventBus: AuthEventBus
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenDataStore.getToken().first() }
        val request = chain.request().newBuilder().apply {
            addHeader("Accept", "application/json")
            addHeader("Content-Type", "application/json")
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()

        val response = chain.proceed(request)

        // If the server returns 401, the token was revoked (e.g. user logged in
        // on another device). Clear local credentials and broadcast the event
        // so MainActivity can redirect to LoginActivity immediately.
        if (response.code == 401) {
            runBlocking { tokenDataStore.clearAll() }
            authEventBus.sendUnauthorizedEvent()
        }

        return response
    }
}
