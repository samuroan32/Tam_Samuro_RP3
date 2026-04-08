package com.twinscalev3.data.remote

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.twinscalev3.data.model.ChatMessage
import com.twinscalev3.data.model.GrowthMode
import com.twinscalev3.data.model.UserProfile
import com.twinscalev3.data.model.toFirebaseMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseDataSource(
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    suspend fun createOrJoinRoom(roomId: String, user: UserProfile, mode: GrowthMode): Result<Unit> = runCatching {
        val roomRef = db.child("rooms").child(roomId)
        val current = roomRef.get().await()

        if (!current.exists()) {
            roomRef.child("mode").setValue(mode.name).await()
            roomRef.child("users").child(user.id).setValue(user.toFirebaseMap()).await()
        } else {
            val users = current.child("users").childrenCount
            require(users < 2) { "Room is full." }
            roomRef.child("users").child(user.id).setValue(user.toFirebaseMap()).await()
        }
    }

    suspend fun updateGrowth(roomId: String, userId: String, growthAsString: String) {
        db.child("rooms").child(roomId).child("users").child(userId).child("growth").setValue(growthAsString).await()
    }

    suspend fun setMode(roomId: String, mode: GrowthMode) {
        db.child("rooms").child(roomId).child("mode").setValue(mode.name).await()
    }

    fun observeRoom(roomId: String): Flow<DataSnapshot> = callbackFlow {
        val ref = db.child("rooms").child(roomId)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) = Unit
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendMessage(message: ChatMessage) {
        val id = message.id.ifBlank { UUID.randomUUID().toString() }
        db.child("messages").child(message.roomId).child(id).setValue(message.copy(id = id)).await()
    }

    fun observeMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.child("messages").child(roomId)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val out = snapshot.children.mapNotNull { it.getValue<ChatMessage>() }.sortedBy { it.timestamp }
                trySend(out)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) = Unit
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun markRead(roomId: String, messageId: String) {
        db.child("messages").child(roomId).child(messageId).child("read").setValue(true).await()
    }

    suspend fun updateToken(userId: String, token: String) {
        db.child("userTokens").child(userId).setValue(token).await()
    }
}
