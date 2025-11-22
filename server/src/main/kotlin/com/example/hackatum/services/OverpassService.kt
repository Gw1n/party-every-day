package com.example.hackatum.services

import com.example.hackatum.database.VenueRepository
import com.example.hackatum.models.OverpassElement
import com.example.hackatum.models.OverpassResponse
import com.example.hackatum.models.Venue
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OverpassService(private val repository: VenueRepository) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }
    
    private val dataFile = File("data/venues.json")
    private val overpassUrl = "https://overpass-api.de/api/interpreter"
    
    // Overpass QL query for München venues
    private val query = """
        [out:json][timeout:25];
        area["name"="München"]->.searchArea;
        (
          node["amenity"="pub"](area.searchArea);
          node["tourism"~"museum|attraction|viewpoint"](area.searchArea);
          node["amenity"="cafe"](area.searchArea);
          node["leisure"="park"](area.searchArea);
        );
        out body;
        >;
        out skel qt;
    """.trimIndent()
    
    suspend fun fetchAndStoreVenues(): List<Venue> {
        try {
            println("Fetching venues from Overpass API...")
            
            val response: OverpassResponse = client.post(overpassUrl) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("data=$query")
            }.body()
            
            println("Received ${response.elements.size} elements from Overpass API")
            
            val allVenues = response.elements.mapNotNull { element ->
                parseElement(element)
            }
            
            // Filter venues that have both street and house number
            val venuesWithAddress = allVenues.filter { venue ->
                !venue.street.isNullOrBlank() && !venue.houseNumber.isNullOrBlank()
            }
            
            println("Parsed ${allVenues.size} venues, ${venuesWithAddress.size} with complete addresses")
            
            // Save to file (for backup)
            saveVenuesToFile(venuesWithAddress)
            
            // Store in database
            repository.clearAll()
            venuesWithAddress.forEach { venue ->
                repository.insertOrUpdateVenue(venue)
            }
            println("Stored ${venuesWithAddress.size} venues in database")
            
            return venuesWithAddress
        } catch (e: Exception) {
            println("Error fetching venues: ${e.message}")
            e.printStackTrace()
            // Return from database if fetch fails
            return repository.getAllVenues(limit = 1000)
        }
    }
    
    private fun parseElement(element: OverpassElement): Venue? {
        val tags = element.tags ?: return null
        val lat = element.lat ?: return null
        val lon = element.lon ?: return null
        
        val name = tags["name"] ?: tags["ref"] ?: "Unnamed"
        
        // Determine category
        val category = when {
            tags["amenity"] == "pub" -> "Pub"
            tags["tourism"] == "museum" -> "Museum"
            tags["tourism"] == "attraction" -> "Landmark"
            tags["tourism"] == "viewpoint" -> "Viewpoint"
            tags["amenity"] == "cafe" -> "Café"
            tags["leisure"] == "park" -> "Park"
            else -> "Other"
        }
        
        return Venue(
            id = element.id.toString(),
            name = name,
            category = category,
            street = tags["addr:street"],
            houseNumber = tags["addr:housenumber"],
            latitude = lat,
            longitude = lon,
            description = tags["description"] ?: tags["note"]
        )
    }
    
    private fun saveVenuesToFile(venues: List<Venue>) {
        dataFile.parentFile?.mkdirs()
        val json = Json { prettyPrint = true }
        val jsonString = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Venue.serializer()),
            venues
        )
        dataFile.writeText(jsonString)
        println("Saved ${venues.size} venues to ${dataFile.absolutePath}")
    }
    
    fun loadVenuesFromFile(): List<Venue> {
        // Now load from database instead of file
        return repository.getAllVenues(limit = 1000)
    }
    
    fun getVenuesPaginated(
        page: Int, 
        pageSize: Int,
        searchQuery: String? = null,
        category: String? = null
    ): Pair<List<Venue>, Long> {
        val offset = ((page - 1) * pageSize).toLong()
        return repository.searchVenues(
            searchQuery = searchQuery,
            category = category,
            limit = pageSize,
            offset = offset
        )
    }
    
    fun getLastUpdateTime(): String {
        return if (dataFile.exists()) {
            val lastModified = dataFile.lastModified()
            val dateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(lastModified),
                java.time.ZoneId.systemDefault()
            )
            dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            "Never"
        }
    }
    
    fun close() {
        client.close()
    }
}
