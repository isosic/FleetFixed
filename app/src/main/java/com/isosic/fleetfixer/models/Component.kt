package com.isosic.fleetfixer.models

import java.util.Date

interface Component{
    val name: String
    val notes: String
    val dateAdded: Date
}