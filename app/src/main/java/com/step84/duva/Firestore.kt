package com.step84.duva

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

interface FirestoreListener<T> {
    fun onStart()
    fun onSuccess(obj: T)
    fun onFailed()
}

interface FirestoreCallback {
    fun onSuccess()
    fun onFailed()
}

object Firestore {
    private const val TAG = "Firestore"
    private const val DB_USERS = "users"
    private const val DB_USERS_UID = "uid"
    private const val DB_SUBSCRIPTIONS = "subscriptions"
    private const val DB_SUBSCRIPTIONS_USER = "user"
    private const val DB_SUBSCRIPTIONS_ACTIVE = "active"
    private const val DB_ZONES = "zones"

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
                if (firebaseFirestoreException != null) {
                    Log.d(TAG, "duva: listen failed for userListener: ", firebaseFirestoreException)
                    callback.onFailed()
                    return@addSnapshotListener
                }

                if (querySnapshot != null && querySnapshot.size() == 1) {
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
                if (firebaseFirestoreException != null) {
                    Log.d(TAG, "duva: listen failed for zonesListener(): ", firebaseFirestoreException)
                    callback.onFailed()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
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
                if (firebaseFirestoreException != null) {
                    Log.d(TAG, "duva: listen failed for subscriptionsListener: ", firebaseFirestoreException)
                    callback.onFailed()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    Log.i(TAG, "duva: subscription fetched or updated for user = " + firebaseUser?.uid)
                    callback.onSuccess(querySnapshot.toObjects(Subscription::class.java))
                }
            }
    }

    fun deleteDocument(dbcollection: String, document: String, callback: FirestoreCallback) {
        val db = FirebaseFirestore.getInstance()

        db
            .collection(dbcollection)
            .document(document)
            .delete()
            .addOnSuccessListener {
                Log.i(TAG, "duva: firestore successfully deleted $dbcollection/$document")
                callback.onSuccess()
            }
            .addOnFailureListener {
                Log.i(TAG, "duva: firestore failed to delete $dbcollection/$document")
                callback.onFailed()
            }
    }

    fun <T> updateField(dbcollection: String, document: String, field: String, value: T, callback: FirestoreCallback) {
        val db = FirebaseFirestore.getInstance()

        db
            .collection(dbcollection)
            .document(document)
            .update(field, value)
            .addOnSuccessListener {
                Log.i(TAG, "duva: firestore successfully updated $dbcollection/$document: set $field to $value")
                callback.onSuccess()
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "duva: firestore failed to update $dbcollection/$document: set $field to $value:", e)
                callback.onFailed()
            }
    }

    fun <T> transactionUpdate(dbcollection: String, document: String, fieldvalues: MutableMap<String, T>, callback: FirestoreCallback) {
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection(dbcollection).document(document)

        db.runTransaction { transaction ->
            for((key, value) in fieldvalues) {
                Log.i(TAG, "duva: firestore adding $key:$value to transactional update")
                transaction.update(ref, key, value)
            }
        }.addOnSuccessListener {
            Log.i(TAG, "duva: firestore successfully updated field transaction")
            callback.onSuccess()
        }.addOnFailureListener { e ->
            Log.d(TAG, "duva: firestore failed to update field transaction", e)
            callback.onFailed()
        }
    }

    fun <T> batchUpdate(dbcollection: String, document: String, fieldValues: MutableMap<String, T>, callback: FirestoreCallback) {
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection(dbcollection).document(document)
        val batch = db.batch()

        for((key, value) in fieldValues) {
            Log.i(TAG, "duva: firestore adding $key:$value to batchUpdate")
            batch.update(ref, key, value)
        }

        batch.commit()
            .addOnSuccessListener {
                Log.i(TAG, "duva: firestore successfully commited batch update")
                callback.onSuccess()
            }
            .addOnFailureListener {
                Log.i(TAG, "duva: firestore failed to commit batch update")
                callback.onFailed()
            }
    }

    fun addObject(dbcollection: String, obj: Any, callback: FirestoreCallback) {
        val db = FirebaseFirestore.getInstance()

        db.collection(dbcollection)
            .add(obj)
            .addOnSuccessListener { documentReference ->
                Log.i(TAG, "duva: firestore successfully added object $obj")
                callback.onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "duva: firestore failed to addObject", exception)
                callback.onFailed()
            }
    }

    fun addUser(obj: User, callback: FirestoreCallback) {
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users").document()
        obj.id = ref.id
        ref
            .set(obj)
            .addOnSuccessListener {
                Log.i(TAG, "duva: user successfully added")
                callback.onSuccess()
            }
            .addOnFailureListener {exception ->
                Log.d(TAG, "duva: user failed to be added", exception)
                callback.onFailed()
            }
    }

    fun addSubscription(obj: Subscription, callback: FirestoreCallback) {
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("subscriptions").document()
        obj.id = ref.id
        ref
            .set(obj)
            .addOnSuccessListener {
                Log.i(TAG, "duva: successfully added subscription")
                callback.onSuccess()
            }
            .addOnFailureListener {exception ->
                Log.d(TAG, "duva: failed to add subscription", exception)
                callback.onFailed()
            }
    }

    /**
     * Deprecated, keep for eventual future use
     * Reason was it's just easier to call setupUser/Subscription/Zone() when user logs in,
     * instead of depending on the previous listener to catch the update
     */
    fun updateCurrentUserFromAuthUid(authuid: String, callback: FirestoreCallback) {
        val db = FirebaseFirestore.getInstance()

        db.collection(DB_USERS)
            .whereEqualTo(DB_USERS_UID, authuid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if(querySnapshot.size() == 1) {
                    for(document in querySnapshot) {
                        Globals.currentUser = document.toObject(User::class.java)
                        callback.onSuccess()
                    }
                } else {
                    Log.d(TAG, "duva: firestore multiple users returned for where clause")
                    callback.onFailed()
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "duva: firestore failed to update user object from user id", exception)
                callback.onFailed()
            }
    }

    fun resetActiveSubscriptions(authuid: String, callback: FirestoreCallback) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        db.collection(DB_SUBSCRIPTIONS).whereEqualTo(DB_SUBSCRIPTIONS_USER, authuid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.forEach {
                    val ref = db.collection(DB_SUBSCRIPTIONS).document(it.id)
                    ref.update(DB_SUBSCRIPTIONS_ACTIVE, false)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "duva: firestore failed to reset active subscriptions")
                callback.onFailed()
            }
    }

    fun subscribeToTopic(zoneId: String, callback: FirestoreCallback) {
        FirebaseMessaging.getInstance().subscribeToTopic(zoneId)
            .addOnSuccessListener {
                Log.i(TAG, "duva: FCM subscribed to $zoneId")
                callback.onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "duva: FCM failed to subscribe to topic: ", exception)
                callback.onFailed()
            }
    }

    fun unsubscribeFromTopic(zoneId: String, callback: FirestoreCallback) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(zoneId)
            .addOnSuccessListener {
                Log.i(TAG, "duva: FCM unsubscribed from $zoneId")
                callback.onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "duva: FCM failed to unsubscribe from topic: ", exception)
            }
    }
}
