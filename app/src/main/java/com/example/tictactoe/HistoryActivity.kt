package com.example.tictactoe

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tictactoe.datastore.AppDatabase
import com.example.tictactoe.datastore.GameDataAdapter
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryActivity:BaseActivity(){

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GameDataAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        val inflater = layoutInflater
        val historyLayout = inflater.inflate(R.layout.activity_history, null)

        // Find the FrameLayout to add the game UI
        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        contentFrame.addView(historyLayout)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "tictactoe-db"
        ).build()

        CoroutineScope(Dispatchers.IO).launch {
            val gameDataList = db.gameDataDao().getAllGameData().reversed()
            launch(Dispatchers.Main) {
                adapter = GameDataAdapter(gameDataList)
                recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        }
    }
}