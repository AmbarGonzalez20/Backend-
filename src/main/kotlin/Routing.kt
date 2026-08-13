package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.io.File
import java.time.Instant
import io.ktor.utils.io.toByteArray

private fun ResultRow.toBache(): Bache {
    return Bache(
        id = this[BachesTable.id].value,
        descripcion = this[BachesTable.descripcion],
        latitud = this[BachesTable.latitud],
        longitud = this[BachesTable.longitud],
        fotoUrl = this[BachesTable.fotoUrl],
        fechaReporte = this[BachesTable.fechaReporte],
        estado = this[BachesTable.estado],
        usuarioId = this[BachesTable.usuarioId],
        fotoResolucionUrl = this[BachesTable.fotoResolucionUrl],
        comentarioResolucion = this[BachesTable.comentarioResolucion],
        fechaResolucion = this[BachesTable.fechaResolucion]
    )
}

private fun obtenerCarpetaUploads(): File {
    val rutaVolumenRailway = System.getenv("RAILWAY_VOLUME_MOUNT_PATH")

    val carpeta = if (!rutaVolumenRailway.isNullOrBlank()) {
        File(rutaVolumenRailway)
    } else {
        File("uploads")
    }

    if (!carpeta.exists()) {
        carpeta.mkdirs()
    }

    return carpeta
}

fun Application.configureRouting() {
    routing {

        // PRUEBA DE CONEXIÓN
        get("/") {
            call.respondText("Conexion exitosa con Railway")
        }

        // ============================================================
        // BACHES
        // ============================================================

        // GET /api/baches
        get("/api/baches") {
            val lista = transaction {
                BachesTable
                    .selectAll()
                    .toList()
                    .map { row ->
                        row.toBache()
                    }
            }

            call.respond(lista)
        }

        // POST /api/baches
        post("/api/baches") {
            val nuevo = call.receive<CrearBacheRequest>()

            if (nuevo.descripcion.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "La descripcion es obligatoria"
                )
                return@post
            }

            val fechaReporte = Instant.now().toString()

            val bacheCreado = transaction {
                val newId = BachesTable.insertAndGetId { row ->
                    row[BachesTable.descripcion] = nuevo.descripcion.trim()
                    row[BachesTable.latitud] = nuevo.latitud
                    row[BachesTable.longitud] = nuevo.longitud
                    row[BachesTable.fotoUrl] = nuevo.fotoUrl
                    row[BachesTable.fechaReporte] = fechaReporte
                    row[BachesTable.estado] = "pendiente"
                    row[BachesTable.usuarioId] = nuevo.usuarioId
                    row[BachesTable.fotoResolucionUrl] = ""
                    row[BachesTable.comentarioResolucion] = ""
                    row[BachesTable.fechaResolucion] = ""
                }

                Bache(
                    id = newId.value,
                    descripcion = nuevo.descripcion.trim(),
                    latitud = nuevo.latitud,
                    longitud = nuevo.longitud,
                    fotoUrl = nuevo.fotoUrl,
                    fechaReporte = fechaReporte,
                    estado = "pendiente",
                    usuarioId = nuevo.usuarioId,
                    fotoResolucionUrl = "",
                    comentarioResolucion = "",
                    fechaResolucion = ""
                )
            }

            call.respond(
                HttpStatusCode.Created,
                bacheCreado
            )
        }

        // GET /api/baches/{id}
        get("/api/baches/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "ID invalido"
                )
                return@get
            }

            val bache = transaction {
                BachesTable
                    .selectAll()
                    .toList()
                    .firstOrNull { row ->
                        row[BachesTable.id].value == id
                    }
                    ?.toBache()
            }

            if (bache == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    "Bache no encontrado"
                )
            } else {
                call.respond(bache)
            }
        }

        // DELETE /api/baches/{id}
        delete("/api/baches/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "ID invalido"
                )
                return@delete
            }

            val eliminado = transaction {
                BachesTable.deleteWhere {
                    BachesTable.id eq id
                } > 0
            }

            if (eliminado) {
                call.respond(
                    HttpStatusCode.OK,
                    "Bache eliminado correctamente"
                )
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    "Bache no encontrado"
                )
            }
        }

        // PUT /api/baches/{id}/estado
        //
        // Se mantienen los tres estados para no romper funcionalidad
        // existente. Acepta tanto "en proceso" como "en_proceso",
        // pero siempre guarda "en_proceso".
        put("/api/baches/{id}/estado") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "ID invalido"
                )
                return@put
            }

            val request = call.receive<ActualizarEstadoRequest>()

            val estadoNormalizado = request.estado
                .trim()
                .lowercase()
                .replace(" ", "_")

            val estadosValidos = setOf(
                "pendiente",
                "en_proceso",
                "resuelto"
            )

            if (estadoNormalizado !in estadosValidos) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Estado invalido. Usa: pendiente, en_proceso o resuelto"
                )
                return@put
            }

            val actualizado = transaction {
                BachesTable.update(
                    {
                        BachesTable.id eq id
                    }
                ) { row ->
                    row[BachesTable.estado] = estadoNormalizado

                    // Si el reporte deja de estar resuelto,
                    // se limpia la evidencia anterior.
                    if (estadoNormalizado != "resuelto") {
                        row[BachesTable.fotoResolucionUrl] = ""
                        row[BachesTable.comentarioResolucion] = ""
                        row[BachesTable.fechaResolucion] = ""
                    }
                } > 0
            }

            if (actualizado) {
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "mensaje" to "Estado actualizado correctamente",
                        "estado" to estadoNormalizado
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    "Bache no encontrado"
                )
            }
        }

        // PUT /api/baches/{id}/resolver
        //
        // Este es el endpoint que debe usar el frontend cuando el
        // administrador marque un reporte como RESUELTO.
        //
        // Requiere fotografía de evidencia y permite comentario opcional.
        put("/api/baches/{id}/resolver") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "ID invalido"
                )
                return@put
            }

            val request = call.receive<ResolverBacheRequest>()

            if (request.fotoResolucionUrl.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "La fotografia de resolucion es obligatoria"
                )
                return@put
            }

            val fechaResolucion = Instant.now().toString()

            val bacheActualizado = transaction {
                val actualizado = BachesTable.update(
                    where = {
                        BachesTable.id eq id
                    }
                ) { row ->
                    row[BachesTable.estado] = "resuelto"
                    row[BachesTable.fotoResolucionUrl] =
                        request.fotoResolucionUrl.trim()
                    row[BachesTable.comentarioResolucion] =
                        request.comentarioResolucion.trim()
                    row[BachesTable.fechaResolucion] =
                        fechaResolucion
                } > 0

                if (!actualizado) {
                    null
                } else {
                    BachesTable
                        .selectAll()
                        .toList()
                        .firstOrNull { row ->
                            row[BachesTable.id].value == id
                        }
                        ?.toBache()
                }
            }

            if (bacheActualizado == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    "Bache no encontrado"
                )
            } else {
                call.respond(
                    HttpStatusCode.OK,
                    bacheActualizado
                )
            }
        }

        // ============================================================
        // ARCHIVOS / FOTOGRAFÍAS
        // ============================================================

        // POST /api/upload
        post("/api/upload") {
            var nombreArchivo = ""
            val carpeta = obtenerCarpetaUploads()

            val multipart = call.receiveMultipart()
            var part = multipart.readPart()

            while (part != null) {
                try {
                    if (part is PartData.FileItem) {
                        nombreArchivo = "${System.currentTimeMillis()}.jpg"
                        val archivo = File(carpeta, nombreArchivo)
                        archivo.writeBytes(part.provider().toByteArray())
                    }
                } finally {
                    part.release()
                }
                part = multipart.readPart()
            }

            if (nombreArchivo.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Sin archivo")
            } else {
                call.respond(mapOf("url" to "/uploads/$nombreArchivo"))
            }
        }

        // GET /uploads/{archivo}
        get("/uploads/{archivo}") {
            val nombreRecibido =
                call.parameters["archivo"]

            if (nombreRecibido.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Archivo no especificado"
                )
                return@get
            }

            // Evita rutas del tipo ../../archivo
            val nombreArchivo =
                File(nombreRecibido).name

            val archivo = File(
                obtenerCarpetaUploads(),
                nombreArchivo
            )

            if (archivo.exists() && archivo.isFile) {
                call.respondFile(archivo)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    "Archivo no encontrado"
                )
            }
        }

        // ============================================================
        // USUARIOS
        // ============================================================

        // POST /api/registro
        post("/api/registro") {
            val usuario = call.receive<Usuario>()

            val existe = transaction {
                UsuariosTable
                    .selectAll()
                    .toList()
                    .any { row ->
                        row[UsuariosTable.email] == usuario.email
                    }
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

            call.respond(
                HttpStatusCode.Created,
                usuarioCreado
            )
        }

        // POST /api/login
        post("/api/login") {
            val request = call.receive<LoginRequest>()

            val usuario = transaction {
                UsuariosTable
                    .selectAll()
                    .toList()
                    .firstOrNull { row ->
                        row[UsuariosTable.email] == request.email &&
                                row[UsuariosTable.password] == request.password
                    }
                    ?.let { row ->
                        LoginResponse(
                            id = row[UsuariosTable.id].value,
                            nombre = row[UsuariosTable.nombre],
                            email = row[UsuariosTable.email],
                            rol = row[UsuariosTable.rol],
                            mensaje = "Login exitoso"
                        )
                    }
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

        // GET /api/usuarios
        get("/api/usuarios") {
            val lista = transaction {
                UsuariosTable
                    .selectAll()
                    .toList()
                    .map { row ->
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
