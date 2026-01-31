package com.apka.terminarzkliniki.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Encja Room = jedna wizyta w bazie danych.
@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimeMillis: Long,
    val petName: String,
    val species: String,
    val ownerName: String,
    val ownerPhone: String,
    val vetName: String,
    val notes: String,
    val remindEnabled: Boolean,
    val remindMinutes: Int,
    val isArchived: Boolean
)
