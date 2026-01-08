package com.hectorscraper.app.api.repository

import androidx.lifecycle.MutableLiveData

interface SessionRepository : NetworkRepository {
    fun getAuthenticationStatus(): MutableLiveData<Pair<Int, String>>
}