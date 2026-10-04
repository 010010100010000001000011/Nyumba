package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

abstract class BaseRepository(protected val firestore: FirebaseFirestore) {
    
    protected fun getCollection(path: String): CollectionReference = firestore.collection(path)

    protected suspend fun <T : Any> addDocument(path: String, data: T): Result<String> = try {
        val ref = getCollection(path).add(data).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    protected suspend fun <T : Any> setDocument(path: String, id: String, data: T): Result<Unit> = try {
        getCollection(path).document(id).set(data).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    protected suspend fun deleteDocument(path: String, id: String): Result<Unit> = try {
        getCollection(path).document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
