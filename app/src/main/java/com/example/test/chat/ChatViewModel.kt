package com.example.test.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var chatId: String? = null

    fun setChatId(chatId: String) {
        this.chatId = chatId
        listenForMessages()
    }

    private fun listenForMessages() {
        viewModelScope.launch {
            chatId?.let {
                firestore.collection("chats").document(it).collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            // Handle error
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            _messages.value = snapshot.toObjects(Message::class.java)
                        }
                    }
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null && chatId != null) {
                try {
                    val id = firestore.collection("chats").document(chatId!!).collection("messages").document().id
                    val message = Message(id, userId, text)
                    firestore.collection("chats").document(chatId!!).collection("messages").document(id).set(message).await()
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }
}
