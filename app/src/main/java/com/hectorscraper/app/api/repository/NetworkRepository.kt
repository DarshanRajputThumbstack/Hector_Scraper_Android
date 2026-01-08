package com.hectorscraper.app.api.repository

import androidx.lifecycle.MutableLiveData
import com.hectorscraper.app.api.model.NetworkStatus

interface NetworkRepository {
    fun getNetworkStates(): MutableLiveData<NetworkStatus>
}