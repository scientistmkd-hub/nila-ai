package com.example.core.security

import android.content.Context
import com.example.data.local.NilaDatabase
import com.example.data.local.entity.AuditEventEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class UserSessionManager private constructor(private val context: Context) {
    private val db = NilaDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUser = MutableStateFlow(
        UserEntity(
            id = GUEST_USER_ID,
            displayName = "Guest User",
            isGuest = true
        )
    )
    val currentUser: StateFlow<UserEntity> = _currentUser.asStateFlow()

    init {
        scope.launch {
            val existing = db.userDao().getUserById(GUEST_USER_ID)
            if (existing == null) {
                db.userDao().insertUser(_currentUser.value)
            }
        }
    }

    fun switchToGuest() {
        val guest = UserEntity(
            id = GUEST_USER_ID,
            displayName = "Guest User",
            isGuest = true
        )
        _currentUser.value = guest
        logAuditEvent("USER_SWITCH_GUEST", "Active profile set to local Guest Mode")
    }

    fun signInUser(email: String, displayName: String) {
        val user = UserEntity(
            id = "user_${email.trim().lowercase().hashCode()}",
            email = email.trim(),
            displayName = displayName.ifBlank { "User" },
            isGuest = false
        )
        _currentUser.value = user
        scope.launch {
            db.userDao().insertUser(user)
            logAuditEvent("USER_SIGN_IN", "Authenticated as ${user.displayName} (Isolated ID)")
        }
    }

    fun signOut() {
        logAuditEvent("USER_SIGN_OUT", "User logged out. Returning to local Guest Mode.")
        switchToGuest()
    }

    fun logAuditEvent(eventType: String, description: String) {
        scope.launch {
            val event = AuditEventEntity(
                id = UUID.randomUUID().toString(),
                userId = _currentUser.value.id,
                eventType = eventType,
                description = description,
                timestamp = System.currentTimeMillis()
            )
            db.auditDao().insertAuditEvent(event)
        }
    }

    companion object {
        const val GUEST_USER_ID = "guest_default"

        @Volatile
        private var INSTANCE: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = UserSessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
