package com.hectorscraper.app.api.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import java.net.NetworkInterface
import java.util.*

object NetworkUtils {


    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected ?: false
            } else {
                val nc =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                nc?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false || nc?.hasTransport(
                    NetworkCapabilities.TRANSPORT_WIFI) ?: false
            }
        } else false
    }

    /**
     * VPN can be harmful to our apps, As pro user can capture packets!
     * So check if device is connected to VPN or not?
     */
    fun isVPNConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networks = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(networks)
            /*Log.i(TAG, "VPN transport is: " + caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
        Log.i(TAG, "NOT_VPN capability is: " + caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));*/
            return caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val networks = cm.allNetworks
            var vpn = false
            for (i in networks.indices) {
                val caps = cm.getNetworkCapabilities(networks[i])

                /*Log.i(TAG, "Network " + i + ": " + networks[i].toString());
                Log.i(TAG, "VPN transport is: " + caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
                Log.i(TAG, "NOT_VPN capability is: " + caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));*/

                vpn = vpn || caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

            }
            return vpn
        } else {
            val networkList = ArrayList<String>()
            try {
                for (networkInterface in Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    if (networkInterface.isUp)
                        networkList.add(networkInterface.name)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return networkList.contains("tun0") || networkList.contains("ppp0")
        }
    }
}