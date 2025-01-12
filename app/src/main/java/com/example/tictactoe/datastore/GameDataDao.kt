package com.example.tictactoe.datastore

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GameDataDao {
    @Insert
    suspend fun insertGameData(gameData: GameData)

    @Query("Select * from game_data")
    suspend fun getAllGameData(): List<GameData>
}