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
    fun getUser(firebaseUser: FirebaseUser?, callback: FirestoreListener<User>) {
        callback.onStart()
        val db = FirebaseFirestore.getInstance()

        db.collection(DB_USERS)
            .whereEqualTo(DB_USERS_UID, firebaseUser?.uid)
            .get()
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    val documents = task.result
                    if(documents != null) {
                        val usersFromDb = documents.toObjects(User::class.java)
                        for(user in usersFromDb) {
                            if(usersFromDb.size == 1) {
                                Log.i(TAG, "duva: found one match, usersFromDb = " + user.added + ", " + user.lastLocation)
                                callback.onSuccess(user)
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "duva: Exception: ", task.exception)
                    callback.onFailed()
                }
            }
    }

    fun getSubscriptions(firebaseUser: FirebaseUser?, callback: FirestoreListener<MutableList<Subscription>>) {
        callback.onStart()
        val db = FirebaseFirestore.getInstance()

        /*
        db.collection(DB_SUBSCRIPTIONS)
            //.whereEqualTo(DB_SUBSCRIPTIONS_ACTIVE, true)
            .whereEqualTo(DB_SUBSCRIPTIONS_USER, firebaseUser?.uid)
            .get()
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    val documents = task.result
                    if(documents != null) {
                        val subscriptionsFromDb = documents.toObjects(Subscription::class.java)
                        callback.onSuccess(subscriptionsFromDb)
                    }
                } else {
                    Log.d(TAG, "duva: Exception", task.exception)
                    callback.onFailed()
                }
            }
            */
        db.collection(DB_SUBSCRIPTIONS)
            .whereEqualTo(DB_SUBSCRIPTIONS_USER, firebaseUser?.uid)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null) {
                    Log.d(TAG, "duva: Listen failed:", firebaseFirestoreException)
                    return@addSnapshotListener
                }

                if(querySnapshot != null) {
                    Log.i(TAG, "duva: subscription fetched or updated for user = " + firebaseUser?.uid)
                    val subscriptionsFromDb = querySnapshot.toObjects(Subscription::class.java)
                    callback.onSuccess(subscriptionsFromDb)
                }
            }
    }
}