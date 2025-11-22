package com.example.hackatum

import com.example.hackatum.database.DatabaseFactory
import com.example.hackatum.database.VenueRepository
import com.example.hackatum.models.VenuesResponse
import com.example.hackatum.services.OverpassService
import com.example.hackatum.services.VenueScheduler
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlin.math.ceil

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()
    
    // Create services
    val repository = VenueRepository()
    val overpassService = OverpassService(repository)
    val scheduler = VenueScheduler(overpassService)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Install plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    install(CORS) {
        anyHost() // For development - restrict in production!
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
    }
    
    // Start scheduler
    scheduler.start(scope)
    
    // Configure routing
    routing {
        get("/") {
            call.respondText("Munich Discovery API - Server is running!")
        }
        
        get("/api/venues") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 15
            val searchQuery = call.request.queryParameters["search"]
            val category = call.request.queryParameters["category"]
            
            val (venues, totalCount) = overpassService.getVenuesPaginated(
                page = page,
                pageSize = pageSize,
                searchQuery = searchQuery,
                category = category
            )
            val totalPages = ceil(totalCount.toDouble() / pageSize).toInt()
            
            val response = VenuesResponse(
                venues = venues,
                lastUpdated = overpassService.getLastUpdateTime(),
                page = page,
                pageSize = pageSize,
                totalCount = totalCount,
                totalPages = totalPages
            )
            call.respond(response)
        }
        
        get("/api/venues/refresh") {
            val venues = overpassService.fetchAndStoreVenues()
            val totalCount = venues.size.toLong()
            val response = VenuesResponse(
                venues = venues.take(15),
                lastUpdated = overpassService.getLastUpdateTime(),
                page = 1,
                pageSize = 15,
                totalCount = totalCount,
                totalPages = ceil(totalCount.toDouble() / 15).toInt()
            )
            call.respond(response)
        }
        
        get("/api/venues/categories") {
            val venues = overpassService.loadVenuesFromFile()
            val categories = venues.groupBy { it.category }
                .mapValues { it.value.size }
            call.respond(categories)
        }
    }
    
    // Cleanup on shutdown
    environment.monitor.subscribe(ApplicationStopped) {
        scheduler.stop()
        overpassService.close()
    }
}