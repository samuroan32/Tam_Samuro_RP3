# TwinScaleV3 (Fresh Standalone Android Project)

A new Android app from scratch using Kotlin + Jetpack Compose + Firebase Realtime Database + FCM.

## 1) Project structure

```text
TwinScaleV3/
  build.gradle.kts
  settings.gradle.kts
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      java/com/twinscalev3/
        MainActivity.kt
        TwinScaleApp.kt
        data/model/Models.kt
        data/remote/FirebaseDataSource.kt
        data/repo/TwinScaleRepository.kt
        domain/growth/GrowthEngine.kt
        notification/ChatPresenceTracker.kt
        notification/NotificationHelper.kt
        notification/TwinScaleMessagingService.kt
        ui/navigation/Nav.kt
        ui/screen/Screens.kt
        ui/theme/Theme.kt
        viewmodel/AppViewModel.kt
      res/
```

## 2) Growth logic summary

`GrowthEngine` uses:

- `randomFactor` sampled from mode-dependent min/max
- `scale(current)` derived from value magnitude (`digits`) via logarithmic + mild linear term
- `modeMultiplier` for mode aggression
- directional factor for grow/shrink

Formula concept used in code:

```kotlin
fraction = randomFactor * scale(current) * modeMultiplier * directionFactor
delta = max(1, current * fraction)
next = current + delta OR max(1, current - delta)
```

This satisfies randomness, progressive scaling, bidirectional updates, and stability for BigInteger values.

## 3) BigInteger handling

- Growth is `BigInteger` in app models and ViewModel.
- In Firebase, growth is stored as String (`growth.toString()`).
- On read, String is parsed with `toBigIntegerOrNull()`.
- Lower bound is clamped to `1` to avoid zero/negative collapse.

## 4) Notification suppression logic

`ChatPresenceTracker` stores:
- `isChatOpen`
- `activePartnerId`

In `TwinScaleMessagingService`:
- If incoming message sender matches active partner and chat is open: suppress notification.
- Else: show local notification via `NotificationHelper`.

This covers closed app/background and “not on active chat” cases.

## 5) Image sending with fallback

- `TwinScaleRepository.bitmapToBase64(...)` compresses image (JPEG quality param) and converts to Base64.
- Message stores `imageBase64` plus `imageFallbackText`.
- Receiver attempts decode with `decodeBase64Image(...)`; fallback text shown when decode fails.
- Add “retry decode” action in UI by reattempting decode on tap (hook point: ChatScreen message card).

## 6) Firebase setup

1. Create Firebase project.
2. Add Android app package `com.twinscalev3`.
3. Download `google-services.json` into `app/`.
4. Enable **Realtime Database** (start in locked mode then apply rules).
5. Recommended basic rules for two-user rooms:

```json
{
  "rules": {
    "rooms": {
      "$roomId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "messages": {
      "$roomId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "userTokens": {
      "$uid": {
        ".read": false,
        ".write": "auth != null && auth.uid == $uid"
      }
    }
  }
}
```

## 7) FCM setup

1. In Firebase Console, enable Cloud Messaging.
2. Add server credentials for Cloud Functions (or Admin SDK environment).
3. Keep `TwinScaleMessagingService` registered in `AndroidManifest.xml`.
4. Request notification permission on Android 13+ (`POST_NOTIFICATIONS`).
5. Create channel at app startup (`TwinScaleApp`).
6. Replace `res/raw/notify_sound.txt` with `notify_sound.ogg`.
7. Upload token from app on login/room enter using `updateToken(userId, token)`.

## 8) Sample Cloud Function (Node.js, Admin SDK)

```js
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendTwinScaleMessagePush = functions.database
  .ref("/messages/{roomId}/{messageId}")
  .onCreate(async (snapshot, context) => {
    const msg = snapshot.val();
    const receiverId = msg.receiverId;
    if (!receiverId) return null;

    const tokenSnap = await admin.database().ref(`/userTokens/${receiverId}`).get();
    const token = tokenSnap.val();
    if (!token) return null;

    const payload = {
      token,
      data: {
        title: "TwinScaleV3",
        text: msg.text || msg.imageFallbackText || "New image",
        senderId: msg.senderId || "",
        roomId: msg.roomId || ""
      },
      android: {
        priority: "high",
        notification: {
          channelId: "twinscale_chat",
          sound: "notify_sound"
        }
      }
    };

    return admin.messaging().send(payload);
  });
```

## 9) Build/run

```bash
cd TwinScaleV3
./gradlew :app:assembleDebug
```

