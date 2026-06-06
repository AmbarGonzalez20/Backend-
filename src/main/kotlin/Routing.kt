package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val baches = mutableListOf<Bache>()
var contadorId = 1

fun Application.configureRouting() {
    routing {

        get("/") {
            call.respondText("Conexion exitosa con Railway")
        }

        get("/api/baches") {
            call.respond(baches)
        }

        post("/api/baches") {
            val nuevo = call.receive<Bache>()
            val conId = nuevo.copy(id = contadorId++)
            baches.add(conId)
            call.respond(HttpStatusCode.Created, conId)
        }

        get("/api/baches/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "ID invalido"
                )
            val bache = baches.find { it.id == id }
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    "No encontrado"
                )
            call.respond(bache)
        }
    }
}