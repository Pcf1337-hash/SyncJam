package com.syncjam.app.feature.social.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncjam.app.feature.social.domain.model.ChatMessage
import com.syncjam.app.feature.social.domain.model.GifResult
import com.syncjam.app.feature.social.domain.model.Reaction
import com.syncjam.app.feature.social.domain.repository.ChatRepository
import com.syncjam.app.feature.social.domain.repository.ReactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

private const val TYPING_INDICATOR_TIMEOUT_MS = 3_000L
private const val GIF_RATE_LIMIT_MS = 5_000L

@Immutable
data class SocialUiState(
    val messages: ImmutableList<ChatMessage> = persistentListOf(),
    /** Reactions received from remote participants, waiting to be consumed by the overlay. */
    val incomingReactions: ImmutableList<Reaction> = persistentListOf(),
    /** Number of unread messages while the chat sheet is closed. */
    val unreadCount: Int = 0,
    /** Display name of the participant currently typing, or null. */
    val typingUser: String? = null,
    /** Epoch-ms timestamp of the last GIF sent by the local user. */
    val lastGifSentAt: Long = 0L,
    /** Whether a GIF search is in progress. */
    val isGifLoading: Boolean = false,
    /** Current GIF search/trending results. */
    val gifResults: ImmutableList<GifResult> = persistentListOf(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val reactionRepository: ReactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private var typingTimeoutJob: Job? = null
    private var sessionId: String = ""

    // ──────────────────────────────────────────────────────────────────────────────
    // Session lifecycle
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Call once when the user joins a session. Starts observing messages and reactions.
     */
    fun joinSession(sessionId: String) {
        this.sessionId = sessionId
        viewModelScope.launch {
            supervisorScope {
                launch {
                    chatRepository.observeMessages(sessionId)
                        .catch { /* TODO: surface error */ }
                        .collect { message -> receiveMessage(message) }
                }
                launch {
                    reactionRepository.observeReactions(sessionId)
                        .catch { /* TODO: surface error */ }
                        .collect { reaction -> receiveReaction(reaction) }
                }
                launch {
                    chatRepository.observeTypingIndicator(sessionId)
                        .catch { /* TODO: surface error */ }
                        .collect { userName ->
                            _uiState.update { it.copy(typingUser = userName) }
                        }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Chat
    // ──────────────────────────────────────────────────────────────────────────────

    fun sendMessage(
        text: String,
        senderName: String,
        senderId: String
    ) {
        val message = ChatMessage(
            id = System.nanoTime().toString(),
            sessionId = sessionId,
            senderId = senderId,
            senderName = senderName,
            senderAvatarUrl = null,
            text = text,
            isOwn = true
        )
        _uiState.update { state ->
            state.copy(messages = (state.messages + message).toPersistentList())
        }
        viewModelScope.launch {
            chatRepository.sendMessage(message)
        }
    }

    private fun receiveMessage(message: ChatMessage, isChatOpen: Boolean = false) {
        _uiState.update { state ->
            state.copy(
                messages = (state.messages + message).toPersistentList(),
                unreadCount = if (isChatOpen) 0 else state.unreadCount + 1
            )
        }
    }

    fun onChatOpened() {
        _uiState.update { it.copy(unreadCount = 0) }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Typing indicator
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Call on every keystroke from [MessageInput] to broadcast the typing indicator
     * and schedule auto-clear after [TYPING_INDICATOR_TIMEOUT_MS].
     */
    fun onTyping(userId: String, displayName: String) {
        typingTimeoutJob?.cancel()
        viewModelScope.launch {
            chatRepository.sendTypingIndicator(sessionId, userId, displayName)
        }
        typingTimeoutJob = viewModelScope.launch {
            delay(TYPING_INDICATOR_TIMEOUT_MS)
            _uiState.update { it.copy(typingUser = null) }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Reactions
    // ──────────────────────────────────────────────────────────────────────────────

    fun sendReaction(
        emoji: String,
        senderName: String,
        senderId: String,
        x: Float
    ) {
        val reaction = Reaction(
            emoji = emoji,
            senderName = senderName,
            senderId = senderId,
            x = x,
            sessionId = sessionId
        )
        _uiState.update { state ->
            state.copy(incomingReactions = (state.incomingReactions + reaction).toPersistentList())
        }
        viewModelScope.launch {
            reactionRepository.sendReaction(reaction)
        }
    }

    private fun receiveReaction(reaction: Reaction) {
        _uiState.update { state ->
            state.copy(
                incomingReactions = (state.incomingReactions + reaction).toPersistentList()
            )
        }
    }

    /**
     * Called by [ReactionOverlay] after it has started animating a reaction, so the ViewModel
     * can remove it from the pending queue.
     */
    fun onReactionConsumed(reactionId: Long) {
        _uiState.update { state ->
            state.copy(
                incomingReactions = state.incomingReactions
                    .filter { it.id != reactionId }
                    .toPersistentList()
            )
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // GIFs
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Sends a GIF URL to all session participants.
     * Enforced rate-limit: one GIF per [GIF_RATE_LIMIT_MS] per user.
     */
    fun sendGif(gifUrl: String) {
        val now = System.currentTimeMillis()
        if (now - _uiState.value.lastGifSentAt < GIF_RATE_LIMIT_MS) return

        _uiState.update { it.copy(lastGifSentAt = now) }
        viewModelScope.launch {
            // TODO: Broadcast GIF URL via Supabase Realtime channel "chat:$sessionId"
        }
    }

    /**
     * Updates the GIF picker results.
     * Pass [gifResults] from [com.syncjam.app.feature.social.data.GiphyDataSource].
     */
    fun onGifResults(gifResults: List<GifResult>) {
        _uiState.update { state ->
            state.copy(
                gifResults = gifResults.toPersistentList(),
                isGifLoading = false
            )
        }
    }

    fun setGifLoading(loading: Boolean) {
        _uiState.update { it.copy(isGifLoading = loading) }
    }

    // ──────────────────────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        typingTimeoutJob?.cancel()
    }
}
