package com.j01000011.nyumba.domain.model

data class BoardingHouse(
    val boardingHouseId: String = "",
    val landlordId: String = "",
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val description: String = "",
    val houseRules: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
