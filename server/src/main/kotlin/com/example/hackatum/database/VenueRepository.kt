package com.example.hackatum.database

import com.example.hackatum.models.Venue
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class VenueRepository {
    
    fun insertVenue(venue: Venue) {
        transaction {
            VenueTable.insert {
                it[externalId] = venue.id
                it[name] = venue.name
                it[category] = venue.category
                it[street] = venue.street!!
                it[houseNumber] = venue.houseNumber!!
                it[latitude] = venue.latitude
                it[longitude] = venue.longitude
                it[description] = venue.description
            }
        }
    }
    
    fun insertOrUpdateVenue(venue: Venue) {
        transaction {
            val exists = VenueTable.selectAll()
                .where { VenueTable.externalId eq venue.id }
                .count() > 0
            
            if (exists) {
                VenueTable.update({ VenueTable.externalId eq venue.id }) {
                    it[name] = venue.name
                    it[category] = venue.category
                    it[street] = venue.street!!
                    it[houseNumber] = venue.houseNumber!!
                    it[latitude] = venue.latitude
                    it[longitude] = venue.longitude
                    it[description] = venue.description
                }
            } else {
                VenueTable.insert {
                    it[externalId] = venue.id
                    it[name] = venue.name
                    it[category] = venue.category
                    it[street] = venue.street!!
                    it[houseNumber] = venue.houseNumber!!
                    it[latitude] = venue.latitude
                    it[longitude] = venue.longitude
                    it[description] = venue.description
                }
            }
        }
    }
    
    fun getAllVenues(limit: Int = 15, offset: Long = 0): List<Venue> {
        return transaction {
            VenueTable.selectAll()
                .limit(limit, offset)
                .map { rowToVenue(it) }
        }
    }
    
    fun getTotalCount(): Long {
        return transaction {
            VenueTable.selectAll().count()
        }
    }
    
    fun searchVenues(
        searchQuery: String? = null,
        category: String? = null,
        limit: Int = 15,
        offset: Long = 0
    ): Pair<List<Venue>, Long> {
        return transaction {
            var query = VenueTable.selectAll()
            
            // Apply search filter
            if (!searchQuery.isNullOrBlank()) {
                query = query.where { 
                    VenueTable.name.lowerCase() like "%${searchQuery.lowercase()}%"
                }
            }
            
            // Apply category filter
            if (!category.isNullOrBlank()) {
                query = query.where { VenueTable.category eq category }
            }
            
            val total = query.count()
            val venues = query
                .limit(limit, offset)
                .map { rowToVenue(it) }
            
            Pair(venues, total)
        }
    }
    
    fun getVenuesByCategory(category: String, limit: Int = 15, offset: Long = 0): List<Venue> {
        return transaction {
            VenueTable.selectAll()
                .where { VenueTable.category eq category }
                .limit(limit, offset)
                .map { rowToVenue(it) }
        }
    }
    
    fun clearAll() {
        transaction {
            VenueTable.deleteAll()
        }
    }
    
    private fun rowToVenue(row: ResultRow): Venue {
        return Venue(
            id = row[VenueTable.externalId],
            name = row[VenueTable.name],
            category = row[VenueTable.category],
            street = row[VenueTable.street],
            houseNumber = row[VenueTable.houseNumber],
            latitude = row[VenueTable.latitude],
            longitude = row[VenueTable.longitude],
            description = row[VenueTable.description]
        )
    }
}
