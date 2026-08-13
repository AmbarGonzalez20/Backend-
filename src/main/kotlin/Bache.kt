package com.example

import kotlinx.serialization.Serializable

@Serializable
data class Bache(
    val id: Int = 0,
    val descripcion: String,
    val latitud: Double,
    val longitud: Double,
    val fotoUrl: String = "",
    val fechaReporte: String = "",
    val estado: String = "pendiente",
    val usuarioId: Int = 0,
    val fotoResolucionUrl: String = "",
    val comentarioResolucion: String = "",
    val fechaResolucion: String = ""
)

@Serializable
data class CrearBacheRequest(
    val descripcion: String,
    val latitud: Double,
    val longitud: Double,
    val fotoUrl: String = "",
    val usuarioId: Int = 0
)

@Serializable
data class ActualizarEstadoRequest(
    val estado: String
)

@Serializable
data class ResolverBacheRequest(
    val estado: String = "resuelto",
    val fotoResolucionUrl: String,
    val comentarioResolucion: String = ""
)
