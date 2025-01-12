package com.example.tictactoe.datastore

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_data")
data class GameData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gameLevel: String?,
    val winner: String?,
    val date: Long?
)
