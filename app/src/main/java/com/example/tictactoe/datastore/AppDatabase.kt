package com.example.tictactoe.datastore

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GameData::class], version = 1)
abstract class AppDatabase: RoomDatabase(){
    abstract fun gameDataDao(): GameDataDao
}