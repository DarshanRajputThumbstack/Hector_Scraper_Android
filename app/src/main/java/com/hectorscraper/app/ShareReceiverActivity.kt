package com.hectorscraper.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.hectorscraper.app.utils.HectorScraper
import com.hectorscraper.app.utils.LocationCookieInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request

class ShareReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // THIS is where data comes from
        val productUrl = intent.getStringExtra(Intent.EXTRA_TEXT).toString()
        fetchStoreIds(parseInstamartShareText(productUrl))
    }

    private fun fetchStoreIds(url: String) {
        Thread {
            try {
                Log.e("TAG", "fetchStoreIds: lat:${HectorScraper.currentLat}   long:${HectorScraper.currentLng}")
                val client = OkHttpClient.Builder().addInterceptor(LocationCookieInterceptor(HectorScraper.currentLat, HectorScraper.currentLng)).build()

                val request = Request.Builder().url(url).build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("MainActivity", "❌ Request failed: ${response.code}")
                    return@Thread
                }

                val body = response.body?.string() ?: ""
                Log.e("MainActivity", "📩 Response received (${body.length} chars)")

                val ids = StoreIdExtractor.extractStoreIds(body)

                runOnUiThread {
                    Log.e("MainActivity", "🎉 Final Store IDs = $ids")
                    Toast.makeText(this, "Store IDs: $ids", Toast.LENGTH_LONG).show()
                    if (ids.isNotEmpty()) {
                        HectorScraper.storeid = ids[0]
                    }
                    finishAffinity()
                }


            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Failed: ${e.message}")
            }
        }.start()
    }

    fun parseInstamartShareText(text: String): String {

        // 1️⃣ Extract URL
        val urlRegex = Regex("(https?://\\S+)")
        val url = urlRegex.find(text)?.value

        // 2️⃣ Extract title
        // Format: "Check out <TITLE> on Instamart:"
        val titleRegex = Regex("Check out (.*?) on Instamart", RegexOption.IGNORE_CASE)
        titleRegex.find(text)?.groupValues?.get(1)

        if (url != null) {
            HectorScraper.productUrl = url
            return url
        }
        return ""
    }
}