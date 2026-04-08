package com.twinscalev3.data.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.twinscalev3.data.model.ChatMessage
import com.twinscalev3.data.model.GrowthMode
import com.twinscalev3.data.model.UserProfile
import com.twinscalev3.data.remote.FirebaseDataSource
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayOutputStream

class TwinScaleRepository(
    private val remote: FirebaseDataSource = FirebaseDataSource()
) {
    suspend fun createOrJoinRoom(roomId: String, user: UserProfile, mode: GrowthMode) =
        remote.createOrJoinRoom(roomId, user, mode)

    suspend fun updateGrowth(roomId: String, userId: String, growthAsString: String) =
        remote.updateGrowth(roomId, userId, growthAsString)

    suspend fun setMode(roomId: String, mode: GrowthMode) = remote.setMode(roomId, mode)

    fun observeRoom(roomId: String) = remote.observeRoom(roomId)

    suspend fun sendMessage(message: ChatMessage) = remote.sendMessage(message)

    fun observeMessages(roomId: String): Flow<List<ChatMessage>> = remote.observeMessages(roomId)

    suspend fun markRead(roomId: String, messageId: String) = remote.markRead(roomId, messageId)

    suspend fun updateToken(userId: String, token: String) = remote.updateToken(userId, token)

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 75): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun decodeBase64Image(data: String): Bitmap? = runCatching {
        val bytes = Base64.decode(data, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
