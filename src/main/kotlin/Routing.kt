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

        // Prueba de conexion
        get("/") {
            call.respondText("Conexion exitosa con Railway")
        }

        // ── Baches ────────────────────────────────────────────

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
                    HttpStatusCode.BadRequest, "ID invalido"
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

        // ── Usuarios ───────────────────────────────────────────

        // POST /api/registro
        post("/api/registro") {
            val usuario = call.receive<Usuario>()

            // Verificar si el email ya existe
            val existe = transaction {
                UsuariosTable.selectAll().toList()
                    .any { row -> row[UsuariosTable.email] == usuario.email }
            }

            if (existe) {
                call.respond(
                    HttpStatusCode.Conflict,
                    "El correo ya esta registrado"
                )
                return@post
            }

            val usuarioCreado = transaction {
                val newId = UsuariosTable.insertAndGetId { row ->
                    row[UsuariosTable.nombre] = usuario.nombre
                    row[UsuariosTable.email] = usuario.email
                    row[UsuariosTable.password] = usuario.password
                    row[UsuariosTable.rol] = "ciudadano"
                }
                LoginResponse(
                    id = newId.value,
                    nombre = usuario.nombre,
                    email = usuario.email,
                    rol = "ciudadano",
                    mensaje = "Registro exitoso"
                )
            }
            call.respond(HttpStatusCode.Created, usuarioCreado)
        }

        // POST /api/login
        post("/api/login") {
            val request = call.receive<LoginRequest>()

            val usuario = transaction {
                UsuariosTable.selectAll().toList()
                    .filter { row ->
                        row[UsuariosTable.email] == request.email &&
                                row[UsuariosTable.password] == request.password
                    }
                    .map { row ->
                        LoginResponse(
                            id = row[UsuariosTable.id].value,
                            nombre = row[UsuariosTable.nombre],
                            email = row[UsuariosTable.email],
                            rol = row[UsuariosTable.rol],
                            mensaje = "Login exitoso"
                        )
                    }.firstOrNull()
            }

            if (usuario == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Credenciales incorrectas"
                )
            } else {
                call.respond(usuario)
            }
        }

        // GET /api/usuarios (solo para administrador)
        get("/api/usuarios") {
            val lista = transaction {
                UsuariosTable.selectAll().toList().map { row ->
                    Usuario(
                        id = row[UsuariosTable.id].value,
                        nombre = row[UsuariosTable.nombre],
                        email = row[UsuariosTable.email],
                        rol = row[UsuariosTable.rol]
                    )
                }
            }
            call.respond(lista)
        }
    }
}