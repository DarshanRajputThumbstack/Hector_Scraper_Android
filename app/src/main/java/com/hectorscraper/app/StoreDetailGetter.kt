package com.hectorscraper.app

import android.util.Log
import org.json.JSONException
import org.json.JSONObject

object StoreIdExtractor {

    private const val TAG = "StoreIdExtractor"

    fun extractStoreIds(responseBody: String): List<String> {
        Log.e(TAG, "🚀 extractStoreIds() START")

        Log.e(TAG, "📦 Response length = ${responseBody.length}")

        if (responseBody.isBlank()) {
            Log.e(TAG, "❌ Response body is EMPTY or BLANK")
            return emptyList()
        }

        // 1. Try pure JSON
        Log.e(TAG, "🧪 Attempting pure JSON parse")
        parseInitialStateJson(responseBody)?.let {
            Log.e(TAG, "✅ Pure JSON parse SUCCESS → StoreIDs=$it")
            return it
        }

        Log.e(TAG, "⚠️ Pure JSON parse FAILED, trying HTML extraction")

        // 2. Try extracting from HTML
        val embeddedJson = extractInitialStateJsonFromHtml(responseBody)
        if (embeddedJson == null) {
            Log.e(TAG, "❌ HTML extraction FAILED (window.___INITIAL_STATE___ not found)")
            return emptyList()
        }

        Log.e(TAG, "📄 Embedded JSON extracted (${embeddedJson.length} chars)")

        val result = parseInitialStateJson(embeddedJson)
        if (result == null) {
            Log.e(TAG, "❌ Embedded JSON parse FAILED")
            return emptyList()
        }

        Log.e(TAG, "🏁 extractStoreIds() END → StoreIDs=$result")
        return result
    }

    private fun parseInitialStateJson(json: String): List<String>? {
        Log.e(TAG, "🔍 parseInitialStateJson() START")
        Log.e(TAG, "📦 JSON length = ${json.length}")

        return try {
            val root = JSONObject(json)
            val ids = ArrayList<String>()

            Log.e(TAG, "✅ Root JSON parsed successfully")

            // Store page
            if (root.has("storeDetailsV2")) {
                Log.e(TAG, "🏪 storeDetailsV2 FOUND")

                val sd = root.getJSONObject("storeDetailsV2")

                val storeId = sd.optString("storeId", "")
                if (storeId.isNotEmpty()) {
                    ids.add(storeId)
                    Log.e(TAG, "✔ storeDetailsV2.storeId = $storeId")
                } else {
                    Log.e(TAG, "⚠️ storeDetailsV2.storeId EMPTY")
                }

                sd.optJSONObject("primaryStore")?.let {
                    val id = it.optString("id", "")
                    if (id.isNotEmpty()) {
                        ids.add(id)
                        Log.e(TAG, "✔ primaryStore.id = $id")
                    } else {
                        Log.e(TAG, "⚠️ primaryStore.id EMPTY")
                    }
                } ?: Log.e(TAG, "⚠️ primaryStore MISSING")

                sd.optJSONArray("secondaryStore")?.let { arr ->
                    Log.e(TAG, "🏬 secondaryStore count = ${arr.length()}")
                    for (i in 0 until arr.length()) {
                        val id = arr.optJSONObject(i)?.optString("id", "")
                        if (!id.isNullOrEmpty()) {
                            ids.add(id)
                            Log.e(TAG, "✔ secondaryStore[$i].id = $id")
                        } else {
                            Log.e(TAG, "⚠️ secondaryStore[$i].id EMPTY")
                        }
                    }
                } ?: Log.e(TAG, "⚠️ secondaryStore ARRAY MISSING")
            } else {
                Log.e(TAG, "ℹ️ storeDetailsV2 NOT PRESENT")
            }

            // Product page
            root.optJSONObject("inventory")
                ?.optJSONObject("product")
                ?.let { productObj ->
                    Log.e(TAG, "📦 inventory.product FOUND")

                    val keys = productObj.keys()
                    while (keys.hasNext()) {
                        val productId = keys.next()
                        val storeId = productObj
                            .optJSONObject(productId)
                            ?.optString("storeId", "")

                        if (!storeId.isNullOrEmpty()) {
                            ids.add(storeId)
                            Log.e(TAG, "✔ product[$productId].storeId = $storeId")
                        } else {
                            Log.e(TAG, "⚠️ product[$productId].storeId EMPTY")
                        }
                    }
                } ?: Log.e(TAG, "ℹ️ inventory.product NOT PRESENT")

            val result = ids.distinct()
            Log.e(TAG, "🎉 parseInitialStateJson() SUCCESS → $result")
            result

        } catch (e: JSONException) {
            Log.e(TAG, "❌ JSON PARSE EXCEPTION: ${e.message}")
            null
        }
    }

    private fun extractInitialStateJsonFromHtml(html: String): String? {
        Log.e(TAG, "🔍 extractInitialStateJsonFromHtml() START")
        Log.e(TAG, "📦 HTML length = ${html.length}")

        val marker = "window.___INITIAL_STATE___"
        val idx = html.indexOf(marker)

        if (idx == -1) {
            Log.e(TAG, "❌ Marker NOT FOUND")
            return null
        }

        Log.e(TAG, "✔ Marker found at index $idx")

        val eq = html.indexOf('=', idx + marker.length)
        if (eq == -1) {
            Log.e(TAG, "❌ '=' NOT FOUND after marker")
            return null
        }

        Log.e(TAG, "✔ '=' found at index $eq")

        var start = -1
        for (i in eq + 1 until html.length) {
            val c = html[i]
            if (c == '{') {
                start = i
                break
            } else if (!c.isWhitespace()) {
                Log.e(TAG, "❌ Unexpected char before JSON start: '$c'")
                return null
            }
        }

        if (start == -1) {
            Log.e(TAG, "❌ JSON opening '{' NOT FOUND")
            return null
        }

        Log.e(TAG, "✔ JSON starts at index $start")

        var depth = 0
        var inString = false
        var escaped = false
        var end = -1

        for (i in start until html.length) {
            val c = html[i]

            if (escaped) {
                escaped = false
                continue
            }

            if (inString) {
                if (c == '\\') escaped = true
                else if (c == '"') inString = false
                continue
            }

            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        end = i
                        break
                    }
                }
            }
        }

        if (end == -1) {
            Log.e(TAG, "❌ JSON closing '}' NOT FOUND")
            return null
        }

        Log.e(TAG, "✔ JSON ends at index $end")

        val extracted = html.substring(start, end + 1)
        Log.e(TAG, "🎯 JSON extracted successfully (${extracted.length} chars)")

        return extracted
    }
}