package com.example.hackatum

import kotlinx.browser.window
import kotlinx.coroutines.await
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.button
import web.cssom.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ApiVenue(
    val id: String,
    val name: String,
    val category: String,
    val street: String? = null,
    val houseNumber: String? = null,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null
)

@Serializable
data class VenuesResponse(
    val venues: List<ApiVenue>,
    val lastUpdated: String,
    val page: Int = 1,
    val pageSize: Int = 15,
    val totalCount: Long = 0,
    val totalPages: Int = 0
)

val App = FC<Props> {
    var venues by useState(emptyList<ApiVenue>())
    var loading by useState(true)
    var error by useState<String?>(null)
    var currentPage by useState(1)
    var totalPages by useState(1)
    var totalCount by useState(0L)
    var searchQuery by useState("")
    var selectedCategory by useState<String?>(null)
    
    val categories = listOf("Pub", "Museum", "Café", "Park", "Landmark", "Viewpoint")
    
    // Fetch venues when page, search, or category changes
    useEffect(currentPage, searchQuery, selectedCategory) {
        loading = true
        error = null
        
        // Build URL with query parameters
        val params = mutableListOf("page=$currentPage", "pageSize=15")
        if (searchQuery.isNotBlank()) {
            params.add("search=${searchQuery}")
        }
        if (selectedCategory != null) {
            params.add("category=$selectedCategory")
        }
        val url = "http://localhost:8081/api/venues?" + params.joinToString("&")
        
        val promise = window.fetch(url)
            .then { response ->
                if (response.ok) {
                    response.text()
                } else {
                    throw Exception("HTTP error! status: ${response.status}")
                }
            }
            .then { text ->
                val json = Json { ignoreUnknownKeys = true }
                val response = json.decodeFromString<VenuesResponse>(text)
                venues = response.venues
                totalPages = response.totalPages
                totalCount = response.totalCount
                loading = false
            }
            .catch { err ->
                console.error("Error fetching venues:", err)
                error = "Failed to load venues: ${err.message}"
                loading = false
            }
    }

    div {
        className = ClassName("w-full min-h-screen bg-gray-50 text-gray-900 flex flex-col items-center")

        // Header
        div {
            className = ClassName("w-full bg-white shadow-sm p-4 flex justify-between items-center")
            h1 {
                className = ClassName("text-xl font-bold")
                +"Munich Discovery"
            }
            button {
                className = ClassName("px-3 py-1 bg-gray-200 rounded-lg")
                +"Favorites"
            }
        }

        // Search bar
        div {
            className = ClassName("w-full max-w-3xl p-4")
            input {
                className = ClassName("w-full p-3 bg-white rounded-xl shadow-sm border border-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500")
                placeholder = "Search in Munich…"
                value = searchQuery
                onChange = { event ->
                    searchQuery = event.target.value
                    currentPage = 1 // Reset to first page on search
                }
            }
        }

        // Category row
        div {
            className = ClassName("w-full max-w-3xl px-4 pb-2")
            
            div {
                className = ClassName("flex gap-3 overflow-x-auto")
                categories.forEach { cat ->
                    div {
                        key = cat
                        val isActive = selectedCategory == cat
                        className = ClassName(
                            "px-4 py-2 border rounded-full shadow-sm cursor-pointer whitespace-nowrap transition-colors " +
                            if (isActive) 
                                "bg-blue-500 text-white border-blue-500 font-semibold" 
                            else 
                                "bg-white text-gray-700 border-gray-200 hover:bg-gray-50"
                        )
                        onClick = {
                            selectedCategory = if (isActive) null else cat
                            currentPage = 1 // Reset to first page on category change
                        }
                        +cat
                    }
                }
            }
            
            // Show active filters
            if (searchQuery.isNotBlank() || selectedCategory != null) {
                div {
                    className = ClassName("flex items-center gap-2 mt-3")
                    span {
                        className = ClassName("text-sm text-gray-600")
                        +"Filters:"
                    }
                    if (searchQuery.isNotBlank()) {
                        div {
                            className = ClassName("text-sm bg-blue-100 text-blue-800 px-3 py-1 rounded-full")
                            +"Search: \"$searchQuery\""
                        }
                    }
                    if (selectedCategory != null) {
                        div {
                            className = ClassName("text-sm bg-blue-100 text-blue-800 px-3 py-1 rounded-full")
                            +"Category: $selectedCategory"
                        }
                    }
                    button {
                        className = ClassName("text-sm text-blue-600 hover:text-blue-800 underline")
                        onClick = {
                            searchQuery = ""
                            selectedCategory = null
                            currentPage = 1
                        }
                        +"Clear all"
                    }
                }
            }
        }
        
        // Loading/Error states
        when {
            loading -> {
                div {
                    className = ClassName("w-full max-w-3xl p-8 text-center")
                    p {
                        className = ClassName("text-lg text-gray-600")
                        +"Loading venues..."
                    }
                }
            }
            error != null -> {
                div {
                    className = ClassName("w-full max-w-3xl p-8 text-center")
                    p {
                        className = ClassName("text-lg text-red-600")
                        +error!!
                    }
                }
            }
            venues.isEmpty() -> {
                div {
                    className = ClassName("w-full max-w-3xl p-8 text-center")
                    p {
                        className = ClassName("text-lg text-gray-600")
                        +"No venues found. Try refreshing the data."
                    }
                }
            }
            else -> {
                // Map + Featured section
                div {
                    className = ClassName("w-full max-w-5xl grid grid-cols-1 md:grid-cols-2 gap-6 p-4")

                    // Map preview
                    div {
                        className = ClassName("w-full h-64 rounded-xl bg-gradient-to-b from-blue-200 to-indigo-300 flex items-center justify-center")
                        span {
                            className = ClassName("text-lg font-semibold")
                            +"Map Preview"
                        }
                    }

                    // Featured
                    div {
                        className = ClassName("flex flex-col gap-4")
                        h2 {
                            className = ClassName("text-lg font-bold")
                            +"Featured"
                        }
                        venues.take(2).forEach { v ->
                            VenueCard {
                                venue = v
                            }
                        }
                    }
                }

                // Nearby section
                div {
                    className = ClassName("w-full max-w-3xl p-4")
                    h2 {
                        className = ClassName("text-lg font-bold mb-2")
                        val headerText = when {
                            searchQuery.isNotBlank() && selectedCategory != null -> 
                                "Results for \"$searchQuery\" in $selectedCategory ($totalCount found)"
                            searchQuery.isNotBlank() -> 
                                "Results for \"$searchQuery\" ($totalCount found)"
                            selectedCategory != null -> 
                                "$selectedCategory Places ($totalCount found)"
                            else -> 
                                "All Places ($totalCount total)"
                        }
                        +headerText
                    }
                    div {
                        className = ClassName("flex flex-col gap-4")
                        venues.forEach { v ->
                            VenueCard {
                                venue = v
                            }
                        }
                    }
                    
                    // Pagination controls
                    if (totalPages > 1) {
                        div {
                            className = ClassName("flex justify-center items-center gap-2 mt-6 pb-4")
                            
                            // Previous button
                            button {
                                className = ClassName(
                                    "px-4 py-2 rounded-lg font-medium " +
                                    if (currentPage > 1) 
                                        "bg-blue-500 text-white hover:bg-blue-600 cursor-pointer" 
                                    else 
                                        "bg-gray-200 text-gray-400 cursor-not-allowed"
                                )
                                disabled = currentPage <= 1
                                onClick = {
                                    if (currentPage > 1) {
                                        currentPage -= 1
                                    }
                                }
                                +"Previous"
                            }
                            
                            // Page numbers
                            span {
                                className = ClassName("px-4 py-2 text-gray-700")
                                +"Page $currentPage of $totalPages"
                            }
                            
                            // Next button
                            button {
                                className = ClassName(
                                    "px-4 py-2 rounded-lg font-medium " +
                                    if (currentPage < totalPages) 
                                        "bg-blue-500 text-white hover:bg-blue-600 cursor-pointer" 
                                    else 
                                        "bg-gray-200 text-gray-400 cursor-not-allowed"
                                )
                                disabled = currentPage >= totalPages
                                onClick = {
                                    if (currentPage < totalPages) {
                                        currentPage += 1
                                    }
                                }
                                +"Next"
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------- Components -------------------------

val VenueCard = FC<VenueCardProps> { props ->
    div {
        className = ClassName("w-full bg-white rounded-xl shadow-md flex overflow-hidden hover:shadow-lg transition-shadow")

        div {
            className = ClassName("w-32 bg-gradient-to-br from-blue-400 to-indigo-500 flex-shrink-0 flex items-center justify-center")
            span {
                className = ClassName("text-white text-3xl font-bold")
                +props.venue.category.take(1)
            }
        }

        div {
            className = ClassName("p-4 flex flex-col justify-between flex-grow")
            div {
                h3 {
                    className = ClassName("font-bold text-lg text-gray-900")
                    +props.venue.name
                }
                p {
                    className = ClassName("text-sm text-gray-500 mt-1")
                    +props.venue.category
                }
                p {
                    className = ClassName("text-sm text-gray-600 mt-2")
                    val address = buildString {
                        if (!props.venue.street.isNullOrBlank()) {
                            append(props.venue.street)
                            if (!props.venue.houseNumber.isNullOrBlank()) {
                                append(" ")
                                append(props.venue.houseNumber)
                            }
                        } else {
                            append("📍 Address not available")
                        }
                    }
                    +address
                }
            }
            props.venue.description?.let { desc ->
                div {
                    className = ClassName("text-sm text-gray-700 mt-2 line-clamp-2")
                    +desc
                }
            }
        }
    }
}

external interface VenueCardProps : Props {
    var venue: ApiVenue
}

