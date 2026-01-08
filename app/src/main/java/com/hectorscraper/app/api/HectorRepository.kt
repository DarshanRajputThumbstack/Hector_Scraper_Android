package com.hectorscraper.app.api

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.hectorscraper.app.utils.CategoryData
import com.hectorscraper.app.api.repository.BaseRepository

class HectorRepository(applicationContext: Application) : BaseRepository(applicationContext) {

    val categoryData = MutableLiveData<CategoryData>()

    suspend fun doRequestForJobCategory(): MutableLiveData<CategoryData> {
        runApi(
            apiCall = { ApiClient.getAPIInterface().doRequestForCategory() }, apiName = "getCategory"
        )?.let { data ->
            Log.e("TAG", "Category Response: $data")
            categoryData.postValue(data)
        }
        return categoryData

    }
}