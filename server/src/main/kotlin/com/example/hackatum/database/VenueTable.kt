package com.example.hackatum.database

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object VenueTable : LongIdTable("venues") {
    val externalId: Column<String> = varchar("external_id", 50).uniqueIndex()
    val name: Column<String> = varchar("name", 500)
    val category: Column<String> = varchar("category", 100)
    val street: Column<String> = varchar("street", 500)
    val houseNumber: Column<String> = varchar("house_number", 50)
    val latitude: Column<Double> = double("latitude")
    val longitude: Column<Double> = double("longitude")
    val description: Column<String?> = text("description").nullable()
}
