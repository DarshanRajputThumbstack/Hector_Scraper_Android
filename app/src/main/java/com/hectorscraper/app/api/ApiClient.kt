package com.hectorscraper.app.api

import android.content.Context
import com.hectorscraper.app.api.network.ConnectivityInterceptor
import com.google.gson.GsonBuilder
import com.hectorscraper.app.BuildConfig
import com.hectorscraper.app.utils.HectorScraper
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiClient {

    private val interceptor = Interceptor { chain ->
        var request = chain.request()
        val builder = request.newBuilder().addHeader("Cache-Control", "no-cache")
        request = builder.build()
        chain.proceed(request)
    }

    fun getAPIInterface(): ApiInterface {
        val gson = GsonBuilder().setLenient().create()

        Logger.addLogAdapter(object : AndroidLogAdapter() {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return BuildConfig.DEBUG
            }
        })
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(getHttpClient(HectorScraper.appContext))
            .build()
            .create(ApiInterface::class.java)
    }

//    @Singleton
//    @Provides
//    fun <T> getClient(context: Context, service: Class<T>, url: String): T {
//        val gson = GsonBuilder().setLenient().create()
//
//        Logger.addLogAdapter(object : AndroidLogAdapter() {
//            override fun isLoggable(priority: Int, tag: String?): Boolean {
//                return BuildConfig.DEBUG
//            }
//        })
//
//        return Retrofit.Builder().baseUrl(BuildConfig.BASE_URL).client(getHttpClient(context))
//            .addConverterFactory(GsonConverterFactory.create(gson)).addConverterFactory(ScalarsConverterFactory.create())
//            .addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build().create(service)
//    }

    @Singleton
    @Provides
    private fun getHttpClient(context: Context): OkHttpClient {
        val client: OkHttpClient
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            client = OkHttpClient().newBuilder().addInterceptor(interceptor).addInterceptor(logging)
                .addInterceptor(ConnectivityInterceptor(context)).connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS).build()
        } else {
            client = OkHttpClient().newBuilder().addInterceptor(interceptor).addInterceptor(ConnectivityInterceptor(context))
                .connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
        }
        return client
    }
}