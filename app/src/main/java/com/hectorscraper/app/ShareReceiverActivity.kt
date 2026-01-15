package com.hectorscraper.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.hectorscraper.app.utils.HectorScraper
import com.hectorscraper.app.utils.LocationCookieInterceptor
import com.hectorscraper.app.utils.PreferenceManager
import okhttp3.OkHttpClient
import okhttp3.Request

class ShareReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // THIS is where data comes from
        val productUrl = intent.getStringExtra(Intent.EXTRA_TEXT).toString()
        fetchStoreIds(parseInstamartShareText(productUrl))
    }

//    private fun fetchStoreIds(url: String) {
//        Thread {
//            try {
//                Log.e(
//                    "TAG",
//                    "fetchStoreIds: lat:${PreferenceManager.getLatitude()}   long:${PreferenceManager.getLongitude()}  location:${PreferenceManager.getLocation()}"
//                )
//                val client = OkHttpClient.Builder().addInterceptor(
//                    LocationCookieInterceptor(
//                        PreferenceManager.getLatitude() ?: "0.0".toDouble(), PreferenceManager.getLongitude() ?: "0.0".toDouble()
//                    )
//                ).build()
//
//                val request = Request.Builder().url(url).build()
//
//                val response = client.newCall(request).execute()
//
//                if (!response.isSuccessful) {
//                    Log.e("MainActivity", "❌ Request failed: ${response.code}")
//                    return@Thread
//                }
//
//                val body = response.body?.string() ?: ""
//                Log.e("MainActivity", "📩 Response received (${body.length} chars)")
//
//                val ids = StoreIdExtractor.extractStoreIds(body)
//
//                runOnUiThread {
//                    Log.e("MainActivity", "🎉 Final Store IDs = $ids")
//                    Toast.makeText(this, "Store IDs: $ids", Toast.LENGTH_LONG).show()
//                    if (ids.isNotEmpty()) {
//                        HectorScraper.storeid = ids[0]
//                    } else {
//                        HectorScraper.storeid = ""
//                    }
//                    finishAffinity()
//                }
//
//
//            } catch (e: Exception) {
//                Log.e("MainActivity", "❌ Failed: ${e.message}")
//            }
//        }.start()
//    }
//
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


    private fun getSafeLatLng(): Pair<Double, Double>? {
        val lat = PreferenceManager.getLatitude()
        val lng = PreferenceManager.getLongitude()

        if (lat == null || lng == null || lat == 0.0 || lng == 0.0) {
            Log.e("STORE", "❌ Invalid LatLng: $lat, $lng")
            return null
        }
        return lat to lng
    }

    private fun fetchStoreIds(url: String) {

        val latLng = getSafeLatLng() ?: return

        Thread {
            try {
                Log.e(
                    "STORE",
                    "🌍 Using Lat=${latLng.first}, Lng=${latLng.second}"
                )

                val client = OkHttpClient.Builder()
                    .addInterceptor(
                        LocationCookieInterceptor(
                            latLng.first,
                            latLng.second
                        )
                    )
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("STORE", "❌ HTTP ${response.code}")
                    return@Thread
                }

                val body = response.body?.string().orEmpty()

                val ids = StoreIdExtractor.extractStoreIds(body)

                // 🔁 Retry once if empty
                if (ids.isEmpty()) {
                    Log.e("STORE", "🔁 Retrying request once...")
                    retryFetch(client, url)
                    return@Thread
                }

                postResult(ids)

            } catch (e: Exception) {
                Log.e("STORE", "❌ Exception: ${e.message}", e)
            }
        }.start()
    }

    private fun retryFetch(client: OkHttpClient, url: String) {
        try {
            Thread.sleep(800) // Allow backend to settle location

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            val body = response.body?.string().orEmpty()
            val ids = StoreIdExtractor.extractStoreIds(body)

            postResult(ids)

        } catch (e: Exception) {
            Log.e("STORE", "❌ Retry failed", e)
        }
    }

    private fun postResult(ids: List<String>) {
        runOnUiThread {
            Log.e("STORE", "🎉 Store IDs: $ids")

            HectorScraper.storeid = ids.firstOrNull().orEmpty()

            Toast.makeText(
                this,
                if (ids.isEmpty()) "Store not found" else "Store ID: ${ids[0]}",
                Toast.LENGTH_LONG
            ).show()

            finishAffinity()
        }
    }
}