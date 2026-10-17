package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.j01000011.nyumba.domain.model.BoardingHouse
import com.j01000011.nyumba.domain.repository.BoardingHouseRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoardingHouseRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore
) : BaseRepository(firestore), BoardingHouseRepository {

    override fun getBoardingHouses(landlordId: String): Flow<List<BoardingHouse>> = callbackFlow {
        val subscription = firestore.collection("boardingHouses")
            .whereEqualTo("landlordId", landlordId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val houses = snapshot.toObjects(BoardingHouse::class.java)
                    trySend(houses)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getAllBoardingHouses(): Flow<List<BoardingHouse>> = callbackFlow {
        val subscription = firestore.collection("boardingHouses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val houses = snapshot.toObjects(BoardingHouse::class.java)
                    trySend(houses)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createBoardingHouse(house: BoardingHouse): Result<String> {
        return try {
            val docRef = firestore.collection("boardingHouses").document()
            val newHouse = house.copy(boardingHouseId = docRef.id)
            docRef.set(newHouse).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBoardingHouse(house: BoardingHouse): Result<Unit> {
        return setDocument("boardingHouses", house.boardingHouseId, house)
    }

    override suspend fun getBoardingHouseById(houseId: String): Result<BoardingHouse> {
        return try {
            val snapshot = firestore.collection("boardingHouses").document(houseId).get().await()
            val house = snapshot.toObject(BoardingHouse::class.java) ?: throw Exception("Boarding house not found")
            Result.success(house)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
