package com.example.apptime

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LeaderboardEntry(val uid: String = "", val name: String = "Аноним", val username: String = "", val minutes: Long = 0L, val updatedAt: Long = 0L)

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val leaderboardCollection = firestore.collection("leaderboard")
    private val usernamesCollection = firestore.collection("usernames")

    val currentUid: String get() = auth.currentUser?.uid.orEmpty()
    fun getCurrentNickname(): String = auth.currentUser?.displayName ?: ""

    suspend fun signInAnonymouslyIfNeeded(): String = withContext(Dispatchers.IO) {
        val existingUser = auth.currentUser
        if (existingUser != null) return@withContext existingUser.uid
        Tasks.await(auth.signInAnonymously()).user?.uid ?: throw IllegalStateException("Не удалось выполнить анонимный вход")
    }

    suspend fun isUsernameAvailable(username: String): Boolean = withContext(Dispatchers.IO) {
        !Tasks.await(usernamesCollection.document(username.trim().lowercase()).get()).exists()
    }

    suspend fun registerUsernameAndSubmit(nickname: String, username: String, minutes: Long) = withContext(Dispatchers.IO) {
        val uid = signInAnonymouslyIfNeeded()
        val cleanUsername = username.trim()
        val batch = firestore.batch()
        batch.set(usernamesCollection.document(cleanUsername.lowercase()), mapOf("uid" to uid))
        batch.set(leaderboardCollection.document(uid), mapOf("uid" to uid, "name" to nickname, "username" to cleanUsername, "minutes" to minutes, "updatedAt" to System.currentTimeMillis()))
        Tasks.await(batch.commit())
        auth.currentUser?.let { Tasks.await(it.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nickname).build())) }
    }

    suspend fun submitScore(nickname: String, username: String, minutes: Long) = withContext(Dispatchers.IO) {
        val uid = signInAnonymouslyIfNeeded()
        Tasks.await(leaderboardCollection.document(uid).set(mapOf("uid" to uid, "name" to nickname.trim().ifEmpty { "Игрок_${uid.take(4)}" }, "username" to username.trim().removePrefix("@"), "minutes" to minutes, "updatedAt" to System.currentTimeMillis())))
    }

    suspend fun getTop50Leaderboard(): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        signInAnonymouslyIfNeeded()
        Tasks.await(leaderboardCollection.orderBy("minutes", Query.Direction.DESCENDING).limit(50).get()).documents.mapNotNull { doc ->
            LeaderboardEntry(doc.getString("uid") ?: doc.id, doc.getString("name") ?: "Аноним", doc.getString("username") ?: "", doc.getLong("minutes") ?: 0L, doc.getLong("updatedAt") ?: 0L)
        }
    }

    // --- ФУНКЦИИ ДЛЯ АДМИНКИ ---
    suspend fun getAllUsers(): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        signInAnonymouslyIfNeeded()
        Tasks.await(leaderboardCollection.orderBy("minutes", Query.Direction.DESCENDING).limit(1000).get()).documents.mapNotNull { doc ->
            LeaderboardEntry(doc.getString("uid") ?: doc.id, doc.getString("name") ?: "Аноним", doc.getString("username") ?: "", doc.getLong("minutes") ?: 0L, doc.getLong("updatedAt") ?: 0L)
        }
    }

    suspend fun deleteUser(uid: String, username: String) = withContext(Dispatchers.IO) {
        val batch = firestore.batch()
        batch.delete(leaderboardCollection.document(uid))
        if (username.isNotBlank()) batch.delete(usernamesCollection.document(username.lowercase()))
        Tasks.await(batch.commit())
    }
}