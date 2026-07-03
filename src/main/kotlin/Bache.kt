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
    val estado: String = "pendiente"
)

@Serializable
data class ActualizarEstadoRequest(
    val estado: String
)