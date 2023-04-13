package task4

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import task3.InvertedIndex
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.log2

data class ObjectWeight(
    val idfArray: DoubleArray,
    val tfIdfMatrix: Array<DoubleArray>
)

var invertedIndexList: List<InvertedIndex> = listOf()

fun fourth(): Pair<ObjectWeight, ObjectWeight> {
    val gson = Gson()
    val itemType = object : TypeToken<List<InvertedIndex>>() {}.type
    val invertedIndexFile =
        File(File("files/task3"), "inverted.txt")
    invertedIndexList = gson.fromJson(invertedIndexFile.readText(), itemType)

    val terms = run {
        File("files/task2/tokens")
            .listFiles()
            .map { it.name to it.readLines() }
    }
    val termsWeight = calcWeight(terms, "tokens")

    val lemmas = run {
        File("files/task2/lemmas")
            .listFiles()
            .map { it.name to it.readLines() }
    }

    val formattedLemmas = lemmas.map { lemma ->
        Pair(lemma.first, lemma.second.map { line ->  line.split(':')[0] })
    }
    val lemmasWeight = calcWeight(formattedLemmas, "lemmas")

    return Pair(termsWeight, lemmasWeight)
}

fun calcWeight(objects: List<Pair<String, List<String>>>, outputDir: String): ObjectWeight {
    val documentsCount = objects.size

    val tfMatrix = Array(invertedIndexList.size) { DoubleArray(documentsCount) }
    val idfArray = DoubleArray(invertedIndexList.size)
    val tfIdfMatrix = Array(invertedIndexList.size) { DoubleArray(documentsCount) }

    invertedIndexList.forEachIndexed { objectIndex, invertedIndex ->
        idfArray[objectIndex] = getIdf(invertedIndex, documentsCount)
        objects.forEachIndexed { docIndex, it ->
            tfMatrix[objectIndex][docIndex] = getTf(invertedIndex, it.first, it.second)
            tfIdfMatrix[objectIndex][docIndex] =
                tfMatrix[objectIndex][docIndex] * idfArray[objectIndex]
        }
    }
    saveResultInFiles(tfMatrix, idfArray, tfIdfMatrix, objects, outputDir)

    return ObjectWeight(idfArray, tfIdfMatrix)
}

fun getTf(invertedIndex: InvertedIndex, docName: String, doc: List<String>): Double {
    val wordCountInDoc = invertedIndex.locations
        .filter { it.fileName == docName }.size.toDouble()
    return wordCountInDoc / doc.size
}

fun getIdf(invertedIndex: InvertedIndex, documentsCount: Int): Double {
    val docsWithWordCount = invertedIndex.locations.distinctBy { it.fileName }.size
    return log2(documentsCount.toDouble() / docsWithWordCount)
}

fun saveResultInFiles(
    tfMatrix: Array<DoubleArray>,
    idfArray: DoubleArray,
    tfIdfMatrix: Array<DoubleArray>,
    documents: List<Pair<String, List<String>>>,
    outputDir: String
) {
    val outputDirectory = File("files/task4/${outputDir}").apply {
        mkdirs()
        listFiles()?.forEach { it.deleteRecursively() }
    }
    val df = DecimalFormat("#.#####")
    df.roundingMode = RoundingMode.CEILING
    val tfFile = File(outputDirectory, "tfTable.txt")
    val idfFile = File(outputDirectory, "idfTable.txt")
    val tfIdfFile = File(outputDirectory, "tfIdfTable.txt")
    val maxCount = invertedIndexList.maxOf { it.term.length } + 1
    val documentRow = StringBuilder("".padEnd(maxCount))
    documents.forEach {
        documentRow.append(it.first.padEnd(8))
    }
    documentRow.appendLine()
    val tfTable = StringBuilder(documentRow)
    val idfTable = StringBuilder()
    val tfIdfTable = StringBuilder(documentRow)

    tfMatrix.forEachIndexed { index, doubles ->
        val rowBuilder = StringBuilder(invertedIndexList[index].term.padEnd(maxCount))
        doubles.forEach {
            rowBuilder.append(df.format(it).padEnd(8))
        }
        tfTable.appendLine(rowBuilder.toString())
    }

    idfArray.forEachIndexed { index, d ->
        idfTable.append(invertedIndexList[index].term.padEnd(maxCount))
            .append(df.format(d).padEnd(8))
            .appendLine()
    }

    tfIdfMatrix.forEachIndexed { index, doubles ->
        val rowBuilder = StringBuilder(invertedIndexList[index].term.padEnd(maxCount))
        doubles.forEach {
            rowBuilder.append(df.format(it).padEnd(8))
        }
        tfIdfTable.appendLine(rowBuilder.toString())
    }
    tfFile.writeText(tfTable.toString())
    idfFile.writeText(idfTable.toString())
    tfIdfFile.writeText(tfIdfTable.toString())
}
