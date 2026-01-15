package com.hectorscraper.app.utils

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import com.google.gson.annotations.SerializedName
import com.hectorscraper.app.utils.Extension.androidIdAsMac
import com.hectorscraper.app.utils.Extension.getActualMacAddress
import com.hectorscraper.app.utils.Extension.getOrCreateFakeMac
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

        val categoryList = listOf("Bath and Body", "Hair Care", "Skincare", "Makeup")

//        var pincodeList = listOf("560006", "400070", "400020", "122003")

        //        var pincodeList = listOf("560006", "400070", "395007", "302017", "452010", "500034", "700027", "110003", "400020", "122003")
//        var pincodeList = listOf("560006", "400070", "395007", "302017", "452010", "500034", "700027", "110003", "400020", "122003")
//        val pincodeList = listOf(
//            "395007", // Surat
//            "141001", // Ludhiana
//            "411014", // Pune
//            "313001", // Udaipur
//            "641001",  // Coimbatore
//            "201301", // Noida
//            "400065", // Andheri, Mumbai
//            "110080", // Delhi (Sangam Vihar)
//            "110045", // West Delhi
//            "226001", // Lucknow
//            "122001", // Gurgaon
//            "400001", // Mumbai Central
//            "400601", // Thane
//            "110001", // Delhi Central
//            "560001", // Bangalore
//            "500001", // Hyderabad
//            "600001", // Chennai
//            "411001", // Pune
//            "380001", // Ahmedabad
//            "700001", // Kolkata
//            "302001", // Jaipur
//            "452001", // Indore
//            "492001", // Raipur
//            "500017", // Hyderabad (example reported working)*
//            "390001" // Vadodara
//        )

        val pincodeList = arrayListOf(
//            "395007", // Surat
//            "141001", // Ludhiana
//            "411014", // Pune
//            "313001", // Udaipur
//            "641001",  // Coimbatore
//            "201301", // Noida
//            "400065", // Andheri, Mumbai
//            "110080", // Delhi (Sangam Vihar)
//            "110045", // West Delhi
//            "226001", // Lucknow
//            "122001", // Gurgaon
//            "400001", // Mumbai Central
//            "400601", // Thane
//            "110001", // Delhi Central
//            "560001", // Bangalore
//            "500001", // Hyderabad
//            "600001", // Chennai
//            "411001", // Pune
//            "380001", // Ahmedabad
//            "700001", // Kolkata
//            "302001", // Jaipur
//            "452001", // Indore
//            "492001", // Raipur
//            "500017", // Hyderabad (example reported working)*
//            "390001", // Vadodara
//            "560021", // Store ID can't get
//            "560086", // Store ID can't get
//            "562106", // Service not available at this place
//            "562123", // Service not available at this place
//            "560001",
//            "380009",
//            "380007",
//            "380006",
//            "395009",
//            "395005",
//            "380053",
//            "560006",
//            "560007",
//            "560008",
//            "560010",
//            "560016",
//            "560017",
//            "560022",
//            "560024",
//            "560025",
//            "560027",
//            "560029",
//            "560030",
//            "560032",
//            "560034",
//            "560035",
//            "560037",
//            "560038",
//            "560040",
//            "560043",
//            "560045",
//            "560047",
//            "560048",
//            "560049",
//            "560050",
//            "560053",
//            "560055",
//            "560056",
//            "560058",
//            "560061",
//            "560062",
//            "560064",
//            "560066",
//            "560067",
//            "560068",
//            "560070",
//            "560071",
//            "560072",
//            "560073",
//            "560074",
//            "560075",
//            "560076",
//            "560077",
//            "560078",
//            "560079",
//            "560080",
//            "560083",
//            "560085",
//            "560087",
//            "560090",
//            "560091",
//            "560092",
//            "560093",
//            "560095",
//            "560097",
//            "560099",
//            "560100",
//            "560102",
//            "560103",
//            "560104",
//            "560105",
//            "560110",
//            "562125",
//            "562149",
//            "562157",
//            "562162",
//            "500003",
            "500016",
            "500047",
            "400706",
            "400708",
            "400705",
            "560006",
            "560007",
            "560008",
            "560016",
            "560017",
        )

//        val uniquePincodeList = pincodeList
//            .map { it.trim() }
//            .filter { it.length == 6 && it.all(Char::isDigit) }
//            .distinct()

        var pincodeIndex = 0
        var categoryIndex = 0
        var currentPincode = ""
        var currentCategory = ""
        var killInstamartApp = false
        var isAddressStored = false

        lateinit var appContext: HectorScraper
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        PreferenceManager.init(this)
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