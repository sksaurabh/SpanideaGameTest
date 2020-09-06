package com.saurabh.spanideagametest.activities


import com.saurabh.spanideagametest.utils.GameEngine
import com.saurabh.spanideagametest.utils.GameSpanIdea
import java.util.*


class GameCalculationDigit(delegated: GameSpanIdea) : GameEngine(delegate = delegated) {

    lateinit var startedPlaying : Date
        private set
    lateinit var startLastGame : Date
        private set

    init {
        this.startedPlaying = Date()
        this.startLastGame = this.startedPlaying
    }

    override fun newGame(newHighScore: Int) {
        super.newGame(newHighScore)
        this.startLastGame = Date()
    }

    fun getTotalTimePlaying() = (Date().time - startedPlaying.time)
    fun getTimePlayingCurrent() = (Date().time - startLastGame.time)
}