package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureRouting() {
    routing {

        get("/") {
            call.respondText("Conexion exitosa con Railway")
        }

        // GET todos los baches
        get("/api/baches") {
            val lista = transaction {
                BachesTable.selectAll().map {
                    Bache(
                        id = it[BachesTable.id],
                        descripcion = it[BachesTable.descripcion],
                        latitud = it[BachesTable.latitud],
                        longitud = it[BachesTable.longitud],
                        fotoUrl = it[BachesTable.fotoUrl],
                        fechaReporte = it[BachesTable.fechaReporte]
                    )
                }
            }
            call.respond(lista)
        }

        // POST crear bache
        post("/api/baches") {
            val nuevo = call.receive<Bache>()
            val id = transaction {
                BachesTable.insertAndGetId {
                    it[descripcion] = nuevo.descripcion
                    it[latitud] = nuevo.latitud
                    it[longitud] = nuevo.longitud
                    it[fotoUrl] = nuevo.fotoUrl
                    it[fechaReporte] = nuevo.fechaReporte
                }
            }
            call.respond(HttpStatusCode.Created, nuevo.copy(id = id.value))
        }

        // GET bache por ID
        get("/api/baches/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "ID invalido"
                )
            val bache = transaction {
                BachesTable.selectAll()
                    .where { BachesTable.id eq id }
                    .map {
                        Bache(
                            id = it[BachesTable.id],
                            descripcion = it[BachesTable.descripcion],
                            latitud = it[BachesTable.latitud],
                            longitud = it[BachesTable.longitud],
                            fotoUrl = it[BachesTable.fotoUrl],
                            fechaReporte = it[BachesTable.fechaReporte]
                        )
                    }.firstOrNull()
            }
            if (bache == null) {
                call.respond(HttpStatusCode.NotFound, "No encontrado")
            } else {
                call.respond(bache)
            }
        }
    }
}