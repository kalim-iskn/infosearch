package task1;

import Crawler
import org.jsoup.nodes.Document
import java.io.File
import java.io.FileOutputStream

fun first(url: String) = run {
    crawle(url)
}

private fun crawle(url: String) {
    val outputDirectory = File("files/task1").apply {
        mkdirs()
        listFiles()?.forEach { it.deleteRecursively() }
    }
    val indexFile = File(outputDirectory, "index.txt").apply { createNewFile() }
    val textFilesDirectory = File(outputDirectory, "files").apply { mkdirs() }
    val predicate = { doc: Document -> doc.body().text().split(' ').size >= 1000 }

    Crawler(100, predicate).apply {
        startCrawl(url)
        getPages().forEachIndexed { index, page ->
            FileOutputStream(indexFile, true).bufferedWriter().use { it.append("$index - ${page.first}\n") }
            File(textFilesDirectory, "$index.html").apply {
                createNewFile()
                val docText = page.second
                writeText(docText)
            }
        }
    }
}
