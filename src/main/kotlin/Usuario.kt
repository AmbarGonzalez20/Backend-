package com.example

import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val id: Int = 0,
    val nombre: String,
    val email: String,
    val password: String = "",
    val rol: String = "ciudadano"
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val id: Int,
    val nombre: String,
    val email: String,
    val rol: String,
    val mensaje: String
)