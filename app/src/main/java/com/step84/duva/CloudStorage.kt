package com.step84.duva

import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.File

interface CloudStorageUploadListener {
    fun onSuccess(url: String)
    fun onFailed()
}

object CloudStorage {
    private val TAG = "CloudStorage"

    fun upload(filename: String, callback: CloudStorageUploadListener) {
        val storage: FirebaseStorage = FirebaseStorage.getInstance()
        val storageReference = storage.reference.child(filename)
        val uploadTask: UploadTask = storageReference.putFile(Uri.fromFile(File(filename)))

        uploadTask
            .addOnSuccessListener { taskSnapshot ->
                Log.i(TAG, "duva: storage uploaded ${taskSnapshot.metadata?.name}, size: ${taskSnapshot.bytesTransferred}")
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "duva: storage upload failed", exception)
                callback.onFailed()
            }

        val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if(!task.isSuccessful) {
                task.exception?.let {
                    callback.onFailed()
                    throw it
                }
            }
            return@Continuation storageReference.downloadUrl
        }).addOnCompleteListener { task ->
            if(task.isSuccessful) {
                val downloadUri = task.result
                callback.onSuccess(downloadUri.toString())
            } else {
                Log.d(TAG, "duva: failed to fetch download URL for file")
                callback.onFailed()
            }
        }
    }
}