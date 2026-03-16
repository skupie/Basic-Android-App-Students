package com.basic.studentportal.service

import com.basic.studentportal.data.api.ApiService
import com.basic.studentportal.data.model.FcmTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun sendTokenToServer(token: String) {
        try {
            api.updateFcmToken(FcmTokenRequest(fcmToken = token))
        } catch (e: Exception) {
            // Silently fail — will retry on next login
        }
    }
}
