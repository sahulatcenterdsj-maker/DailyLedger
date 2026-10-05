package com.sadique.dailyledger.auth

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val googleEmail: String? = null,
)
