package com.j01000011.nyumba.domain.repository

import com.j01000011.nyumba.domain.model.BoardingHouse
import kotlinx.coroutines.flow.Flow

interface BoardingHouseRepository {
    fun getBoardingHouses(landlordId: String): Flow<List<BoardingHouse>>
    suspend fun createBoardingHouse(house: BoardingHouse): Result<String>
    suspend fun updateBoardingHouse(house: BoardingHouse): Result<Unit>
    suspend fun getBoardingHouseById(houseId: String): Result<BoardingHouse>
}
