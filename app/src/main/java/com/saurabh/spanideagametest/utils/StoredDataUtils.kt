package com.saurabh.spanideagametest.utils

import android.content.Context
import com.saurabh.spanideagametest.R

open class StoredDataUtils(context: Context) {

    private val preferenceKey = context.getString(R.string.game_data_preference_key)
    private val highscoreKey = context.getString(R.string.game_data_highest_score_int_key)
    private val sPref = context.getSharedPreferences(this.preferenceKey, Context.MODE_PRIVATE)


    fun putHighscore(newHS : Int) {
        val editor = sPref.edit()
        editor.putInt(this.highscoreKey, newHS)
        editor.apply()
    }


    fun getHighscore() : Int {
        return this.sPref.getInt(this.highscoreKey, -1)
    }
}