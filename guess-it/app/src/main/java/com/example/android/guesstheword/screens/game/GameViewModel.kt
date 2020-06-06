package com.example.android.guesstheword.screens.game

import android.os.CountDownTimer
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

class GameViewModel: ViewModel() {

    enum class BuzzType(val pattern: LongArray) {
        CORRECT(longArrayOf(100, 100, 100, 100, 100, 100)),
        GAME_OVER(longArrayOf(0, 2000)),
        COUNTDOWN_PANIC(longArrayOf(0, 200)),
        NO_BUZZ(longArrayOf(0))
    }

    companion object {
        // These represent different important times
        // This is the number of milliseconds in a second
        const val ONE_SECOND = 1000L

        const val TWO_SECOND = 2000L
        // This is the total time of the game
        const val COUNTDOWN_TIME = 10000L
    }

    // The current word
    private val _word = MutableLiveData<String>()
    val word: LiveData<String>
        get() = _word

    // The current score
    private val _score = MutableLiveData<Int>()
    val score: LiveData<Int>
        get() = _score

    // The list of words - the front of the list is the next word to guess
    private lateinit var wordList: MutableList<String>

    private val _gameFinished = MutableLiveData<Boolean>()
    val gameFinished : LiveData<Boolean>
        get() = _gameFinished

    private val timer: CountDownTimer
    private val _secondsToFinish = MutableLiveData<Long>()

    private val _buzzPattern = MutableLiveData<LongArray>()
    val buzzPattern: LiveData<LongArray>
        get() = _buzzPattern

    val secondsToFinishAsString = Transformations.map(_secondsToFinish) { time->
        DateUtils.formatElapsedTime(time)
    }

    init {
        _word.value = ""
        _score.value = 0
        _buzzPattern.value = BuzzType.NO_BUZZ.pattern
        resetList()
        nextWord()
        _gameFinished.value = false
        timer = object: CountDownTimer(COUNTDOWN_TIME, ONE_SECOND){
            override fun onFinish() {
                _gameFinished.value =true
                _buzzPattern.value = BuzzType.GAME_OVER.pattern
            }

            override fun onTick(millisecondToFinish: Long) {
                _secondsToFinish.value = (millisecondToFinish / ONE_SECOND)
                if(millisecondToFinish == TWO_SECOND ){
                    _buzzPattern.value = BuzzType.COUNTDOWN_PANIC.pattern
                }
            }
        }.start()
    }

    /**
     * Resets the list of words and randomizes the order
     */
    private fun resetList() {
        wordList = mutableListOf(
                "queen",
                "hospital",
                "basketball",
                "cat",
                "change",
                "snail",
                "soup",
                "calendar",
                "sad",
                "desk",
                "guitar",
                "home",
                "railway",
                "zebra",
                "jelly",
                "car",
                "crow",
                "trade",
                "bag",
                "roll",
                "bubble"
        )
        wordList.shuffle()
    }

    /**
     * Moves to the next word in the list
     */
    private fun nextWord() {
        //Select and remove a word from the list
        if (wordList.isEmpty()) {
            resetList()
        }
        _word.value = wordList.removeAt(0)

    }

    /** Methods for buttons presses **/

    fun onSkip() {
        _score.value = (_score.value)?.minus(1)
        nextWord()
    }

    fun onCorrect() {
        _score.value = (_score.value)?.plus(1)
        _buzzPattern.value = BuzzType.CORRECT.pattern
        nextWord()
    }

    fun onGameFinishComplete(){
        _gameFinished.value = false
    }

    override fun onCleared() {
        super.onCleared()
        timer.cancel()
    }

    fun onBuzActivated(){
        _buzzPattern.value = BuzzType.NO_BUZZ.pattern
    }

}