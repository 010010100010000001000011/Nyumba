package com.j01000011.nyumba.core.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var firestore: FirebaseFirestore
    
    @Inject
    lateinit var auth: FirebaseAuth

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = auth.currentUser?.uid ?: return
        updateTokenInFirestore(uid, token)
    }

    private fun updateTokenInFirestore(uid: String, token: String) {
        firestore.collection("users").document(uid)
            .update("fcmToken", token)
    }
}
