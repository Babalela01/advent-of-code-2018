package day15

abstract class Cell(var position: Point, val type: Char) {
    override fun toString(): String = type.toString()
}

class WallCell(point: Point) : Cell(point, '#')
class EmptyCell(position: Point) : Cell(position, '.')

class UnitCell(position: Point, type: Char, private val attackScore: Int = 3, private val combat: Combat) :
    Cell(position, type) {
    var hp =  200
    val isAlive get() = hp > 0

    override fun toString(): String = "$type"

    private fun enemies(): List<UnitCell> = combat.board.filter(this::isEnemy).map { it as UnitCell }

    fun isEnemy(it: Cell) = it is UnitCell && it.type != type

    fun takeTurn(): Boolean {
        val allEnemies = enemies()

        if (allEnemies.isEmpty()) {
            return false
        }

        val consideredEnemies = allEnemies.filter { combat.adjacentCells(it.position)
            .any { cell -> cell is EmptyCell } }

        if (adjacentEnemies().isEmpty() && consideredEnemies.isNotEmpty()) {
            move()
        }

        if (adjacentEnemies().isNotEmpty()) {
            attack(adjacentEnemies()[0])
        }

        return true
    }

    private fun move() {
        val path = combat.bfs(this)

        if (path != null) {
            moveTo(path.first)
        }
    }

    private fun moveTo(target: Cell) {
        combat.set(position, EmptyCell(position))
        position = target.position
        combat.set(position, this)
    }

    private fun adjacentEnemies() = combat.adjacentCells(position)
        .filter(this::isEnemy)
        .map { it as UnitCell }
        .sortedWith(ENEMY_COMPARATOR)

    private fun attack(enemy: UnitCell) {
        enemy.hp -= attackScore
        if (!enemy.isAlive) {
            combat.set(enemy.position, EmptyCell(enemy.position))
        }
    }
}