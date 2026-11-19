package com.example.tiptime.ui

import androidx.lifecycle.ViewModel
import com.example.tiptime.engine.game.DoomGame

/** Conserva la partida del minijuego al girar la pantalla o cambiar de idioma. */
class GameViewModel : ViewModel() {
    val game = DoomGame()
}
