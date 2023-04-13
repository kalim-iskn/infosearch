package task3

import com.bpodgursky.jbool_expressions.*
import com.bpodgursky.jbool_expressions.parsers.ExprParser
import com.bpodgursky.jbool_expressions.rules.RuleSet
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

lateinit var expressionVariables: Map<String, String>
lateinit var documentLocations: Set<String>
val invertedIndex = mutableMapOf<String, MutableList<Location>>()

fun third() {
    generate()
}

fun indexFile(file: File) {
    val fileName = file.name
    file.readLines().forEachIndexed { index, word ->
        var locations = invertedIndex[word]
        if (locations == null) {
            locations = mutableListOf()
            invertedIndex[word] = locations
        }
        locations.add(Location(fileName, index + 1))
    }
}

fun generate() {
    val files = run {
        File("files/task2/tokens").listFiles()
    }
    files?.forEach { indexFile(it) }
    val gson = Gson()
    val outputDirectory = File("files/task3").apply {
        mkdirs()
        listFiles()?.forEach { it.deleteRecursively() }
    }
    val file = File(outputDirectory, "inverted.txt").apply { createNewFile() }
    FileOutputStream(file, true).bufferedWriter().use { output ->
        val invertedIndexList = invertedIndex.map { InvertedIndex(it.key, it.value) }
        output.appendLine(gson.toJson(invertedIndexList))
    }
}

fun booleanSearch(expression: String, variablesMap: Map<String, String>) {
    documentLocations = invertedIndex.values.flatten().map { it.fileName }.toSet()
    expressionVariables = variablesMap
    val expr = ExprParser.parse(expression)
    val simplifiedExpr = RuleSet.simplify(expr)
    val set = iterateExpression(simplifiedExpr)
    set.forEach { println(it) }
    println("Количество найденных документов: ${set.size} шт.")
}

fun <T> Iterable<Iterable<T>>.unionFlatten(): Set<T> {
    var result = setOf<T>()
    for (element in this) {
        result = result.union(element)
    }
    return result
}


fun iterateExpression(expression: Expression<String>): Set<String> {
    return when (expression) {
        is Not -> documentLocations
            .minus(findWordInDocs((expression.e as Variable).value))
            .toSet()
        is Or -> expression.children.map { iterateExpression(it) }.unionFlatten()
        is And -> expression.children.map { iterateExpression(it) }.intersectFlatten()
        is Variable -> findWordInDocs(expression.value)
        else -> setOf()
    }
}

fun findWordInDocs(wordToken: String): Set<String> {
    return findWordLocations(wordToken).map { it.fileName }.toSet()
}

fun findWordLocations(wordToken: String): List<Location> {
    val w = expressionVariables[wordToken]?.toLowerCase()
    return invertedIndex[w]?.toList() ?: listOf()
}

data class InvertedIndex(
    val term: String,
    val locations: MutableList<Location>
)

data class Location(
    val fileName: String,
    val lineIndex: Int
) {
    override fun toString() = "{$fileName, line index $lineIndex}"
}

fun <T> Iterable<Iterable<T>>.intersectFlatten(): Set<T> {
    var result = first().toSet()
    for (element in drop(1)) {
        result = result.intersect(element)
    }
    return result
}
