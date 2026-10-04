package com.j01000011.nyumba.presentation.landlord

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.BoardingHouse
import com.j01000011.nyumba.domain.repository.BoardingHouseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CreateHouseState {
    object Idle : CreateHouseState()
    object Loading : CreateHouseState()
    object Success : CreateHouseState()
    data class Error(val message: String) : CreateHouseState()
}

@HiltViewModel
class BoardingHouseSwitcherViewModel @Inject constructor(
    private val boardingHouseRepository: BoardingHouseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("nyumba_prefs", Context.MODE_PRIVATE)

    private val _boardingHouses = MutableStateFlow<List<BoardingHouse>>(emptyList())
    val boardingHouses: StateFlow<List<BoardingHouse>> = _boardingHouses.asStateFlow()

    private val _activeBoardingHouse = MutableStateFlow<BoardingHouse?>(null)
    val activeBoardingHouse: StateFlow<BoardingHouse?> = _activeBoardingHouse.asStateFlow()

    private val _createState = MutableStateFlow<CreateHouseState>(CreateHouseState.Idle)
    val createState: StateFlow<CreateHouseState> = _createState.asStateFlow()

    fun loadBoardingHouses(landlordId: String) {
        viewModelScope.launch {
            boardingHouseRepository.getBoardingHouses(landlordId).collectLatest { houses ->
                _boardingHouses.value = houses
                val savedId = sharedPrefs.getString("last_boarding_house_id", null)
                val matchingHouse = houses.find { it.boardingHouseId == savedId }
                _activeBoardingHouse.value = matchingHouse ?: houses.firstOrNull()
            }
        }
    }

    fun switchBoardingHouse(house: BoardingHouse) {
        _activeBoardingHouse.value = house
        sharedPrefs.edit().putString("last_boarding_house_id", house.boardingHouseId).apply()
    }
    
    fun createBoardingHouse(name: String, address: String, phone: String, landlordId: String) {
        viewModelScope.launch {
            _createState.value = CreateHouseState.Loading
            val house = BoardingHouse(
                landlordId = landlordId,
                name = name,
                address = address,
                phone = phone
            )
            boardingHouseRepository.createBoardingHouse(house)
                .onSuccess {
                    _createState.value = CreateHouseState.Success
                }
                .onFailure { e ->
                    _createState.value = CreateHouseState.Error(e.message ?: "Failed to create boarding house")
                }
        }
    }

    fun updateBoardingHouse(house: BoardingHouse) {
        viewModelScope.launch {
            boardingHouseRepository.updateBoardingHouse(house)
        }
    }

    fun clearCreateState() {
        _createState.value = CreateHouseState.Idle
    }
}
