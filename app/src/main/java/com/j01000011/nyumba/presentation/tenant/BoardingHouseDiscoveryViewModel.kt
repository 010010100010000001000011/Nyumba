package com.j01000011.nyumba.presentation.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.BoardingHouse
import com.j01000011.nyumba.domain.repository.BoardingHouseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardingHouseDiscoveryViewModel @Inject constructor(
    private val boardingHouseRepository: BoardingHouseRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allBoardingHouses = MutableStateFlow<List<BoardingHouse>>(emptyList())
    
    val filteredBoardingHouses: StateFlow<List<BoardingHouse>> = combine(
        _allBoardingHouses,
        _searchQuery
    ) { houses, query ->
        if (query.isBlank()) {
            houses
        } else {
            val q = query.lowercase().trim()
            houses.filter { house ->
                house.name.lowercase().contains(q) ||
                house.address.lowercase().contains(q) ||
                house.description.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadBoardingHouses()
    }

    fun loadBoardingHouses() {
        viewModelScope.launch {
            boardingHouseRepository.getAllBoardingHouses().collectLatest { houses ->
                _allBoardingHouses.value = houses
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }
}
