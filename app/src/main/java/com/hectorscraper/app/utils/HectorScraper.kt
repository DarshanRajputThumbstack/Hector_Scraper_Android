package com.hectorscraper.app.utils

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import com.google.gson.annotations.SerializedName
import com.hectorscraper.app.utils.AccessibilityChecker.androidIdAsMac
import com.hectorscraper.app.utils.AccessibilityChecker.getActualMacAddress
import com.hectorscraper.app.utils.AccessibilityChecker.getOrCreateFakeMac
import dagger.hilt.android.HiltAndroidApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class HectorScraper : Application(), LifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        var keyword = "Moisturizer"
        var productUrl = ""
        var storeid = ""

        var currentLat = 0.0
        var currentLng = 0.0

        var pincodeList = listOf("560006", "400070", "395007", "302017", "452010", "500034", "700027", "110003", "400020", "122003")

//        var pincodeList = listOf("560006", "400070", "400020", "122003")
        var currentIndex = 0
        var killInstamartApp = false
        var isAddressStored = false

        lateinit var appContext: HectorScraper
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        ExcelManager.createExcelIfNotExists(this)
        val actualMac = androidIdAsMac(this)
        Log.e("FAKE_MAC", "Actual Device MAC = $actualMac")

        val fakeMac = getOrCreateFakeMac(this)
        Log.e("FAKE_MAC", "Fake MAC (APP ONLY) = $fakeMac")
        Log.e("FAKE_MAC", "WLAN MAC = ${getActualMacAddress()}")
    }

    fun getTodayDate(): String {
        val now = System.currentTimeMillis()
        val date = Date(now)
        val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        Log.e("TIME", format.format(date))
        return format.format(date)
    }

    fun getCurrentTime(): String {
        val now = System.currentTimeMillis()
        val date = Date(now)
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        Log.e("TIME", format.format(date))
        return format.format(date)
    }
}

data class CategoryData(
    @SerializedName("category") val category: String? = "",
)

data class LatLng(val lat: Double, val lng: Double)