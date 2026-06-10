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

        get("/api/baches") {
            val lista = transaction {
                BachesTable.selectAll().toList().map { row ->
                    Bache(
                        id = row[BachesTable.id].value,
                        descripcion = row[BachesTable.descripcion],
                        latitud = row[BachesTable.latitud],
                        longitud = row[BachesTable.longitud],
                        fotoUrl = row[BachesTable.fotoUrl],
                        fechaReporte = row[BachesTable.fechaReporte]
                    )
                }
            }
            call.respond(lista)
        }

        post("/api/baches") {
            val nuevo = call.receive<Bache>()
            val bacheCreado = transaction {
                val newId = BachesTable.insertAndGetId { row ->
                    row[BachesTable.descripcion] = nuevo.descripcion
                    row[BachesTable.latitud] = nuevo.latitud
                    row[BachesTable.longitud] = nuevo.longitud
                    row[BachesTable.fotoUrl] = nuevo.fotoUrl
                    row[BachesTable.fechaReporte] = nuevo.fechaReporte
                }
                Bache(
                    id = newId.value,
                    descripcion = nuevo.descripcion,
                    latitud = nuevo.latitud,
                    longitud = nuevo.longitud,
                    fotoUrl = nuevo.fotoUrl,
                    fechaReporte = nuevo.fechaReporte
                )
            }
            call.respond(HttpStatusCode.Created, bacheCreado)
        }

        get("/api/baches/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "ID invalido"
                )
            val bache = transaction {
                BachesTable.selectAll().toList()
                    .filter { row -> row[BachesTable.id].value == id }
                    .map { row ->
                        Bache(
                            id = row[BachesTable.id].value,
                            descripcion = row[BachesTable.descripcion],
                            latitud = row[BachesTable.latitud],
                            longitud = row[BachesTable.longitud],
                            fotoUrl = row[BachesTable.fotoUrl],
                            fechaReporte = row[BachesTable.fechaReporte]
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