package day15

import java.util.Comparator.comparing
import kotlin.math.min

data class Point(val x: Int, val y: Int)

val PATH_COMPARATOR: Comparator<Pair<Combat.Cell, Int>> = comparing { path: Pair<Combat.Cell, Int> -> path.second }
    .thenComparing { path: Pair<Combat.Cell, Int> -> path.first.position.y }
    .thenComparing { path: Pair<Combat.Cell, Int> -> path.first.position.x }

val ENEMY_COMPARATOR: Comparator<Combat.Unit> = comparing(Combat.Unit::hp)
    .thenComparing { it -> it.position.y }
    .thenComparing { it -> it.position.x }

class Combat(boardLines: List<String>) {

    private val width = boardLines[0].length
    private val height = boardLines.size

    val board: MutableList<Cell>
    var rounds = 0

    init {
        board = boardLines.mapIndexed { y, line ->
            line.mapIndexed { x, char ->
                when (char) {
                    '#' -> Cell(Point(x, y), char)
                    '.' -> EmptyCell(Point(x, y))
                    'G', 'E' -> Unit(Point(x, y), char)
                    else -> throw IllegalArgumentException("Unknown cell $char at ($x,$y)")
                }
            }
        }.flatten().toMutableList()
    }

    private fun round(): Boolean = board
        .filter { it is Unit }
        .map { it as Unit }
        .asSequence()
        .filter { it.isAlive }
        .fold(true) { result, unit -> result && unit.takeTurn() }

    fun simulate(): Int {

        do {
            val canContinue = round()
            rounds++
            println("Round $rounds completed")
        } while (canContinue)

//        println(this)
        return score(rounds)
    }

    private fun score(rounds: Int): Int {
        val hpTotal = board.filter { it is Unit }.sumBy { (it as Unit).hp }
        val score = hpTotal * (rounds - 1)

        println("${rounds - 1} x $hpTotal = $score")

        return score
    }

    operator fun get(x: Int, y: Int): Cell = board[index(x, y)]
    operator fun get(point: Point): Cell = board[index(point.x, point.y)]

    private fun index(x: Int, y: Int) = y * width + x

    fun set(point: Point, cell: Cell) {
        board[index(point.x, point.y)] = cell
    }

    fun adjacentCells(point: Point): List<Cell> {
        val adjacent = mutableListOf<Cell>()
        if (point.y > 0) {
            adjacent.add(this[point.x, point.y - 1])
        }

        if (point.x > 0) {
            adjacent.add(this[point.x - 1, point.y])
        }

        if (point.x < width - 1) {
            adjacent.add(this[point.x + 1, point.y])
        }

        if (point.y < height - 1) {
            adjacent.add(this[point.x, point.y + 1])
        }

        return adjacent
    }

    override fun toString(): String {
        return "Round $rounds\n" + board.chunked(width).joinToString("\n") { it.joinToString("") } + "\n"
    }

    open class Cell(var position: Point, val type: Char) {
        open fun moveTowards(enemiesOf: Char, currentDepth: Int = 0, bestDepth: Int = Int.MAX_VALUE): Pair<Cell, Int>? {
            return null
        }

        override fun toString(): String = type.toString()
    }

    inner class EmptyCell(position: Point) : Cell(position, '.') {

        var inPath = false

        override fun moveTowards(enemiesOf: Char, currentDepth: Int, bestDepth: Int): Pair<Cell, Int>? {
            if (inPath) {
                return null
            }

            if (currentDepth > bestDepth) {
                return null
            }

            inPath = true

            val adjacentCells = adjacentCells(position)

            val subPaths = mutableListOf<Pair<Cell, Int>>()

            var newBestDepth = bestDepth
            for (adjacentCell in adjacentCells) {
                val subPath = adjacentCell.moveTowards(enemiesOf, currentDepth + 1, newBestDepth)

                if (subPath != null) {
                    val path = Pair(this, subPath.second + 1)
                    newBestDepth = min(bestDepth, path.second)
                    subPaths.add(path)
                }
            }

            inPath = false

            return subPaths.sortedWith(PATH_COMPARATOR).firstOrNull()
        }
    }

    inner class Unit(position: Point, type: Char, var hp: Int = 200, private val attackScore: Int = 3) : Cell(position, type) {
        val isAlive get() = hp > 0

        override fun toString(): String = "$type"

        private fun enemies(): List<Unit> = board.filter(this::isEnemy).map { it as Unit }

        private fun isEnemy(it: Cell) = it is Unit && it.type != type

        fun takeTurn(): Boolean {
            val allEnemies = enemies()

            if (allEnemies.isEmpty()) {
                return false
            }

            val consideredEnemies = allEnemies.filter { adjacentCells(it.position).any { cell -> cell is EmptyCell } }

            if (adjacentEnemies().isEmpty() && consideredEnemies.isNotEmpty()) {
                move()
            }

            if (adjacentEnemies().isNotEmpty()) {
                attack(adjacentEnemies()[0])
            }

            return true
        }

        private fun move() {
            val path = adjacentCells(position)
                .mapNotNull { it.moveTowards(type) }
                .sortedWith(PATH_COMPARATOR)
                .firstOrNull()

            if (path != null) {
                val target = path.first
                set(position, EmptyCell(position))
                position = target.position
                set(position, this)
            }
        }

        override fun moveTowards(enemiesOf: Char, currentDepth: Int, bestDepth: Int): Pair<Cell, Int>? {
            return if (type != enemiesOf) {
                Pair(this, 0)
            } else {
                null
            }
        }

        private fun adjacentEnemies() = adjacentCells(position)
            .filter(this::isEnemy)
            .map { it as Unit }
            .sortedWith(ENEMY_COMPARATOR)

        private fun attack(enemy: Unit) {
            enemy.hp -= attackScore
            if (!enemy.isAlive) {
                set(enemy.position, EmptyCell(enemy.position))
            }
        }
    }

}

//Identify all possible targets (all enemy units)
// -> No enemies = combat ends
//Identify open squares adjacent to each target
// -> if not in range + no open squares = turn ends

//If a target is in range = attack
//Else = move

//To Move:
//Find reachable enemy-adjacent squares
//Order by distance from current location
//Move towards enemy
//(On tie - use reading order)

//To Attack:
//Select adjacent unit with fewest HP
//Deal damage = attack power
//HP <= 0: Die


