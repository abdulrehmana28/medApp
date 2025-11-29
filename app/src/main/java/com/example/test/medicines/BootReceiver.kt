package com.example.test.medicines

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: return

            // Re-fetch medicines from Firestore and schedule them
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).collection("medicines")
                .get()
                .addOnSuccessListener { snapshot ->
                    val medicines = snapshot.toObjects(Medicine::class.java)
                    for (med in medicines) {
                        AlarmScheduler.scheduleAlarmsForMedicine(context, med)
                    }
                }
        }
    }
}