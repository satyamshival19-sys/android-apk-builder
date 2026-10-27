package com.example.sampleapp

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class LocalServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        var uri = session.uri
        if (uri == "/") uri = "/index.html"

        // HTML फाइल ऐप की इंटरनल स्टोरेज से सर्व करें
        val file = File(context.filesDir, "scripts$uri")

        return if (file.exists()) {
            val inputStream = FileInputStream(file)
            // HTML फाइल को सही माइम टाइप के साथ सर्व करें
            newChunkedResponse(Response.Status.OK, "text/html", inputStream)
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File Not Found")
        }
    }
}
