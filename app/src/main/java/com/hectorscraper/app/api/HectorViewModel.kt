package com.hectorscraper.app.api

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hectorscraper.app.api.HectorRepository
import com.hectorscraper.app.api.network.NetworkViewModel
import kotlinx.coroutines.launch

class HectorViewModel(applicationContext: Application) : NetworkViewModel(applicationContext) {

    override val repository = HectorRepository(applicationContext)

    var categoryData = repository.categoryData

    fun doRequestForJobCategory() {
        viewModelScope.launch {
            try {
                repository.doRequestForJobCategory()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class HectorViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HectorViewModel::class.java)) {
            return HectorViewModel(application) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}