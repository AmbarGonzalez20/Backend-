package com.example

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object BachesTable : IntIdTable("baches") {
    val descripcion = varchar("descripcion", 500)
    val latitud = double("latitud")
    val longitud = double("longitud")
    val fotoUrl = varchar("foto_url", 500).default("")
    val fechaReporte = varchar("fecha_reporte", 100).default("")
}

fun initDatabase() {
    val databaseUrl = System.getenv("DATABASE_URL")
        ?: "jdbc:postgresql://localhost:5432/railway"

    val jdbcUrl = if (databaseUrl.startsWith("postgresql://")) {
        databaseUrl.replace("postgresql://", "jdbc:postgresql://")
    } else {
        databaseUrl
    }

    Database.connect(
        url = jdbcUrl,
        driver = "org.postgresql.Driver"
    )

    transaction {
        SchemaUtils.create(BachesTable)
    }
}