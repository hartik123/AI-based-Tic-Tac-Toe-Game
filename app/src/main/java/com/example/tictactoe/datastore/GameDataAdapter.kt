package com.example.tictactoe.datastore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tictactoe.R
import java.text.SimpleDateFormat
import java.util.*

class GameDataAdapter(private val gameDataList: List<GameData>) : RecyclerView.Adapter<GameDataAdapter.GameDataViewHolder>() {

    class GameDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gameLevelTextView: TextView = itemView.findViewById(R.id.text_game_level)
        val winnerTextView: TextView = itemView.findViewById(R.id.text_winner)
        val dateTextView: TextView = itemView.findViewById(R.id.text_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameDataViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_data, parent, false)
        return GameDataViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameDataViewHolder, position: Int) {
        val gameData = gameDataList[position]

        // Set game level
        holder.gameLevelTextView.text = gameData.gameLevel ?: "N/A"

        // Set winner
        holder.winnerTextView.text = gameData.winner ?: "N/A"

        // Format date or set a default message if null
        holder.dateTextView.text = gameData.date?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
        } ?: "N/A"
    }

    override fun getItemCount(): Int {
        return gameDataList.size
    }
}
