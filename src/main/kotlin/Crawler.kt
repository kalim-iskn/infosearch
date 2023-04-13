import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.*

class Crawler(
    private val pagesCount: Int, private val pagePredicate: (Document) -> Boolean
) {
    private val pagesVisited = HashSet<String>()
    private val pagesToVisit = LinkedList<String>()
    private val linksAndPageTexts: MutableList<Pair<String, String>> = mutableListOf()

    fun startCrawl(url: String) {

        while (linksAndPageTexts.size < pagesCount) {
            val currentUrl: String

            if (pagesToVisit.isEmpty()) {
                currentUrl = url
                pagesVisited.add(url)
            } else {
                currentUrl = next()
            }

            crawl(currentUrl)
        }
    }

    private fun crawl(url: String): Boolean {
        try {
            val connection = Jsoup.connect(url).userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36")
            val htmlDocument = connection.get()

            val response = connection.response()

            if (!response.contentType().contains("text/html")) {
                return false
            }

            val linksOnPage = htmlDocument.select("a[href]")
            linksOnPage.forEach {
                val link = it.absUrl("href").split('#')[0]
                pagesToVisit.add(link)
            }

            if (pagePredicate(htmlDocument)) {
                linksAndPageTexts.add(url to htmlDocument.html())
            }
            return true
        } catch (ioe: IOException) {
            return false
        }
    }

    private fun next(): String {
        var nextUrl: String
        do {
            nextUrl = pagesToVisit.removeAt(0)
        } while (pagesVisited.contains(nextUrl))
        pagesVisited.add(nextUrl)
        return nextUrl
    }

    fun getPages(): List<Pair<String, String>> = linksAndPageTexts
}