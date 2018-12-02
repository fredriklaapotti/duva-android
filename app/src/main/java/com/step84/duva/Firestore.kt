package com.step84.duva

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

interface FirestoreListener<T> {
    fun onStart()
    fun onSuccess(obj: T)
    fun onFailed()
}

object Firestore {
    private const val TAG = "Firestore"
    private const val DB_USERS = "users"
    private const val DB_USERS_UID = "uid"
    private const val DB_SUBSCRIPTIONS = "subscriptions"
    private const val DB_SUBSCRIPTIONS_ACTIVE = "active"
    private const val DB_SUBSCRIPTIONS_USER = "user"
    private const val DB_ZONES = "zones"
    private const val DB_ZONES_ID = "zid"

    /**
     * Populate user object from database.
     *
     * @param firebaseUser Firebase user object from auth.currentUser
     * @param callback Callback listener
     */
    fun userListener(firebaseUser: FirebaseUser?, callback: FirestoreListener<User>) {
        callback.onStart()
        val db = FirebaseFirestore.getInstance()

        db.collection(DB_USERS)
            .whereEqualTo(DB_USERS_UID, firebaseUser?.uid)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null) {
                    Log.d(TAG, "duva: listen failed for userListener: ", firebaseFirestoreException)
                    callback.onFailed()
                    return@addSnapshotListener
                }

                if(querySnapshot != null && querySnapshot.size() == 1) {
                    Log.i(TAG, "duva: user object fetched or updated for user = " + firebaseUser?.uid)
                    callback.onSuccess(querySnapshot.toObjects(User::class.java)[0])
                }
            }
    }

    fun zonesListener(callback: FirestoreListener<MutableList<Zone>>) {
        callback.onStart()
        val db = FirebaseFirestore.getInstance()

        db.collection(DB_ZONES)
            //.whereEqualTo()
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null) {
                    Log.d(TAG, "duva: listen failed for zonesListener(): ", firebaseFirestoreException)
                    callback.onFailed()
                    return@addSnapshotListener
                }

                if(querySnapshot != null) {
                    Log.i(TAG, "duva: zone fetched or updated = " + querySnapshot.documents.toString())
                    callback.onSuccess(querySnapshot.toObjects(Zone::class.java))
                }
            }
    }

    fun subscriptionsListener(firebaseUser: FirebaseUser?, callback: FirestoreListener<MutableList<Subscription>>) {
        callback.onStart()
        val db = FirebaseFirestore.getInstance()

        db.collection(DB_SUBSCRIPTIONS)
            .whereEqualTo(DB_SUBSCRIPTIONS_USER, firebaseUser?.uid)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null) {
                    Log.d(TAG, "duva: listen failed for subscriptionsListener: ", firebaseFirestoreException)
                    callback.onFailed()
                    return@addSnapshotListener
                }

                if(querySnapshot != null) {
                    Log.i(TAG, "duva: subscription fetched or updated for user = " + firebaseUser?.uid)
                    callback.onSuccess(querySnapshot.toObjects(Subscription::class.java))
                }
            }
    }
}