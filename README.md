# CSE535: Project 2 

AI based TicTacToe Game Application: Group 16

## Overview
This mobile game application, developed for the CSE535 course, offers an engaging gaming experience with three modes, including AI play with varying difficulty levels, progress tracking, and friend competitions.

## Mobile App Screens
<p align="center">
<kbd><img src="https://github.com/user-attachments/assets/6d73315b-cbc3-4af8-bb2e-4c255f444f92" width="200"></kbd> &nbsp;
<kbd><img src="https://github.com/user-attachments/assets/00fffd76-d47c-4855-931d-6240cbaf3490" width="200"></kbd> &nbsp;
<kbd><img src="https://github.com/user-attachments/assets/b83a4fbe-9c0d-4897-aeed-2398dfa4e7f5" width="200"></kbd> &nbsp;
</p>
<p align="center">
<kbd><img src="https://github.com/user-attachments/assets/53c16326-aa50-41f7-a230-cdddea8dddad" width="200"></kbd> &nbsp;
<kbd><img src="https://github.com/user-attachments/assets/8c5992bb-14db-4228-89c9-e76cd1bc8a22" width="200"></kbd> &nbsp;
<kbd><img src="https://github.com/user-attachments/assets/61fbe6ed-e912-46e0-a2fe-674ec3abe815" width="200"></kbd> &nbsp;
</p>

## Demo
[![Demo Video of the AI based Tic Tac Toe Game](https://img.youtube.com/vi/39cvCMhZ-JE/0.jpg)](https://www.youtube.com/watch?v=39cvCMhZ-JE)



## MinMax Algorithm
The core development of the game, specifically the gameplay, is implemented using the MinMax Algorithm with Alpha-Beta Pruning to optimize the AI's moves against the human player. Additionally, various difficulty modes are introduced:

EASY: The AI selects positions entirely at random **(100% random)**.
MEDIUM: The AI makes a **random move 50%** of the time, and for the other **50%, it selects moves based on the MinMax tree**.
HARD: The AI always **(100% of the time) selects moves based on the MinMax tree**. In this case, the outcome can either be a tie or a loss for the human player.

## Features
- User-friendly Interface: Intuitive design for easy navigation.
- Difficulty Levels: Choose between Easy, Medium, and Hard difficulty settings to play against AI.
- History Tracking: View game history, including levels, winners, and dates.
- Real-time Updates: Dynamic updating of game.
- Responsive Design: Optimized for various screen sizes and orientations.
- Singleplayer mode: Play against Artificial Intelligence(AI) with 3 choice of difficulty EASY, MEDIUM, or HARD.
- Multiplayer mode: 2 players can have on-device play and two-device play over a BLUETOOTH.

## Screens
### 1. Game Screen - Gameplay
- **Start a New Game**: The user always makes the first move and plays as Player X.
- **AI Opponent**: The AI plays as Player O, with moves determined by the selected difficulty mode:
  - Optimal moves are chosen using the Minimax algorithm with alpha-beta pruning.
- **End Conditions**:
  - Game ends when a player wins (row, column, diagonal) or if a draw occurs.
  - An end-of-game message displays the result and congratulates the winner.
- **Switch Difficulty Mode**: Users can change the difficulty mode at any time.
- **Reset Game**: Users can reset the board to start a new game.

### 2. Settings Screen - Difficulty Modes
- **Easy**: The AI chooses random actions.
- **Medium**: The AI chooses random actions 50% of the time and optimal actions 50% of the time.
- **Hard**: The AI always chooses optimal actions.

### 3. Past Games Screen
- Displays a history of past games in the following format:

```
Date       | Winner   | Difficulty Mode
2024-01-01 | Human    | Hard
2024-01-02 | Computer | Medium
2024-01-03 | Human    | Easy
```

## JSON based communincation for the Bluetooth Implementation

```
{
  "gameState": {
    "board": [
      ["X", " ", " "],
      [" ", " ", " "],
      [" ", " ", " "]
    ],
    "turn": "1",
    "winner": " ",
    "draw": false,
    "connectionEstablished": true,
    "reset": false
  },
  "metadata": {
    "choices": [
      {
        "id": "player1",
        "name": "Player 1 MAC Address"
      },
      {
        "id": "player2",
        "name": "Player 2 MAC Address"
      }
    ],
    "miniGame": {
      "player1Choice": "Player 1 MAC Address",
      "player2Choice": "Player 1 MAC Address"
    }
  }
}
```


## Technology Stack
- Android Studio: Kotlin for development.
- RecyclerView: For displaying game history in a list format.
- XML: For layout designs of the Game Screen, History SCreen, and Difficulty Mode Screen.
- RoomDB: For data storage(Winner, Difficuty Mode, Date Played) and management.

## Installation
Clone the repository:
```bash
git clone https://github.com/sr33kar/tic-tac-toe.git
```
- Open the project in Android Studio.
- Run the application on an emulator(for single player or on-device multiplayer play) or a physical device(for single player or on-device/two-device multiplayer play).

## Usage
- Launch the app.
- Select a difficulty level to start a single-play game against AI in the settings section(easy selected by default).
- Track your game history in the history section.
- Compete with friends with double-play/bluetooth options and see who can achieve the highest score!

## Contribution
Current contributors: [Hartik Suhagiya](https://github.com/hartik123) | [Sarthak Patel](https://github.com/sarthak1208) | [Aanshi Patwari](https://github.com/aanshi18) | [Sreekar Gadasu](https://github.com/sr33kar) | Rohit Bathi | Gyan Pratipat

We welcome contributions! Please follow these steps to contribute:
- Fork the repository.
- Create a new branch (git checkout -b feature/YourFeature).
- Make your changes and commit them (git commit -m 'Add some feature').
- Push to the branch (git push origin feature/YourFeature).
- Open a Pull Request.

## Acknowledgments
Special thanks to the CSE535 course instructor, Professor Jaejong Baek, TAs Taha Shaheen and Animesh Singh, and peers for their support and collaboration.
