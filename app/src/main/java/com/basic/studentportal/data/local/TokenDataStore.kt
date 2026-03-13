package com.basic.studentportal.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "student_portal_prefs")

@Singleton
class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val PROFILE_PHOTO_KEY = stringPreferencesKey("profile_photo_url")
    }

    fun getToken(): Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    fun getUserName(): Flow<String?> = context.dataStore.data.map { it[USER_NAME_KEY] }
    fun getUserEmail(): Flow<String?> = context.dataStore.data.map { it[USER_EMAIL_KEY] }
    fun getProfilePhotoUrl(): Flow<String?> = context.dataStore.data.map { it[PROFILE_PHOTO_KEY] }

    suspend fun saveAuthData(token: String, name: String, email: String, role: String, photoUrl: String?) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_NAME_KEY] = name
            prefs[USER_EMAIL_KEY] = email
            prefs[USER_ROLE_KEY] = role
            if (photoUrl != null) prefs[PROFILE_PHOTO_KEY] = photoUrl
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    fun isLoggedIn(): Flow<Boolean> = getToken().map { !it.isNullOrBlank() }
}
