package com.example

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object BachesTable : IntIdTable("baches") {
    val descripcion = varchar("descripcion", 500)
    val latitud = double("latitud")
    val longitud = double("longitud")
    val fotoUrl = varchar("foto_url", 500).default("")
    val fechaReporte = varchar("fecha_reporte", 100).default("")
}

fun initDatabase() {
    val databaseUrl = System.getenv("DATABASE_URL")
        ?: throw IllegalStateException("DATABASE_URL no encontrada")

    // Parsear la URL de Railway: postgresql://user:password@host:port/dbname
    val uri = URI(databaseUrl)
    val userInfo = uri.userInfo.split(":")
    val user = userInfo[0]
    val password = userInfo[1]
    val host = uri.host
    val port = uri.port
    val dbName = uri.path.removePrefix("/")

    val jdbcUrl = "jdbc:postgresql://$host:$port/$dbName"

    Database.connect(
        url = jdbcUrl,
        driver = "org.postgresql.Driver",
        user = user,
        password = password
    )

    transaction {
        SchemaUtils.create(BachesTable)
    }
}