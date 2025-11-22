package com.example.hackatum.database

import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val dotenv = dotenv()
        val driverClassName = "org.h2.Driver"
        val jdbcURL = dotenv["JDBC_DATABASE_URL"] ?: "jdbc:h2:file:./data/venues_db;DB_CLOSE_DELAY=-1"
        val database = Database.connect(jdbcURL, driverClassName)
        
        transaction(database) {
            SchemaUtils.create(VenueTable)
        }
    }
}
