package com.isosic.myapplication.models

import java.util.UUID

data class Bike(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)