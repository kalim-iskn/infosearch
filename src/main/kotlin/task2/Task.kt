package task2

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.PrintWriter

fun second() {
    execute()
}

private fun tokenizeText(text: String): List<String> {
    return text.toLowerCase()
        .replace(Regex("[^a-zA-Zа-яА-Я ]"), " ")
        .split(" ")
        .filter { it != " " && it != "" }
        .distinct()
}

private fun writeTokens(tokens: List<String>, outputFile: File) {
    FileOutputStream(outputFile, true).bufferedWriter().use { output ->
        tokens.forEach { token ->
            output.appendLine(token)
        }
    }
}

private fun execute() {
    val textFilesDirectory = File("files/task1/files")
    val tokenizedDocs = textFilesDirectory.listFiles()?.toList()?.map { file ->
        val htmlDoc = Jsoup.parse(file.bufferedReader().use { it.readText() })
        val text = htmlDoc.body().text()

        val tokens = tokenizeText(text)
        DocumentTokens(
            englishWordTokensList = tokens.filter { it.matches(Regex("[a-zA-Z]+")) },
            russianWordTokensList = tokens.filter { it.matches(Regex("[а-яА-Я]+")) },
            documentName = file.nameWithoutExtension
        )
    }
    val outputTokensDirectory = File("files/task2/tokens/").apply {
        mkdirs()
        listFiles()?.forEach { it.deleteRecursively() }
    }
    val outputLemmasDirectory = File("files/task2/lemmas/").apply {
        mkdirs()
        listFiles()?.forEach { it.deleteRecursively() }
    }

    tokenizedDocs?.forEach {
        // Запысаываем токены
        val resultTokenFile =
            File(outputTokensDirectory, "tokens_" + it.documentName + ".txt").apply { createNewFile() }
        writeTokens(
            it.getFilteredWithStopWordsRussianTokens() + it.getFilteredWithStopWordsEnglishTokens(),
            resultTokenFile
        )

        //Лемматизация
        val resultLemmasFile =
            File(outputLemmasDirectory, "lemmas_" + it.documentName + ".txt").apply { createNewFile() }
        resultLemmasFile.printWriter().use { pw ->
            RussianAnalyzer().lemmatizeTokens(it.getFilteredWithStopWordsRussianTokens(), resultLemmasFile, pw)
            EnglishAnalyzer().lemmatizeTokens(it.getFilteredWithStopWordsEnglishTokens(), resultLemmasFile, pw)
        }
    }

}

private fun Analyzer.lemmatizeTokens(
    tokens: List<String>,
    outputFile: File,
    printWriter: PrintWriter
) {
    val map = HashMap<String, HashSet<String>>()
    val stream = tokenStream(outputFile.name, tokens.joinToString(" "))
    stream.reset()
    stream.use {
        var i = 0
        while (stream.incrementToken()) {
            val token = tokens[i]
            val lemma = stream.getAttribute(CharTermAttribute::class.java).toString()

            when {
                map[lemma]?.isNotEmpty() == true -> map[lemma]?.add(token)
                else -> map[lemma] = hashSetOf(token)
            }
            i++
        }
    }
    map.forEach { (lemma, lemmaTokens) ->
        printWriter.appendLine("$lemma: ${lemmaTokens.joinToString(" ")}")
    }
}

data class DocumentTokens(
    private val englishWordTokensList: List<String>,
    private val russianWordTokensList: List<String>,
    val documentName: String
) {
    private val englishStopWords: List<String>
    private val russianStopWords: List<String>

    init {
        val gson = Gson()
        val itemType = object : TypeToken<List<String>>() {}.type
        englishStopWords = gson.fromJson(FileReader(File("stops/en.json")), itemType)
        russianStopWords = gson.fromJson(FileReader(File("stops/ru.json")), itemType)
    }

    //Фильтрация стоп-слов
    fun getFilteredWithStopWordsEnglishTokens(): List<String> =
        englishWordTokensList.filter { !englishStopWords.contains(it) }

    fun getFilteredWithStopWordsRussianTokens(): List<String> =
        russianWordTokensList.filter { !russianStopWords.contains(it) }

}
