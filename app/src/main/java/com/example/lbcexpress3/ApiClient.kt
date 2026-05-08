package com.example.lbcexpress3

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ApiClient {

    // Emulator  → 10.0.2.2  (maps to your PC's localhost)
    // Real device → change to your PC's LAN IP e.g. 192.168.1.x
    private const val BASE_URL = "http://10.0.2.2/LBCExpress/api"

    // Set by SessionManager after login — attached to every request automatically
    var authId: Int = -1
    var authType: String = ""   // "customer" or "employee"

    /** Build the auth params that every request must carry */
    private fun authParams(): Map<String, String> {
        if (authId <= 0 || authType.isEmpty()) return emptyMap()
        return mapOf("_auth_id" to authId.toString(), "_auth_type" to authType)
    }

    suspend fun get(endpoint: String, params: Map<String, String> = emptyMap()): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                val allParams = authParams() + params
                val query = allParams.entries.joinToString("&") {
                    "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
                }
                val urlStr = if (query.isEmpty()) "$BASE_URL/$endpoint"
                             else "$BASE_URL/$endpoint?$query"
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout    = 10_000
                }
                val response = conn.inputStream.bufferedReader().readText().trim()
                conn.disconnect()
                JSONObject(response)
            } catch (e: Exception) {
                Log.e("ApiClient", "GET $endpoint error: ${e.message}")
                JSONObject().put("ok", false).put("error", e.message ?: "Network error")
            }
        }
    }

    suspend fun post(endpoint: String, params: Map<String, String>): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                val allParams = authParams() + params
                val body = allParams.entries.joinToString("&") {
                    "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
                }
                val conn = (URL("$BASE_URL/$endpoint").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput      = true
                    connectTimeout = 10_000
                    readTimeout    = 10_000
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(body) }

                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = BufferedReader(InputStreamReader(stream)).readText().trim()
                conn.disconnect()
                JSONObject(response)
            } catch (e: Exception) {
                Log.e("ApiClient", "POST $endpoint error: ${e.message}")
                JSONObject().put("ok", false).put("error", e.message ?: "Network error")
            }
        }
    }
}
