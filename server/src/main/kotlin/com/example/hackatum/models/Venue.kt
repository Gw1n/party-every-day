package com.example.hackatum.models

import kotlinx.serialization.Serializable

@Serializable
data class Venue(
    val id: String,
    val name: String,
    val category: String,
    val street: String? = null,
    val houseNumber: String? = null,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null
) {
    val fullAddress: String
        get() = buildString {
            if (!street.isNullOrBlank()) {
                append(street)
                if (!houseNumber.isNullOrBlank()) {
                    append(" ")
                    append(houseNumber)
                }
            } else {
                append("Address not available")
            }
        }
}

@Serializable
data class VenuesResponse(
    val venues: List<Venue>,
    val lastUpdated: String,
    val page: Int = 1,
    val pageSize: Int = 15,
    val totalCount: Long = 0,
    val totalPages: Int = 0
)
