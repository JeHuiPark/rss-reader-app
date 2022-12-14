package com.example.rssreader

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rssreader.feed.adapter.ArticleAdapter
import com.example.rssreader.feed.search.ResultsCounter
import com.example.rssreader.feed.search.Searcher
import com.example.rssreader.view.hide
import com.example.rssreader.view.show
import kotlinx.coroutines.*

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class SearchActivity : AppCompatActivity() {

    private lateinit var viewManager: LinearLayoutManager
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var articles: RecyclerView
    private val searcher: Searcher = Searcher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        viewManager = LinearLayoutManager(this)
        articleAdapter = ArticleAdapter()
        articles = findViewById<RecyclerView>(R.id.articles).apply {
            layoutManager = viewManager
            adapter = articleAdapter
        }

        GlobalScope.launch {
            updateCounter()
        }

        findViewById<Button>(R.id.searchButton).setOnClickListener {
            articleAdapter.clear()
            GlobalScope.launch {
                ResultsCounter.reset()
                showLoading()
                hideUpdateCounter()
                search()
                showUpdateCounter()
                hideLoading()
            }
        }
    }

    private fun search() {
        val query = findViewById<EditText>(R.id.searchText).text.toString()
        val channel = searcher.search(query)
        while(!channel.isClosedForReceive) {
            val article = channel.tryReceive().getOrNull() ?: continue

            GlobalScope.launch(Dispatchers.Main) {
                articleAdapter.add(article)
            }
        }
    }

    private fun showLoading() = findViewById<ProgressBar>(R.id.progressBar).show()

    private fun hideLoading() = findViewById<ProgressBar>(R.id.progressBar).hide()

    @SuppressLint("SetTextI18n")
    private suspend fun updateCounter() {
        val notifications = ResultsCounter.notificationReceiver
        val results = findViewById<TextView>(R.id.results)

        while(!notifications.isClosedForReceive) {
            val newAmount = notifications.receive()
            withContext(Dispatchers.Main) {
                results.text = "Results: $newAmount"
            }
        }
    }

    private fun showUpdateCounter() = findViewById<TextView>(R.id.results).show()
    private fun hideUpdateCounter() = findViewById<TextView>(R.id.results).hide()
}