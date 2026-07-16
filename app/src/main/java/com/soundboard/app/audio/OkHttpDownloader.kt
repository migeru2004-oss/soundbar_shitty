package com.soundboard.app.audio

import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpDownloader : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ExtractionException::class)
    override fun execute(request: Request): Response {
        val reqBuilder = okhttp3.Request.Builder().url(request.url())
        request.headers().forEach { (key, values) ->
            values.forEach { value ->
                reqBuilder.addHeader(key, value)
            }
        }

        val response = client.newCall(reqBuilder.build()).execute()
        val body = response.body ?: throw IOException("Empty response body")
        val bodyString = body.string()

        val headersMap = mutableMapOf<String, MutableList<String>>()
        response.headers.names().forEach { name ->
            headersMap[name] = response.headers.values(name)
        }

        return Response(
            response.code,
            response.message,
            headersMap,
            bodyString,
            response.request.url.toString()
        )
    }
}
