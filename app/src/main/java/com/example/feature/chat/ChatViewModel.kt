package com.example.feature.chat

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiRouter
import com.example.ai.voice.AndroidTtsEngine
import com.example.core.command.LocalCommandRouter
import com.example.core.model.MemoryCategory
import com.example.core.model.MessageRole
import com.example.core.security.UserSessionManager
import com.example.data.local.NilaDatabase
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val conversationId: String = "",
    val messages: List<MessageEntity> = emptyList(),
    val isSending: Boolean = false,
    val inputText: String = "",
    val error: String? = null,
    val infoMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = NilaDatabase.getInstance(application)
    private val userSessionManager = UserSessionManager.getInstance(application)
    private val localCommandRouter = LocalCommandRouter(application)
    private val aiRouter = AiRouter(application, localCommandRouter)
    val ttsEngine = AndroidTtsEngine(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val activeUserId: String
        get() = userSessionManager.currentUser.value.id

    init {
        initializeConversation()
    }

    private fun initializeConversation() {
        viewModelScope.launch {
            val convos = db.conversationDao().getConversationsForUser(activeUserId).firstOrNull()
            val existing = convos?.firstOrNull()

            if (existing != null) {
                _uiState.update { it.copy(conversationId = existing.id) }
                observeMessages(existing.id)
            } else {
                val newId = UUID.randomUUID().toString()
                val newConvo = ConversationEntity(
                    id = newId,
                    userId = activeUserId,
                    title = "New Session"
                )
                db.conversationDao().insertConversation(newConvo)
                _uiState.update { it.copy(conversationId = newId) }
                observeMessages(newId)

                // Initial welcome message from Nila
                val welcomeMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = newId,
                    userId = activeUserId,
                    role = MessageRole.ASSISTANT,
                    content = "Hello! I am Nila, your personal AI assistant. I run locally on your device with complete privacy. How can I help you today?",
                    metadata = "Local On-Device Engine"
                )
                db.messageDao().insertMessage(welcomeMsg)
            }
        }
    }

    private fun observeMessages(convId: String) {
        viewModelScope.launch {
            db.messageDao().getMessagesForConversation(convId, activeUserId).collect { list ->
                _uiState.update { it.copy(messages = list) }
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isSending) return

        val convId = _uiState.value.conversationId
        _uiState.update { it.copy(inputText = "", isSending = true, error = null) }

        viewModelScope.launch {
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                userId = activeUserId,
                role = MessageRole.USER,
                content = text
            )
            db.messageDao().insertMessage(userMsg)

            val history = _uiState.value.messages.map { it.role.name to it.content }
            val (reply, provider) = aiRouter.routeQuery(
                prompt = text,
                conversationContext = history
            )

            val assistantMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                userId = activeUserId,
                role = MessageRole.ASSISTANT,
                content = reply,
                metadata = provider
            )
            db.messageDao().insertMessage(assistantMsg)
            _uiState.update { it.copy(isSending = false) }
        }
    }

    fun saveToMemory(content: String) {
        viewModelScope.launch {
            val mem = MemoryEntity(
                id = UUID.randomUUID().toString(),
                userId = activeUserId,
                category = MemoryCategory.PERSONAL_DETAIL,
                key = content.take(30).trim(),
                value = content
            )
            db.memoryDao().insertMemory(mem)
            _uiState.update { it.copy(infoMessage = "Saved to Nila Memory Bank") }
        }
    }

    fun deleteMessage(msgId: String) {
        viewModelScope.launch {
            db.messageDao().deleteMessage(msgId, activeUserId)
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            val convId = _uiState.value.conversationId
            db.messageDao().deleteMessagesForConversation(convId, activeUserId)
            _uiState.update { it.copy(infoMessage = "Chat cleared") }
        }
    }

    fun copyMessage(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Nila Message", text)
        clipboard?.setPrimaryClip(clip)
        _uiState.update { it.copy(infoMessage = "Copied to clipboard") }
    }

    fun dismissInfo() {
        _uiState.update { it.copy(infoMessage = null, error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine.shutdown()
    }
}
