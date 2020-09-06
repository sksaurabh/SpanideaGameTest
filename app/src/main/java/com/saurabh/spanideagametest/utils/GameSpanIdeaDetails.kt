package com.saurabh.spanideagametest.utils

import java.io.Serializable
import java.util.Random
import kotlin.collections.ArrayList


interface GameSpanIdea {
    fun updateTileValue(move: GameEngine.Transition)
    fun userPB(score: Int)
    fun userWin()
    fun userFail()
    fun userScoreChanged(score: Int)
}


enum class GameMoves { Up, Down, Left, Right }


enum class TileMoveType { Add, Slide, Merge, Clear, Reset }



open class GameEngine : Serializable {

    private var delegate: GameSpanIdea

    private val gridCount = GameConstants.TILE_CNT
    private val rowCnt = GameConstants.DIMENSION
    private val colCnt = GameConstants.DIMENSION
    private val blankTile = GameConstants.EMPTY_TILE_VAL

    var score = 0
        private set
    var previousHighScore = 0
        private set
    var maxTile = GameConstants.EMPTY_TILE_VAL
        private set

    private var gameOver = false
    private var numEmpty = GameConstants.TILE_CNT

    private var previousMoves : ArrayList<GameBoardRecord> = ArrayList()
    private var transitions : ArrayList<Transition> = ArrayList()
    private var tiles : ArrayList<Int> = ArrayList()

    private val rand = Random()

    constructor(delegate: GameSpanIdea) {
        this.delegate = delegate
    }


    open fun newGame(newHighScore: Int) {
        this.previousHighScore = newHighScore
        this.numEmpty = GameConstants.TILE_CNT
        this.maxTile = GameConstants.EMPTY_TILE_VAL
        this.gameOver = false
        this.score = 0

        this.previousMoves = ArrayList()
        this.transitions = ArrayList()
        this.tiles = ArrayList()


        for (i in 0 until gridCount) {
            tiles.add(GameConstants.EMPTY_TILE_VAL)
            this.transitions.add(Transition(TileMoveType.Clear, this.blankTile, i))
        }

        this.addNewTile(2)
        this.addNewTile(2)
        this.previousMoves.add(0, getGameBoardRecord())
        this.applyGameMoves()
    }


    fun replotBoard() {
        transitions = ArrayList()
        for (i in 0 until gridCount) {
            this.transitions.add(Transition(TileMoveType.Reset, tiles[i], i))
        }
        this.applyGameMoves()
    }


    private fun getGameBoardRecord() : GameBoardRecord {
        return GameBoardRecord(tiles, score, numEmpty, gameOver, maxTile)
    }

    fun acheivedTarget() : Boolean {
        return (maxTile >= GameConstants.WIN_TARGET)
    }

    fun getTileValue(at: Int) : Int {
        if (at >= 0 && at < gridCount) {
            return tiles[at]
        }
        return 0
    }

    private fun addNewTile(seedValue: Int = -1) : Boolean {
        if (numEmpty == 0) { return false }

        var value = if (seedValue < 2 || seedValue > 4) {
            // Randomly select 2 or 4 at a ratio 3.5:1 in favour ot 2.
            val sample = rand.nextInt(100)
            if (sample >= GameConstants.RANDOM_RATIO) {
                4
            } else {
                2
            }
        } else {
            seedValue
        }

        val pos = rand.nextInt(numEmpty)
        var blanksFound = 0

        for (i in 0 until gridCount) {
            if (tiles[i] == blankTile) {
                if (blanksFound == pos) {
                    tiles[i] = value
                    if (value > maxTile) { maxTile = value }
                    numEmpty -= 1
                    transitions.add(Transition(TileMoveType.Add, value, i))
                    return true
                }
                blanksFound += 1
            }
        }
        return false
    }

    private fun hasMovesRemaining() : Boolean {

        if (numEmpty > 0) { return true }

        // check left-right for compact moves remaining.
        val arrLimitX = gridCount-colCnt
        for (i in 0 until arrLimitX) {
            if (tiles[i] == tiles[i + colCnt]) { return true }
        }

        // check up-down for compact moves remaining.
        val arrLimitY = gridCount - 1
        for (i in 0 until arrLimitY) {
            if ((i + 1) % rowCnt > 0) {
                if (tiles[i] == tiles[i + 1]) { return true }
            }
        }
        return false
    }

    private fun slideTileRowOrColumn(vararg indexes: Int) : Boolean {

        var moved = false
        val tmpArr = intArrayOf(*indexes)

        // Do we have some sliding to do, or not?
        var es = 0  // empty spot index
        for (j in tmpArr.indices) {
            if (tiles[tmpArr[es]] != blankTile) {
                es += 1
                continue
            } else if (tiles[tmpArr[j]] == blankTile) {
                continue
            } else {
                // Otherwise we have a slide condition
                tiles[tmpArr[es]] = tiles[tmpArr[j]]
                tiles[tmpArr[j]] = blankTile
                transitions.add(Transition(TileMoveType.Slide, tiles[tmpArr[es]], tmpArr[es], tmpArr[j]))
                moved = true
                es += 1
            }
        }
        return moved
    }

    private fun compactTileRowOrColumn(vararg indexes: Int) : Boolean {

        var compacted = false
        val tmpArr = intArrayOf(*indexes)

        for (j in 0 until (tmpArr.size-1)) {
            if (tiles[tmpArr[j]] != blankTile && tiles[tmpArr[j]] == tiles[tmpArr[j+1]]) { // we found a matching pair
                val ctv = tiles[tmpArr[j]] * 2   // = compacted tile value
                tiles[tmpArr[j]] = ctv
                tiles[tmpArr[j+1]] = blankTile
                score += ctv
                if (ctv > maxTile) {
                    maxTile = ctv
                }  // is this the biggest tile # so far
                transitions.add(Transition(TileMoveType.Merge, ctv, tmpArr[j], tmpArr[j+1]))
                compacted = true
                numEmpty += 1
            }
        }
        return compacted
    }

    private fun slideLeft() : Boolean {
        val a = slideTileRowOrColumn(0, 4, 8, 12)
        val b = slideTileRowOrColumn(1, 5, 9, 13)
        val c = slideTileRowOrColumn(2, 6, 10, 14)
        val d = slideTileRowOrColumn(3, 7, 11, 15)
        return (a || b || c || d)
    }

    private fun slideRight() : Boolean {
        val a = slideTileRowOrColumn(12, 8, 4, 0)
        val b = slideTileRowOrColumn(13, 9, 5, 1)
        val c = slideTileRowOrColumn(14, 10, 6, 2)
        val d = slideTileRowOrColumn(15, 11, 7, 3)
        return (a || b || c || d)
    }

    private fun slideUp() : Boolean {
        val a = slideTileRowOrColumn(0, 1, 2, 3)
        val b = slideTileRowOrColumn(4, 5, 6, 7)
        val c = slideTileRowOrColumn(8, 9, 10, 11)
        val d = slideTileRowOrColumn(12, 13, 14, 15)
        return (a || b || c || d)
    }

    private fun slideDown() : Boolean {
        val a = slideTileRowOrColumn(3, 2, 1, 0)
        val b = slideTileRowOrColumn(7, 6, 5, 4)
        val c = slideTileRowOrColumn(11, 10, 9, 8)
        val d = slideTileRowOrColumn(15, 14, 13, 12)
        return (a || b || c || d)
    }

    private fun compactLeft() : Boolean {
        val a = compactTileRowOrColumn(0, 4, 8, 12)
        val b = compactTileRowOrColumn(1, 5, 9, 13)
        val c = compactTileRowOrColumn(2, 6, 10, 14)
        val d = compactTileRowOrColumn(3, 7, 11, 15)
        return (a || b || c || d)
    }

    private fun compactRight() : Boolean {
        val a = compactTileRowOrColumn(12, 8, 4, 0)
        val b = compactTileRowOrColumn(13, 9, 5, 1)
        val c = compactTileRowOrColumn(14, 10, 6, 2)
        val d = compactTileRowOrColumn(15, 11, 7, 3)
        return (a || b || c || d)
    }

    private fun compactUp() : Boolean {
        val a = compactTileRowOrColumn(0, 1, 2, 3)
        val b = compactTileRowOrColumn(4, 5, 6, 7)
        val c = compactTileRowOrColumn(8, 9, 10, 11)
        val d = compactTileRowOrColumn(12, 13, 14, 15)
        return (a || b || c || d)
    }

    private fun compactDown() : Boolean {
        val a = compactTileRowOrColumn(3, 2, 1, 0)
        val b = compactTileRowOrColumn(7, 6, 5, 4)
        val c = compactTileRowOrColumn(11, 10, 9, 8)
        val d = compactTileRowOrColumn(15, 14, 13, 12)
        return (a || b || c || d)
    }

    private fun actionMoveLeft() : Boolean {
        val a = slideLeft()
        val b = compactLeft()
        val c = slideLeft()
        return (a || b || c)
    }

    private fun actionMoveRight() : Boolean {
        val a = slideRight()
        val b = compactRight()
        val c = slideRight()
        return (a || b || c)
    }

    private fun actionMoveUp() : Boolean {
        val a = slideUp()
        val b = compactUp()
        val c = slideUp()
        return (a || b || c)
    }

    private fun actionMoveDown() : Boolean {
        val a = slideDown()
        val b = compactDown()
        val c = slideDown()
        return (a || b || c)
    }

    // TODO THIS FUNCTION IS THE MAIN CONTROLLER FOR GAME MOVES

    fun actionMove(move : GameMoves) : Boolean {

        var tempScore = this.score

        // Clean the transition record as we are doing a new move action.
        this.transitions = ArrayList()

        val changed = when (move) {
            GameMoves.Up -> actionMoveUp()
            GameMoves.Down -> actionMoveDown()
            GameMoves.Left -> actionMoveLeft()
            GameMoves.Right -> actionMoveRight()
        }


        if (changed) {
            addNewTile()
            this.previousMoves.add(0, getGameBoardRecord())  // index zero is current/last board result
            if (previousMoves.size > GameConstants.MAX_PREVIOUS_MOVES+1) {
                this.previousMoves.removeAt(previousMoves.size-1)
            }
            this.applyGameMoves()
        }

        if (this.score != tempScore) {
            delegate.userScoreChanged(score = this.score)
        }

        if (!hasMovesRemaining()) {
            this.gameOver = true
            if (this.maxTile >= GameConstants.WIN_TARGET) {
                delegate.userWin()
            } else {
                delegate.userFail()
            }
            if (this.score != tempScore && this.score > this.previousHighScore) {
                delegate.userPB(this.score)
            }
            return false
        }

        return changed
    }


    private fun applyGameMoves() {
        for (trans in this.transitions) {
            delegate.updateTileValue(trans)
        }
    }


    fun goBackOneMove() : Boolean {
        if (this.previousMoves.size > 1) {
            val pm = this.previousMoves[1]
            this.tiles = pm.tiles
            this.score = pm.score
            this.numEmpty = pm.numEmpty
            this.gameOver = pm.gameOver
            this.maxTile = pm.maxTile
            this.previousMoves.removeAt(0)
            this.replotBoard()
            return true
        }
        return false
    }

    fun asString() : String {
        var str = "---------\n"
        str += "|$tiles[0]|$tiles[4]|$tiles[8]|$tiles[12]|\n"
        str += "|$tiles[1]|$tiles[5]|$tiles[9]|$tiles[13]|\n"
        str += "|$tiles[2]|$tiles[6]|$tiles[10]|$tiles[14]|\n"
        str += "|$tiles[3]|$tiles[7]|$tiles[11]|$tiles[15]|\n"
        str += "---------"
        return (str)
    }

    // Tile movement instructions record for the game board renderer
    class Transition : Serializable {
        var action: TileMoveType
        var value: Int
        var location: Int
        var oldLocation: Int

        constructor(action: TileMoveType, value: Int, location: Int, oldLocation: Int = -1) {
            this.action = action
            this.value = value
            this.location = location
            this.oldLocation = oldLocation
        }
    }


    class GameBoardRecord : Serializable {
        var tiles: ArrayList<Int>
        var score: Int
        var numEmpty: Int
        var gameOver: Boolean
        var maxTile: Int

        constructor(tiles: ArrayList<Int>, score: Int, numEmpty: Int, gameOver: Boolean, maxTile: Int) {
            this.tiles = tiles
            this.score = score
            this.numEmpty = numEmpty
            this.gameOver = gameOver
            this.maxTile = maxTile
        }
    }
}