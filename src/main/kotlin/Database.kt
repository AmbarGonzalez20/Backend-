package com.example

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object BachesTable : IntIdTable("baches") {
    val descripcion = varchar("descripcion", 500)
    val latitud = double("latitud")
    val longitud = double("longitud")
    val fotoUrl = varchar("foto_url", 500).default("")
    val fechaReporte = varchar("fecha_reporte", 100).default("")
    val estado = varchar("estado", 50).default("pendiente")
    val usuarioId = integer("usuario_id").default(0)

    val fotoResolucionUrl =
        varchar("foto_resolucion_url", 500).default("")

    val comentarioResolucion =
        varchar("comentario_resolucion", 1000).default("")

    val fechaResolucion =
        varchar("fecha_resolucion", 100).default("")
}

object UsuariosTable : IntIdTable("usuarios") {
    val nombre = varchar("nombre", 200)
    val email = varchar("email", 200).uniqueIndex()
    val password = varchar("password", 500)
    val rol = varchar("rol", 50).default("ciudadano")
}

fun initDatabase() {
    val databaseUrl = System.getenv("DATABASE_URL")
        ?: throw IllegalStateException("DATABASE_URL no encontrada")

    val uri = URI(databaseUrl)
    val userInfo = uri.userInfo?.split(":", limit = 2)
        ?: throw IllegalStateException("DATABASE_URL no contiene usuario y contraseña")

    if (userInfo.size < 2) {
        throw IllegalStateException("DATABASE_URL no contiene una contraseña válida")
    }

    val user = userInfo[0]
    val password = userInfo[1]
    val host = uri.host
        ?: throw IllegalStateException("DATABASE_URL no contiene host")
    val port = if (uri.port > 0) uri.port else 5432
    val dbName = uri.path.removePrefix("/")

    val jdbcUrl = "jdbc:postgresql://$host:$port/$dbName"

    Database.connect(
        url = jdbcUrl,
        driver = "org.postgresql.Driver",
        user = user,
        password = password
    )

    transaction {
        SchemaUtils.createMissingTablesAndColumns(BachesTable)
        SchemaUtils.createMissingTablesAndColumns(UsuariosTable)
    }
}
