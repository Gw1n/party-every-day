package com.example.hackatum.services

import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class VenueScheduler(private val overpassService: OverpassService) {
    private var job: Job? = null
    
    fun start(scope: CoroutineScope) {
        // Schedule periodic updates (once per day = 24 hours)
        job = scope.launch {
            // Only fetch on startup if no cached data exists
            if (overpassService.loadVenuesFromFile().isEmpty()) {
                println("No cached venues found, fetching from Overpass API...")
                try {
                    overpassService.fetchAndStoreVenues()
                    println("Initial fetch completed")
                } catch (e: Exception) {
                    println("Error in initial fetch: ${e.message}")
                }
            } else {
                println("Using cached venues from ${overpassService.getLastUpdateTime()}")
            }
            
            // Start the periodic update loop
            while (isActive) {
                delay(TimeUnit.HOURS.toMillis(24)) // Update once per day
                try {
                    println("Scheduled venue update starting...")
                    overpassService.fetchAndStoreVenues()
                    println("Scheduled venue update completed")
                } catch (e: Exception) {
                    println("Error in scheduled update: ${e.message}")
                }
            }
        }
    }
    
    fun stop() {
        job?.cancel()
    }
}
