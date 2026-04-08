package com.twinscalev3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.twinscalev3.data.model.ChatMessage
import com.twinscalev3.data.model.GrowthMode
import com.twinscalev3.data.model.UserProfile
import com.twinscalev3.data.repo.TwinScaleRepository
import com.twinscalev3.domain.growth.GrowthEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.UUID

data class AppUiState(
    val roomId: String = "",
    val selfId: String = "",
    val selfName: String = "",
    val selfGrowth: BigInteger = BigInteger.ONE,
    val partnerId: String = "",
    val partnerGrowth: BigInteger = BigInteger.ONE,
    val mode: GrowthMode = GrowthMode.BALANCED,
    val messages: List<ChatMessage> = emptyList(),
    val online: Boolean = true
) {
    val ratioText: String
        get() = if (partnerGrowth == BigInteger.ZERO) "∞" else selfGrowth.toBigDecimal(MathContext.DECIMAL64)
            .divide(partnerGrowth.toBigDecimal(MathContext.DECIMAL64), MathContext.DECIMAL64).toPlainString()

    val largerLabel: String
        get() = when {
            selfGrowth > partnerGrowth -> "You"
            selfGrowth < partnerGrowth -> "Partner"
            else -> "Equal"
        }

    val selfScale: Float
        get() = normalized(selfGrowth)
    val partnerScale: Float
        get() = normalized(partnerGrowth)

    private fun normalized(v: BigInteger): Float {
        val digits = v.toString().length.coerceAtLeast(1)
        val n = (0.4f + (digits.coerceAtMost(300) / 300f))
        return n.coerceIn(0.35f, 1.4f)
    }
}

class AppViewModel(
    private val repo: TwinScaleRepository = TwinScaleRepository(),
    private val growthEngine: GrowthEngine = GrowthEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun enterRoom(roomId: String, name: String, initialGrowth: String) {
        val selfId = UUID.randomUUID().toString()
        val user = UserProfile(
            id = selfId,
            name = name,
            growth = initialGrowth.toBigIntegerOrNull() ?: BigInteger.ONE,
            online = true
        )
        _uiState.update { it.copy(roomId = roomId, selfId = selfId, selfName = name, selfGrowth = user.growth) }

        viewModelScope.launch {
            repo.createOrJoinRoom(roomId, user, _uiState.value.mode)
            observeRoom()
            observeChat()
        }
    }

    fun switchMode(mode: GrowthMode) {
        _uiState.update { it.copy(mode = mode) }
        viewModelScope.launch { repo.setMode(_uiState.value.roomId, mode) }
    }

    fun growSelf() = mutateSelf(true)
    fun shrinkSelf() = mutateSelf(false)

    private fun mutateSelf(up: Boolean) {
        val s = _uiState.value
        val next = if (up) growthEngine.grow(s.selfGrowth, s.mode) else growthEngine.shrink(s.selfGrowth, s.mode)
        _uiState.update { it.copy(selfGrowth = next) }
        viewModelScope.launch { repo.updateGrowth(s.roomId, s.selfId, next.toString()) }
    }

    fun boostPartner() {
        val s = _uiState.value
        val next = growthEngine.grow(s.partnerGrowth, s.mode)
        _uiState.update { it.copy(partnerGrowth = next) }
    }

    fun drainPartner() {
        val s = _uiState.value
        val next = growthEngine.shrink(s.partnerGrowth, s.mode)
        _uiState.update { it.copy(partnerGrowth = next) }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        val s = _uiState.value
        viewModelScope.launch {
            repo.sendMessage(
                ChatMessage(
                    roomId = s.roomId,
                    senderId = s.selfId,
                    receiverId = s.partnerId,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun sendSampleImageFallback() {
        val s = _uiState.value
        viewModelScope.launch {
            repo.sendMessage(
                ChatMessage(
                    roomId = s.roomId,
                    senderId = s.selfId,
                    receiverId = s.partnerId,
                    text = "",
                    imageBase64 = null,
                    imageFallbackText = "[image unavailable - tap retry]",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun observeRoom() {
        val roomId = _uiState.value.roomId
        viewModelScope.launch {
            repo.observeRoom(roomId).collect { snapshot ->
                val users = snapshot.child("users").children.toList()
                if (users.size == 2) {
                    val self = users.firstOrNull { it.key == _uiState.value.selfId }
                    val partner = users.firstOrNull { it.key != _uiState.value.selfId }
                    val selfGrowth = (self?.child("growth")?.value as? String)?.toBigIntegerOrNull() ?: _uiState.value.selfGrowth
                    val partnerGrowth = (partner?.child("growth")?.value as? String)?.toBigIntegerOrNull() ?: _uiState.value.partnerGrowth
                    _uiState.update {
                        it.copy(
                            selfGrowth = selfGrowth,
                            partnerGrowth = partnerGrowth,
                            partnerId = partner?.key ?: ""
                        )
                    }
                }
            }
        }
    }

    private fun observeChat() {
        viewModelScope.launch {
            repo.observeMessages(_uiState.value.roomId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }
}

class AppViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AppViewModel() as T
    }
}
