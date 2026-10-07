package com.sadique.dailyledger.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.sadique.dailyledger.auth.UserProfile
import com.sadique.dailyledger.security.SecureSecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("daily_ledger_settings")

data class CloudState(val ready: Boolean = false, val revision: String = "")
data class AppSettings(
    val user: UserProfile?,
    val theme: String,
    val biometric: Boolean,
    val driveEnabled: Boolean,
    val lastSync: Long,
    val cloudEnabled: Boolean = true,
    val lastCloudSync: Long = 0,
    val cloudStatus: String = "Your backup will be checked when you are online.",
    val weatherEnabled: Boolean = false,
    val weatherCity: String = "",
    val weatherTemperature: String = "",
    val weatherCondition: String = "",
    val weatherUpdatedAt: Long = 0,
    val transactionSounds: Boolean = false,
)

class SettingsStore(private val context: Context) {
    private object K {
        val userId = stringPreferencesKey("user_id")
        val email = stringPreferencesKey("email")
        val name = stringPreferencesKey("name")
        val photo = stringPreferencesKey("photo")
        val googleEmail = stringPreferencesKey("google_email")
        val theme = stringPreferencesKey("theme")
        val biometric = booleanPreferencesKey("biometric")
        val oldDrive = booleanPreferencesKey("drive_enabled")
        val oldSync = longPreferencesKey("last_sync")
        val weatherEnabled = booleanPreferencesKey("weather_enabled")
        val weatherCity = stringPreferencesKey("weather_city")
        val weatherTemperature = stringPreferencesKey("weather_temperature")
        val weatherCondition = stringPreferencesKey("weather_condition")
        val weatherUpdatedAt = longPreferencesKey("weather_updated_at")
        val transactionSounds = booleanPreferencesKey("transaction_sounds")
    }
    private fun drive(owner: String) = booleanPreferencesKey("drive:$owner")
    private fun driveTime(owner: String) = longPreferencesKey("drive_time:$owner")
    private fun cloud(owner: String) = booleanPreferencesKey("cloud:$owner")
    private fun cloudTime(owner: String) = longPreferencesKey("cloud_time:$owner")
    private fun cloudMessage(owner: String) = stringPreferencesKey("cloud_message:$owner")
    private fun ready(owner: String) = booleanPreferencesKey("cloud_ready:$owner")
    private fun revision(owner: String) = stringPreferencesKey("cloud_revision:$owner")
    private fun skippedRestoreRevisionKey(owner: String) = stringPreferencesKey("cloud_restore_skipped:$owner")
    private fun profile(p: Preferences) = p[K.userId]?.let {
        UserProfile(it, p[K.email].orEmpty(), p[K.name].orEmpty(), p[K.photo], p[K.googleEmail])
    }
    val all: Flow<AppSettings> = context.dataStore.data.map { p ->
        val id = p[K.userId].orEmpty()
        AppSettings(
            user = profile(p),
            theme = p[K.theme] ?: "SYSTEM",
            biometric = p[K.biometric] ?: false,
            driveEnabled = p[drive(id)] ?: false,
            lastSync = p[driveTime(id)] ?: 0L,
            cloudEnabled = p[cloud(id)] ?: true,
            lastCloudSync = p[cloudTime(id)] ?: 0L,
            cloudStatus = p[cloudMessage(id)] ?: "Your backup will be checked when you are online.",
            weatherEnabled = p[K.weatherEnabled] ?: false,
            weatherCity = p[K.weatherCity].orEmpty(),
            weatherTemperature = p[K.weatherTemperature].orEmpty(),
            weatherCondition = p[K.weatherCondition].orEmpty(),
            weatherUpdatedAt = p[K.weatherUpdatedAt] ?: 0L,
            transactionSounds = p[K.transactionSounds] ?: false,
        )
    }
    val user = all.map { it.user }
    val theme = all.map { it.theme }
    val biometric = all.map { it.biometric }
    val driveEnabled = all.map { it.driveEnabled }
    val lastSync = all.map { it.lastSync }
    suspend fun saveUser(u: UserProfile) {
        context.dataStore.edit { p ->
            p[K.userId] = u.id; p[K.email] = u.email; p[K.name] = u.name
            if (u.photoUrl != null) p[K.photo] = u.photoUrl else p.remove(K.photo)
            if (u.googleEmail != null) p[K.googleEmail] = u.googleEmail else p.remove(K.googleEmail)
        }
    }
    suspend fun clearUser() {
        context.dataStore.edit {
            it.remove(K.userId); it.remove(K.email); it.remove(K.name); it.remove(K.photo); it.remove(K.googleEmail)
        }
    }
    suspend fun setTheme(value: String) { context.dataStore.edit { it[K.theme] = value } }
    suspend fun setBiometric(value: Boolean) { context.dataStore.edit { it[K.biometric] = value } }
    suspend fun setDriveEnabled(owner: String, value: Boolean) { context.dataStore.edit { it[drive(owner)] = value } }
    suspend fun setLastSync(owner: String, value: Long) { context.dataStore.edit { it[driveTime(owner)] = value } }
    suspend fun setCloudEnabled(owner: String, value: Boolean) { context.dataStore.edit { it[cloud(owner)] = value } }
    suspend fun setWeatherEnabled(value: Boolean) { context.dataStore.edit { it[K.weatherEnabled] = value } }
    suspend fun setWeatherCity(value: String) {
        context.dataStore.edit {
            it[K.weatherCity] = value.trim()
            it[K.weatherUpdatedAt] = 0L
        }
    }
    suspend fun setWeatherTemperature(value: String) { context.dataStore.edit { it[K.weatherTemperature] = value.trim() } }
    suspend fun setWeatherCondition(value: String) { context.dataStore.edit { it[K.weatherCondition] = value.trim() } }
    suspend fun setWeatherSnapshot(city: String, temperature: String, condition: String, updatedAt: Long) {
        context.dataStore.edit {
            it[K.weatherCity] = city.trim()
            it[K.weatherTemperature] = temperature.trim()
            it[K.weatherCondition] = condition.trim()
            it[K.weatherUpdatedAt] = updatedAt
        }
    }
    suspend fun setTransactionSounds(value: Boolean) { context.dataStore.edit { it[K.transactionSounds] = value } }
    suspend fun cloudState(owner: String): CloudState {
        val p = context.dataStore.data.first()
        return CloudState(p[ready(owner)] ?: false, p[revision(owner)].orEmpty())
    }
    suspend fun cloudComplete(owner: String, rev: String, message: String) {
        context.dataStore.edit {
            it[ready(owner)] = true; it[revision(owner)] = rev
            it[cloudTime(owner)] = System.currentTimeMillis(); it[cloudMessage(owner)] = message
        }
    }
    suspend fun cloudStatus(owner: String, message: String) {
        context.dataStore.edit { it[cloudMessage(owner)] = message }
    }
    suspend fun skippedRestoreRevision(owner: String): String {
        return context.dataStore.data.first()[skippedRestoreRevisionKey(owner)].orEmpty()
    }
    suspend fun setSkippedRestoreRevision(owner: String, value: String) {
        context.dataStore.edit { prefs ->
            if (value.isBlank()) prefs.remove(skippedRestoreRevisionKey(owner))
            else prefs[skippedRestoreRevisionKey(owner)] = value
        }
    }
    suspend fun resetCloudState(owner: String, message: String) {
        context.dataStore.edit {
            it[ready(owner)] = false
            it.remove(revision(owner))
            it.remove(skippedRestoreRevisionKey(owner))
            it[cloudTime(owner)] = System.currentTimeMillis()
            it[cloudMessage(owner)] = message
        }
    }
    suspend fun migrateLegacyDriveSettings() {
        val p = context.dataStore.data.first()
        val owner = p[K.userId] ?: return
        SecureSecretStore(context, owner).migrateLegacy()
        context.dataStore.edit {
            if (it[drive(owner)] == null) it[drive(owner)] = it[K.oldDrive] ?: false
            if (it[driveTime(owner)] == null) it[driveTime(owner)] = it[K.oldSync] ?: 0L
            it.remove(K.oldDrive); it.remove(K.oldSync)
        }
    }
    suspend fun migrateDriveAccount(old: String, new: String) {
        context.dataStore.edit {
            if (it[drive(new)] == null && it[drive(old)] != null) it[drive(new)] = it[drive(old)]!!
            if (it[driveTime(new)] == null && it[driveTime(old)] != null) it[driveTime(new)] = it[driveTime(old)]!!
        }
    }
}
