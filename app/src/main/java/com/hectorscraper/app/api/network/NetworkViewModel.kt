package com.hectorscraper.app.api.network

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.hectorscraper.app.api.model.NetworkStatus
import com.hectorscraper.app.api.repository.NetworkRepository

abstract class NetworkViewModel(private val applicationContext: Application): ViewModel() {
    abstract val repository: NetworkRepository

    private val networkStatus: LiveData<NetworkStatus> by lazy { repository.getNetworkStates() }

    fun getNetworkStates() = networkStatus
}