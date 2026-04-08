package com.twinscalev3.data.model

import java.math.BigInteger

enum class GrowthMode { GENTLE, BALANCED, EXTREME }

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val growth: BigInteger = BigInteger.ONE,
    val online: Boolean = false,
    val fcmToken: String = ""
)

data class Room(
    val roomId: String = "",
    val userA: UserProfile = UserProfile(),
    val userB: UserProfile = UserProfile(),
    val mode: GrowthMode = GrowthMode.BALANCED
)

data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val imageBase64: String? = null,
    val imageFallbackText: String? = null
)

fun UserProfile.toFirebaseMap(): Map<String, Any> = mapOf(
    "id" to id,
    "name" to name,
    "avatarUrl" to avatarUrl,
    "growth" to growth.toString(),
    "online" to online,
    "fcmToken" to fcmToken
)

fun userFromMap(map: Map<String, Any?>): UserProfile = UserProfile(
    id = map["id"] as? String ?: "",
    name = map["name"] as? String ?: "",
    avatarUrl = map["avatarUrl"] as? String ?: "",
    growth = (map["growth"] as? String)?.toBigIntegerOrNull() ?: BigInteger.ONE,
    online = map["online"] as? Boolean ?: false,
    fcmToken = map["fcmToken"] as? String ?: ""
)
