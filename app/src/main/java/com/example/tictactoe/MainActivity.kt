package com.example.tictactoe

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.tictactoe.datastore.AppDatabase
import com.example.tictactoe.datastore.GameData
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Date
import java.util.UUID
import kotlin.random.Random
import android.content.Context

class MainActivity : BaseActivity() {
    var board = Array(3) { IntArray(3) { -1 } } // -1 means unmarked
    var currentPlayer = 0 // 0 for player 'X', 1 for player 'O'
    var gameActive = true
    var mutableList = mutableListOf<Int>(0,1,2,3,4,5,6,7,8)
    var movements = mutableListOf<Int>()
    var gameLevel = "easy"
    val winningPositions = arrayOf(
        // Rows
        arrayOf(0, 0, 0, 1, 0, 2),
        arrayOf(1, 0, 1, 1, 1, 2),
        arrayOf(2, 0, 2, 1, 2, 2),

        // Columns
        arrayOf(0, 0, 1, 0, 2, 0),
        arrayOf(0, 1, 1, 1, 2, 1),
        arrayOf(0, 2, 1, 2, 2, 2),

        // Diagonals
        arrayOf(0, 0, 1, 1, 2, 2),
        arrayOf(0, 2, 1, 1, 2, 0)
    )

    var isAI = true
    var currentGameMode = "single"

    lateinit var singleButton:Button
    lateinit var doubleButton:Button
    lateinit var doubleBluetoothButton:Button

    private lateinit var db: AppDatabase

    private lateinit var adapter: ArrayAdapter<String>
    private val discoveredDevices = mutableListOf<String>()

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream
    private var isPlayerTurn = true // Track whose turn it is
    private var isServer = false    // Track if this device is hosting
    private var foundPlayer1 = false
    lateinit var hostButton: Button
    lateinit var joinButton: Button
    private var whoGoesFirstDialog: AlertDialog? = null

    private val MY_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    private val enableBluetoothLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_CANCELED) {
            // User denied enabling Bluetooth
            Toast.makeText(
                this@MainActivity,
                "Bluetooth enabling denied",
                Toast.LENGTH_SHORT
            ).show()
        } else {

            Toast.makeText(
                this@MainActivity,
                "Bluetooth enabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "tictactoe-db"
        ).build()

        val file = File(cacheDir, "game_cache.json")

        if(!file.exists()){
            val jsonObject1 = JSONObject()
            val boardStateJson1 = convertToJSONArray(board)
            jsonObject1.put("boardState", boardStateJson1)
            jsonObject1.put("currentPlayer", currentPlayer)
            jsonObject1.put("gameActive", gameActive)
            jsonObject1.put("mutableList", JSONArray(mutableList.map{it}))
            jsonObject1.put("gameLevel", gameLevel)
            jsonObject1.put("currentGameMode", currentGameMode)
            file.writeText(jsonObject1.toString())
        }

        Log.d("Testing", "Running")

        // Inflate the game layout
        val inflater = layoutInflater
        val gameLayout = inflater.inflate(R.layout.activity_main, null)

        // Find the FrameLayout to add the game UI
        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)

        //Changing Game Mode
        val modeLayout = gameLayout.findViewById<LinearLayout>(R.id.mode_layout)
        initializeButtonColors(modeLayout, file)
        setupButtonListeners(modeLayout, file)
        singleButton = modeLayout.findViewById(R.id.single_player)
        doubleButton = modeLayout.findViewById(R.id.two_player_local)
        doubleBluetoothButton = modeLayout.findViewById(R.id.two_player_bluetooth)

        // Set up the grid layout and reset button
        val gridLayout = gameLayout.findViewById<GridLayout>(R.id.gridLayout)
        for (i in 0 until gridLayout.childCount) {
            val button = gridLayout.getChildAt(i) as Button
            button.setOnClickListener {
                Log.d("BUTTON CLICKED NUMBER", ""+button.tag.toString())
                gamePlayAlgorithm(it, isAI, gameLevel, gridLayout)

                val cachedData = file.readText()
                val jsonObject = JSONObject(cachedData)

                val boardStateJson = convertToJSONArray(board)
                jsonObject.put("boardState", boardStateJson)
                jsonObject.put("currentPlayer", currentPlayer)
                jsonObject.put("gameActive", gameActive)
                jsonObject.put("mutableList", JSONArray(mutableList.map{it}))
                jsonObject.put("gameLevel", gameLevel)
                jsonObject.put("currentGameMode", currentGameMode)
                file.writeText(jsonObject.toString())

                modifySingleDoubleButtonEnableDisable()

            }
        }

        val resetButton: Button = gameLayout.findViewById(R.id.resetButton)
        resetButton.setOnClickListener {
            resetGame()
            modifySingleDoubleButtonEnableDisable()
        }

        // Add the game layout to the content frame of the base layout
        contentFrame.addView(gameLayout)

        if(file.exists()){
            val cachedData = file.readText()
            Log.d("CacheData", cachedData)
            updateUI()
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        hostButton= gameLayout.findViewById<Button>(R.id.hostButton)
        joinButton= findViewById(R.id.joinButton)

        // Start the server when "Host Game" is clicked
        hostButton.setOnClickListener {
            if (bluetoothAdapter.isEnabled) {
                startServer()
                setupGameButtons(gridLayout)
                resetButton.setOnClickListener {
                    resetGame()
                    sendMove(-1)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // Request the permission
                        ActivityCompat.requestPermissions(
                            this@MainActivity, arrayOf<String>(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ), 786
                        )
                    } else {
                        requestBluetoothEnable()
                    }
                } else {
                    requestBluetoothEnable()
                }
            }
        }

        // Join the game as a client when "Join Game" is clicked
        joinButton.setOnClickListener {
            if (bluetoothAdapter.isEnabled) {
                showDeviceList()
                setupGameButtons(gridLayout)
                resetButton.setOnClickListener {
                    resetGame()
                    sendMove(-1)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // Request the permission
                        ActivityCompat.requestPermissions(
                            this@MainActivity, arrayOf<String>(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ), 786
                        )
                    } else {
                        requestBluetoothEnable()
                    }
                } else {
                    requestBluetoothEnable()
                }
            }
        }
        hostButton.isEnabled = false
        joinButton.isEnabled = false
    }

    // Request Bluetooth to be enabled if it isn't already
    private fun requestBluetoothEnable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        enableBtIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        enableBluetoothLauncher.launch(enableBtIntent)
    }


    // Set up Tic-Tac-Toe grid buttons
    private fun setupGameButtons(gameGrid: GridLayout) {
        for (i in 0 until gameGrid.childCount) {
            val button = gameGrid.getChildAt(i) as Button
            button.setOnClickListener {
                if (gameActive && isPlayerTurn && button.text.isEmpty()) {
                    button.text = if (isServer) "X" else "O"// Calculate row and column for player 1 (optional if needed later)
                    if(isServer) {
                        button.setTextColor(Color.RED)
                    } else {
                        button.setTextColor(Color.BLUE)
                    }
                    val row = i / 3
                    val col = i % 3
                    // Remove the selected tag (position) from mutableList
                    mutableList.remove(i)

                    if (board[row][col] == -1) {
                        // Update the board
                        board[row][col] = if (isServer) 1 else 0
                    }
                    runOnUiThread {
                        sendMove(i)
                    }
                    isPlayerTurn = false
                }
            }
        }
    }

    // Start the Bluetooth server to listen for connections
    private fun startServer() {
        val server = BluetoothServer(bluetoothAdapter, this) { socket ->
            onConnected(socket)
        }
        Toast.makeText(this, "Waiting for a player to join", Toast.LENGTH_SHORT).show()
        server.startListening()
        isServer = true
    }

    // Show a list of paired devices for the user to connect
    private fun showDeviceList() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        showDeviceListDialog()

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices?.forEach {
            if(it.name!=null)
            {
                updateDeviceList(it.name + "\n"+it.address)
            }
        }
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        if (bluetoothAdapter.startDiscovery()) {
            Toast.makeText(applicationContext, "Discovering peers", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                applicationContext,
                "Please enable GPS location of your device.",
                Toast.LENGTH_SHORT
            ).show()
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
    }

    fun updateDeviceList(deviceName: String) {
        // Add the device to the list
        discoveredDevices.add(deviceName)

        // Notify the adapter that the data has changed to refresh the ListView
        adapter.notifyDataSetChanged()
    }

    private fun clearDeviceList() {
        // Add the device to the list
        discoveredDevices.clear()

        // Notify the adapter that the data has changed to refresh the ListView
        adapter.notifyDataSetChanged()
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                device?.let {
                    // Add the discovered device to the list
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    if(it.name!=null)
                        updateDeviceList(it.name+"\n"+it.address)
                }
            }
        }
    }

    private fun showDeviceListDialog() {

        // Inflate the custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_list_view, null)

        // Find the ListView in the custom layout
        val listView: ListView = dialogView.findViewById(R.id.device_list_view)

        // Create an ArrayAdapter to hold the device names
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, discoveredDevices)
        listView.adapter = adapter
        clearDeviceList()

        // Create the AlertDialog with the custom view
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select a device")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        // Set item click listener for the ListView
        listView.setOnItemClickListener { _, _, position, _ ->
            // Handle device selection here
            val selectedDevice = discoveredDevices[position]
            val deviceAddress = selectedDevice.substring(selectedDevice.length - 17)
            dialog.dismiss()
//                Intent intent = new Intent(GameModeSelectionActivity.this, BTGameLayoutActivity.class);
//                intent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
//                startActivity(intent);
            val selectedBluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
            // You can initiate connection to the selected device here
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@setOnItemClickListener
            }
            selectedBluetoothDevice.createBond();
            initiateConnection(selectedBluetoothDevice)
        }

        // Show the dialog
        dialog.show()
    }


    // Connect as a client to the selected device
    private fun initiateConnection(device: BluetoothDevice) {
        val client = BluetoothClient(device, this) { socket ->
            onConnected(socket)
        }
        client.connect()
        isServer = false
    }

    // Handle Bluetooth connection and start listening for moves
    private fun onConnected(socket: BluetoothSocket) {
        outputStream = socket.outputStream
        inputStream = socket.inputStream

        runOnUiThread {
            Toast.makeText(this@MainActivity, "Connected!", Toast.LENGTH_SHORT).show()
        }
        listenForMoves()
//        Log.d("listenchecker", "checking here sreeku")
        if (!foundPlayer1) {
            runOnUiThread {
                showWhoGoesFirstDialog(this@MainActivity)
            }
        }
    }

    fun showWhoGoesFirstDialog(context: Context) {
        Log.d("listenfor,oves", "hihihihi++++++++++++++++")
        whoGoesFirstDialog = AlertDialog.Builder(context)
            .setTitle("Who Goes First?")
            .setMessage("Select who will go first:")
            .setPositiveButton("ME") { _, _ ->
                Toast.makeText(context, "You start!", Toast.LENGTH_SHORT).show()  // "ME" selected (Player 1 goes first)
                sendMove(-2)
            }
            .setNegativeButton("OPPONENT") { _, _ ->
                Toast.makeText(context, "Opponent start!", Toast.LENGTH_SHORT).show()  // "OPPONENT" selected (Player 2 goes first)
                sendMove(-3)
            }
            .setCancelable(false)  // Prevent the dialog from being dismissed without a choice
            .create()

        whoGoesFirstDialog?.show()
    }

    private fun logBoard(board: Array<IntArray>) {
        for (row in board) {
            // Join the elements of each row into a single string
            val rowString = row.joinToString(separator = " | ") { it.toString() }
            Log.d("GameBoard", rowString)
        }
    }

    // Send a move to the opponent
    private fun sendMove(position: Int) {
        var draw = false
        var winner = " "
        var reset = false
        val gson = Gson()
        if (position == -1 ) {
            reset = true
        }
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val macAddress: String = bluetoothAdapter?.address ?: "Unavailable"


        logBoard(board)

        val currentPlayers = if(isServer) 1 else 0
        if(checkWinner(currentPlayers)) {
            Toast.makeText(this, "You Won!!", Toast.LENGTH_SHORT).show()
            gameActive = false
            winner = "ME"
            storeGameData("Bluetooth", if (isServer) "X" else "O")
        } else if (isBoardFull()) {
            Toast.makeText(this, "Draw!", Toast.LENGTH_SHORT).show()
            gameActive = false
            winner = " "
            draw = true
            storeGameData("Bluetooth", "Draw")
        }
        val message = "$position\n"



        // Create the JSON structure according to your specification
        val gameState = mapOf(
            "position" to position,
            "board" to board,
            "turn" to "",
            "winner" to winner,
            "draw" to draw,
            "connectionEstablished" to true,
            "reset" to reset
        )

        var metadata = mapOf(
            "choices" to listOf(
                mapOf("id" to "player1", "name" to "Player 1 MAC Address"),
                mapOf("id" to "player2", "name" to "Player 2 MAC Address")
            ),
            "miniGame" to mapOf(
                "player1Choice" to "Player 1 MAC Address",
                "player2Choice" to "Player 2 MAC Address"
            )
        )

        if(position == -2){
            isServer = true
            isPlayerTurn = true
            metadata = mapOf(
                "choices" to listOf(
                    mapOf("id" to "player1", "name" to macAddress),
                    mapOf("id" to "player2", "name" to "")
                ),
                "miniGame" to mapOf(
                    "player1Choice" to macAddress,
                    "player2Choice" to ""
                )
            )
        } else if (position == -3) {
            isServer = false
            isPlayerTurn = false
            metadata = mapOf(
                "choices" to listOf(
                    mapOf("id" to "player1", "name" to ""),
                    mapOf("id" to "player2", "name" to macAddress)
                ),
                "miniGame" to mapOf(
                    "player1Choice" to "",
                    "player2Choice" to macAddress
                )
            )
        }

        val jsonData = mapOf("gameState" to gameState, "metadata" to metadata)

        // Convert the map to a JSON string
        val jsonString = gson.toJson(jsonData)
        outputStream.write(jsonString.toByteArray())
    }

    // Listen for moves from the opponent
    private fun listenForMoves() {
        val buffer = ByteArray(1024)
        val inputStream = inputStream
        Thread {
            while (true) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    Log.d("listening","%%%*%*%*%*")

                    if (bytesRead > 0) {

                        val receivedMessage = String(buffer, 0, bytesRead).trim()
                        runOnUiThread {
                            receiveMove(receivedMessage)  // Process the received JSON
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show()
                    }
                    break
                }
            }
        }.start()
    }

    private fun dismissWhoGoesFirstDialog() {
        Log.d("dismisser", "000000000000000000000000000")
        whoGoesFirstDialog?.dismiss()  // Dismiss the dialog safely
        whoGoesFirstDialog = null  // Clear reference
        if(isPlayerTurn) {
            Toast.makeText(this, "You start!", Toast.LENGTH_SHORT)
                .show()  // "ME" selected (Player 1 goes first)
        } else {
            Toast.makeText(this, "Opponent starts!", Toast.LENGTH_SHORT)
                .show()  // "ME" selected (Player 1 goes first)
        }
    }

    private fun receiveMove(message: String) {
        try {
            val jsonObject = JSONObject(message)

            // Extract game state from the JSON
            val gameState = jsonObject.getJSONObject("gameState")
            val position = gameState.getInt("position")
            val boardArray = gameState.getJSONArray("board")
            val board = Array(3) { row ->
                Array(3) { col -> boardArray.getJSONArray(row).getString(col) }
            }
            val turn = gameState.getString("turn")
            val winner = gameState.getString("winner")
            val draw = gameState.getBoolean("draw")
            val reset = gameState.getBoolean("reset")

            val metadata =jsonObject.getJSONObject("metadata")
            val miniGame = metadata.getJSONObject("miniGame")
            if(position == -2 || position == -3) {
                if (miniGame.getString("player1Choice") == "") {
                    isPlayerTurn = true
                    isServer = true
                    dismissWhoGoesFirstDialog()
                } else {
                    isPlayerTurn = false
                    isServer = false
                    dismissWhoGoesFirstDialog()
                }
            }
            if (winner != " ") {
                Toast.makeText(this, "Opponent won.", Toast.LENGTH_SHORT).show()
                this.gameActive = false
                storeGameData("Bluetooth", "Opponent")
            } else if (draw) {
                Toast.makeText(this, "Draw!", Toast.LENGTH_SHORT).show()
                this.gameActive = false
                storeGameData("Bluetooth", "Draw")
            }

            if(position != -1 && position != -2) {
                val button = findViewById<Button>(
                    resources.getIdentifier("button$position", "id", packageName)
                )
                if (button.text.isEmpty()) {
                    button.text = if (isServer) "O" else "X"
                    if(isServer) {
                        button.setTextColor(Color.BLUE)
                    } else {
                        button.setTextColor(Color.RED)
                    }
                    val row = position / 3
                    val col = position % 3
                    // Remove the selected tag (position) from mutableList
                    mutableList.remove(position)

                    if (this.board[row][col] == -1) {
                        // Update the board
                        this.board[row][col] = if (isServer) 0 else 1

                        logBoard(this.board)
                    }
                    isPlayerTurn = true
                }
            }
            if (reset) {
                resetGame()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun initializeButtonColors(modeLayout: LinearLayout, file: File) {
        if(file.exists()){
            val cacheData = file.readText()
            val jsonObject = JSONObject(cacheData)
            currentGameMode = jsonObject.getString("currentGameMode").lowercase()
        }
        for (i in 0 until modeLayout.childCount) {
            val btn = modeLayout.getChildAt(i) as Button
            btn.setBackgroundColor(
                if (btn.text.toString().lowercase() == currentGameMode) {
                    Color.parseColor("#6375eb") // Selected level color
                } else {
                    Color.parseColor("#d98ff7") // Unselected level color
                }
            )
        }
    }

    private fun setupButtonListeners(modeLayout: LinearLayout, file: File) {
        for (i in 0 until modeLayout.childCount) {
            val btn = modeLayout.getChildAt(i) as Button
            btn.setOnClickListener {
                updateGameMode(btn, file, modeLayout)
            }
        }
    }

    private fun updateGameMode(selectedButton: Button, file: File, modeLayout: LinearLayout) {
        if (file.exists()) {
            // Step 1: Read the existing cached data
            val cachedData = file.readText()
            val jsonObject = JSONObject(cachedData)

            // Step 2: Update the game level in the JSON object
            jsonObject.put("currentGameMode", selectedButton.text.toString().lowercase())
            Log.d("STATUS", ""+selectedButton.text.toString())
            // Step 3: Write the updated JSON back to the file
            file.writeText(jsonObject.toString())
        }

        currentGameMode = selectedButton.text.toString().lowercase()

        // Update button colors after selection
        for (i in 0 until modeLayout.childCount) {
            val btn = modeLayout.getChildAt(i) as Button
            btn.setBackgroundColor(
                if (btn.text.toString().lowercase() == selectedButton.text.toString().lowercase()) {
                    Color.parseColor("#6375eb") // Selected button color
                } else {
                    Color.parseColor("#d98ff7") // Unselected button color
                }
            )
        }
        if(selectedButton.text.toString().lowercase()!="single"){
            isAI=false
        }
        else{
            isAI=true
        }
        if(currentGameMode != doubleBluetoothButton.text.toString().lowercase()) {
            hostButton.isEnabled = false
            joinButton.isEnabled = false
        }
        else
        {
            hostButton.isEnabled = true
            joinButton.isEnabled = true
        }
    }

    private fun convertToJSONArray(board: Array<IntArray>): JSONArray {
        val jsonArray = JSONArray()  // Create a parent JSONArray for the board

        // Iterate through the 2D array (board)
        for (row in board) {
            val rowArray = JSONArray()  // Create a new JSONArray for each row
            for (cell in row) {
                rowArray.put(cell)  // Add each cell to the row JSONArray
            }
            jsonArray.put(rowArray)  // Add the row JSONArray to the parent JSONArray
        }

        return jsonArray  // Return the complete JSONArray
    }


    private fun gamePlayAlgorithm(view:View, isAI: Boolean, difficultyMode: String, gridLayout: GridLayout){

        if (!gameActive) return

        // Initialize variables
        lateinit var button: Button
        var tag: Int = -1

        // Player 1 (human player)
        if (currentPlayer == 0) {
            button = view as Button
            tag = button.tag.toString().toInt()
            movements.add(tag)
            if(button.isEnabled)
            {
                button.setTextColor(Color.parseColor("#ff0000"))
                button.isEnabled = false;
                button.isClickable =false;
            }


            // Perform other actions like marking the button or updating the game board here
        }
        // Player 2 (AI or second player)
        else if (currentPlayer == 1) {
            if(isAI && currentGameMode=="single"){
                // Select a random available position from mutableList
                if (difficultyMode == "easy") {
                    val idx = Random.nextInt(mutableList.size)
                    tag = mutableList[idx]
                }

                // MinMax Algorithm
                if(difficultyMode == "hard"){
                    val (profit, bestMove) = getTheBestMove(mutableList, board, currentPlayer)
                    tag = bestMove
                }

                if(difficultyMode == "medium"){
                    val choice = Random.nextInt(1)
                    if(choice == 0){
                        val idx = Random.nextInt(mutableList.size)
                        tag = mutableList[idx]
                    }
                    else{
                        val (profit, bestMove) = getTheBestMove(mutableList, board, currentPlayer)
                        tag = bestMove
                    }
                }
            }
            else if (currentGameMode=="double"){
                button = view as Button
                tag = button.tag.toString().toInt()
                movements.add(tag)
            }

            val buttonId = resources.getIdentifier("button$tag", "id", packageName)
            button = findViewById(buttonId)
            if(button.isEnabled)
            {
                button.setTextColor(Color.parseColor("#0000ff"))
                button.isEnabled = false;
                button.isClickable = false;
            }
        }
        // Calculate row and column for player 1 (optional if needed later)
        val row = tag / 3
        val col = tag % 3
        // Remove the selected tag (position) from mutableList
        mutableList.remove(tag)

        if (board[row][col] == -1) {
            // Update the board
            board[row][col] = currentPlayer

            // Update the button text based on the current player
            button.text = if (currentPlayer == 0) "X" else "O"

            // Check if the current player won
            if (checkWinner(currentPlayer)) {
                gameActive = false
//                    Toast.makeText(this, "Player ${if (currentPlayer == 0) "X" else "O"} wins!", Toast.LENGTH_LONG).show()
                if(currentPlayer==0){
                    Toast.makeText(this, "Player X wins!", Toast.LENGTH_LONG).show()
                    if(currentGameMode=="single"){
                        storeGameData(gameLevel.replaceFirstChar(Char::uppercase), "X")
                    }
                    else{
                        storeGameData("Double", "X")
                    }
                }
                else if(!isAI){
                    if(currentGameMode=="single"){
                        storeGameData(gameLevel.replaceFirstChar(Char::uppercase), "O")
                    }
                    else{
                        storeGameData("Double", "O")
                    }
                    Toast.makeText(this, "Player O wins!", Toast.LENGTH_LONG).show()
                }
                else{
                    storeGameData(gameLevel.replaceFirstChar(Char::uppercase), "AI")
                    Toast.makeText(this, "AI wins!", Toast.LENGTH_LONG).show()
                }
                return
            } else if (isBoardFull()) {
                // If no winner and the board is full, it's a draw
                gameActive = false
                Toast.makeText(this, "It's a draw!", Toast.LENGTH_LONG).show()
                storeGameData(gameLevel.replaceFirstChar(Char::uppercase), "Draw")
                return
            } else {
                // Switch the current player
                currentPlayer = 1 - currentPlayer
                if(currentPlayer==1 && isAI){
                    gamePlayAlgorithm(view, isAI, difficultyMode, gridLayout)
                }
            }
        }
    }

    private fun getTheBestMove(
        ListOfAvailablePlaces: MutableList<Int>,
        tempBoard: Array<IntArray>,
        turn: Int,
        alpha: Int = Int.MIN_VALUE,
        beta: Int = Int.MAX_VALUE
    ): Pair<Int, Int> {

        // Check for any winning positions
        for (position in winningPositions) {
            // Check if there's a winning combination on the board for the current player
            if (tempBoard[position[0]][position[1]] == turn &&
                tempBoard[position[2]][position[3]] == turn &&
                tempBoard[position[4]][position[5]] == turn
            ) {
                // If player 0 wins, return 1 (profit for player 0)
                // If player 1 wins, return -1 (loss for player 0, profit for player 1)
                return if (turn == 0) Pair(1, -1) else Pair(-1, -1) // Return profit and invalid tag (-1)
            }
        }

        // If no available moves and no winner, return 0 (Draw)
        if (ListOfAvailablePlaces.isEmpty()) {
            return Pair(0, -1)
        }

        var bestResult = if (turn == 0) Int.MIN_VALUE else Int.MAX_VALUE
        var bestMove = -1
        var currentAlpha = alpha
        var currentBeta = beta

        for (i in ListOfAvailablePlaces.indices) {
            val tag = ListOfAvailablePlaces[i]
            val row = tag / 3
            val col = tag % 3

            val newBoard = tempBoard.map { it.clone() }.toTypedArray()
            newBoard[row][col] = turn

            // Make a copy of the available positions list
            val newList = ListOfAvailablePlaces.toMutableList()
            newList.removeAt(i)

            // Recursively calculate the profit for the opponent's turn
            val (result, _) = getTheBestMove(newList, newBoard, 1 - turn, alpha, beta)

            if (turn == 0) {
                if (result > bestResult) {
                    bestResult = result
                    bestMove = tag
                }
                // Alpha-beta pruning
                currentAlpha = maxOf(currentAlpha, bestResult)
            } else {
                if (result < bestResult) {
                    bestResult = result
                    bestMove = tag
                }
                // Alpha-beta pruning
                currentBeta = minOf(currentBeta, bestResult)
            }

            // Prune the search tree
            if (beta <= alpha) {
                break
            }
        }

        return Pair(bestResult, bestMove)
    }

    private fun checkWinner(currentPlayer: Int): Boolean {
        // Rows, columns, and diagonals to check
        val winningPositions = arrayOf(
            // Rows
            arrayOf(0, 0, 0, 1, 0, 2),
            arrayOf(1, 0, 1, 1, 1, 2),
            arrayOf(2, 0, 2, 1, 2, 2),

            // Columns
            arrayOf(0, 0, 1, 0, 2, 0),
            arrayOf(0, 1, 1, 1, 2, 1),
            arrayOf(0, 2, 1, 2, 2, 2),

            // Diagonals
            arrayOf(0, 0, 1, 1, 2, 2),
            arrayOf(0, 2, 1, 1, 2, 0)
        )

        // Check for winning condition
        for (position in winningPositions) {
            if (board[position[0]][position[1]] == currentPlayer &&
                board[position[2]][position[3]] == currentPlayer &&
                board[position[4]][position[5]] == currentPlayer) {
                return true
            }
        }
        return false
    }

    private fun isBoardFull(): Boolean {
        Log.d("Is board full", "**********")
        for (i in 0..2) {
            for (j in 0..2) {
                if (board[i][j] == -1) return false
            }
        }
        return true
    }

    private fun resetGame() {
        board = Array(3) { IntArray(3) { -1 } }
        currentPlayer = 0
        gameActive = true
        mutableList = mutableListOf<Int>(0,1,2,3,4,5,6,7,8)
//        currentGameMode = "single"

        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)
        for (i in 0 until gridLayout.childCount) {
            val button = gridLayout.getChildAt(i) as Button
            button.text = ""
            button.isEnabled = true
            button.isClickable = true
        }

        val file = File(cacheDir, "game_cache.json")
        if(file.exists()){
            val cachedData = file.readText()
            val jsonObject = JSONObject(cachedData)
            jsonObject.put("boardState", convertToJSONArray(Array(3) { IntArray(3) { -1 } }))
            jsonObject.put("currentPlayer", 0)
            jsonObject.put("gameActive", true)
            jsonObject.put("mutableList", JSONArray(mutableListOf<Int>(0,1,2,3,4,5,6,7,8)))
            jsonObject.put("gameLevel", gameLevel)
            jsonObject.put("currentGameMode", currentGameMode)
            file.writeText(jsonObject.toString())
        }
    }

    // Handle navigation icon clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI() {

        val file = File(cacheDir, "game_cache.json")
        if (!file.exists()) {
            return // No cached data to update, so just return
        }
        val cachedData = file.readText()
        val jsonObject = JSONObject(cachedData)
//        Log.d("Cached Data", jsonObject.getJSONArray("boardState").toString())

        board = jsonObject.getJSONArray("boardState").let { array ->
            Array(array.length()) { rowIndex ->
                val row = array.getJSONArray(rowIndex)
                IntArray(row.length()) { colIndex ->
                    row.getInt(colIndex)
                }
            }
        }
        currentPlayer = jsonObject.getInt("currentPlayer")
        gameActive = jsonObject.getBoolean("gameActive")
        mutableList = (jsonObject.getJSONArray("mutableList")).let { array ->
            MutableList(array.length()) { array.getInt(it) }
        }

        gameLevel = jsonObject.getString("gameLevel").lowercase()
        currentGameMode = jsonObject.getString("currentGameMode").lowercase()
        if(currentGameMode == "single"){
            isAI = true
        }
        else{
            isAI = false
        }
        Log.d("CURRENT GAME MODE", currentGameMode)

        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)
        for (i in 0 until gridLayout.childCount) {
            val button = gridLayout.getChildAt(i) as Button
            val tag = button.tag.toString().toInt()
            val row = tag / 3
            val col = tag % 3

            // Update the button based on the board state
            when (board[row][col]) {
                0 -> { // Player 'X'
                    button.text = "X"
                    button.setTextColor(Color.RED)
                    button.isEnabled = false
                    button.isClickable = false
                }
                1 -> { // Player 'O'
                    button.text = "O"
                    button.setTextColor(Color.BLUE)
                    button.isEnabled = false
                    button.isClickable = false
                }
                else -> { // Unmarked
                    button.text = ""
                }
            }
        }
        modifySingleDoubleButtonEnableDisable()
    }

    private fun modifySingleDoubleButtonEnableDisable(){
        if(mutableList.size!=9){
            if( currentGameMode.lowercase()=="single"){
                singleButton.isEnabled = true
                doubleButton.isEnabled = false
                doubleBluetoothButton.isEnabled = false
            }
            else if(currentGameMode.lowercase()=="double"){
                singleButton.isEnabled = false
                doubleButton.isEnabled = true
                doubleBluetoothButton.isEnabled = false
            } else {
                singleButton.isEnabled = false
                doubleButton.isEnabled = false
                doubleBluetoothButton.isEnabled = true
            }
        }
        else{
            singleButton.isEnabled = true
            doubleButton.isEnabled = true
            doubleBluetoothButton.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Delete the cache file when the app is closing
        val cacheFile = File(cacheDir, "game_cache.json")
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }

    private fun storeGameData(gameLevel: String, winner: String){

        val gameData = GameData(
            gameLevel = gameLevel,
            winner = winner,
            date = Date().time
        )
        CoroutineScope(Dispatchers.IO).launch {
            db.gameDataDao().insertGameData(gameData)
        }
    }
}