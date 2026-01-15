package com.hectorscraper.app.utils

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class LocationCookieInterceptor(
    private val lat: Double,
    private val lng: Double
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val cookieValue =
            "lat=$lat; lng=$lng; latitude=$lat; longitude=$lng"

        Log.e("COOKIE", "➡️ Sending Cookies: $cookieValue")
        Log.e("COOKIE", "➡️ Request URL: ${originalRequest.url}")

        val requestWithCookies = originalRequest.newBuilder()
            .addHeader("Cookie", cookieValue)
            .addHeader("Cache-Control", "no-cache")
//            .addHeader("User-Agent", "Swiggy-Android")
            .build()
        Log.e("COOKIE", "➡️ Request URL: ${requestWithCookies}")


        val response = chain.proceed(requestWithCookies)

        // Log response cookies
        val responseCookies = response.headers("Set-Cookie")
        if (responseCookies.isNotEmpty()) {
            responseCookies.forEach {
                Log.e("COOKIE", "⬅️ Received Set-Cookie: $it")
            }
        } else {
            Log.e("COOKIE", "⚠️ No Set-Cookie received")
        }

        return response
    }
}
