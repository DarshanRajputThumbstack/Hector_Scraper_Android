package com.hectorscraper.app.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.net.NetworkInterface
import java.util.Locale
import java.util.UUID
import kotlin.collections.iterator

object AccessibilityChecker {

    fun isServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponent = ComponentName(context, serviceClass)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").map { it.trim() }.any { it.equals(expectedComponent.flattenToString(), ignoreCase = true) }
    }

    fun generateFakeMac(): String {
        val bytes = ByteArray(6)
        UUID.randomUUID().toString().replace("-", "").chunked(2).take(6).forEachIndexed { index, hex ->
                bytes[index] = hex.toInt(16).toByte()
            }

        // Set local + unicast bits
        bytes[0] = (bytes[0].toInt() and 0xFE or 0x02).toByte()

        val mac = bytes.joinToString(":") {
            "%02X".format(Locale.US, it)
        }

        Log.e("FAKE_MAC", "Generated fake MAC = $mac")

        return mac
    }

    fun getOrCreateFakeMac(context: Context): String {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        val key = "fake_mac"

        val existing = prefs.getString(key, null)
        if (existing != null) {
            Log.e("FAKE_MAC", "Existing fake MAC = $existing")
            return existing
        }

        val newMac = generateFakeMac()
        prefs.edit().putString(key, newMac).apply()

        Log.e("FAKE_MAC", "Generated & saved fake MAC = $newMac")
        return newMac
    }

    fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        )
    }

    fun androidIdAsMac(context: Context): String {
        return getAndroidId(context).chunked(2).take(6).joinToString(":").uppercase()
    }

    fun getActualMacAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (iface in interfaces) {
                if (iface.name.equals("wlan0", ignoreCase = true)) {
                    val macBytes = iface.hardwareAddress
                    if (macBytes != null) {
                        return macBytes.joinToString(":") {
                            "%02X".format(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Android 10+ always ends here
        return "02:00:00:00:00:00"
    }

    @SuppressLint("MissingPermission")
    fun setMockLocation(context: Context, lat: Double, lng: Double) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val provider = LocationManager.GPS_PROVIDER

        try {
            locationManager.addTestProvider(
                provider, false, false, false, false, true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_FINE
            )
        } catch (_: Exception) {
        }

        locationManager.setTestProviderEnabled(provider, true)

        val location = Location(provider).apply {
            latitude = lat
            longitude = lng
            accuracy = 5f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }

        locationManager.setTestProviderLocation(provider, location)

        Log.e("MOCK_LOC", "Location set to $lat , $lng")
    }

    fun pincodeToLatLng(pincode: String): LatLng? {
        return when (pincode) {
            "395007" -> LatLng(21.1702, 72.8311) // Surat
            "110001" -> LatLng(28.6304, 77.2177) // Delhi
            "400001" -> LatLng(18.9388, 72.8354) // Mumbai
            else -> null
        }
    }

    data class LatLng(val lat: Double, val lng: Double)

    fun getLatLngFromPincodeOSM(
        pincode: String, callback: (LatLng?) -> Unit
    ) {
        val url = "https://nominatim.openstreetmap.org/search" + "?postalcode=$pincode" + "&country=India" + "&format=json"

        val client = OkHttpClient()

        val request = Request.Builder().url(url).header("User-Agent", "AndroidApp") // REQUIRED
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: run {
                    callback(null)
                    return
                }

                val array = JSONArray(body)
                if (array.length() == 0) {
                    callback(null)
                    return
                }

                val obj = array.getJSONObject(0)
                val lat = obj.getDouble("lat")
                val lon = obj.getDouble("lon")

                callback(LatLng(lat, lon))
            }
        })
    }

    fun canMockLocation(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun openMockLocationSettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e("MOCK_LOCATION", "Unable to open developer settings")
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}