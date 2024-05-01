package com.bhm.demo.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Door(
    @PrimaryKey(autoGenerate = true) var localId: Int = 0,
    val mac: String = "",
    val deviceName: String = "",
    val name: String = ""
)