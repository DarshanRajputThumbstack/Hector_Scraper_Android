// PreferenceManager.kt
package com.hectorscraper.app.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {

    private const val PREF_NAME = "location_prefs"
    private const val KEY_LATITUDE = "key_latitude"
    private const val KEY_LONGITUDE = "key_longitude"

    private lateinit var prefs: SharedPreferences

    /**
     * Must be called once from Application class
     */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )
    }

    fun saveLocation(latitude: Double, longitude: Double) {
        prefs.edit()
            .putString(KEY_LATITUDE, latitude.toString())
            .putString(KEY_LONGITUDE, longitude.toString())
            .apply()
    }

    fun getLatitude(): Double? =
        prefs.getString(KEY_LATITUDE, null)?.toDoubleOrNull()

    fun getLongitude(): Double? =
        prefs.getString(KEY_LONGITUDE, null)?.toDoubleOrNull()

    fun getLocation(): Extension.LatLng? {
        val lat = getLatitude()
        val lng = getLongitude()
        return if (lat != null && lng != null) {
            Extension.LatLng(lat, lng)
        } else null
    }

    fun hasLocation(): Boolean =
        prefs.contains(KEY_LATITUDE) && prefs.contains(KEY_LONGITUDE)

    fun clear() {
        prefs.edit().clear().apply()
    }
}