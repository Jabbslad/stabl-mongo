package jabbslad

import com.google.gson.Gson
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.robinkanters.podkast.PodcastLoader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import java.net.URL

/**
 * Created by jabbslad on 06/06/2017.
 */

val client = OkHttpClient()
val gson = Gson()

data class Episode(val feedUrl: String)
data class Result(val resultCount: Int, val results: Array<Episode>)

fun fetch(id: String) : String {
    val request = Request.Builder()
            .url("https://itunes.apple.com/lookup?id=$id")
            .build()

    val response = client.newCall(request).execute()
    val result = gson.fromJson(response.body()!!.charStream(), Result::class.java)
    return result.results[0].feedUrl
}

fun run(id: String) : Episode {
    val podcast = PodcastLoader.load(URL(if (id.startsWith("http://")) id else fetch(id)))
    return Episode(podcast.link?.toString() ?: id)
}

fun mongify(stabl: MongoDatabase, episode: Episode) =
        stabl.getCollection<Episode>()
                .replaceOne(Filters.eq("feedUrl", episode.feedUrl), episode, UpdateOptions().upsert(true))!!

fun main(args: Array<String>) {
    KMongo.createClient(args[0], args[1].toInt()).use {
        val stabl = it.getDatabase("stabl")
        args.slice(2 until args.size).map { run(it) }.forEach { mongify(stabl, it) }
    }
}
