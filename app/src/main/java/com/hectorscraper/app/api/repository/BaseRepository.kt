package com.hectorscraper.app.api.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.hectorscraper.app.api.model.NetworkStatus
import com.hectorscraper.app.api.network.NoConnectivityException
import com.google.gson.Gson
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException


open class BaseRepository(private val applicationContext: Application) : SessionRepository {

    companion object {
        private const val MESSAGE_KEY = "message"
        private const val ERROR_KEY = "error"
    }


    private val networkStatus = MutableLiveData<NetworkStatus>()

    private val authenticationStatus = MutableLiveData<Pair<Int, String>>()

    //Remove Arraylist from response its just for demo
//    suspend inline fun <T> runApi(
//        crossinline apiCall: suspend () -> Response<MainResponseModel<T>>,
//        apiName: String,
//        crossinline successBlock: (T) -> Unit = { },
//        networkStatus: MutableLiveData<NetworkStatus>? = null,
//        ext: Any? = null,
//    ): T? {
//        val networkStates = networkStatus ?: getNetworkStates()
//        return try {
//            networkStates.postValue(NetworkStatus.Running(msg = "Running",
//                ext = ext,apiName))
//            //Invoke the function
//            val response = apiCall.invoke()
//            if (response.isSuccessful && response.body() != null) {
//                if (response.body()!!.Data != null) {
//                    val msg = response.body()!!.Message
//                    //Alright our api call was success so post success
//                    networkStates.postValue(
//                        NetworkStatus.Success(msg = msg,
//                        ext = ext,apiName))
//                    successBlock.invoke(response.body()!!.Data)
//                    response.body()!!.Data
//                } else {
//                    //Alright our api call was success so post success
//                    networkStates.postValue(NetworkStatus.Failed(response.body()!!.Message, ext = ext,apiName))
//                    null
//                }
//            } else {
//
//                when (response.code()) {
//                    400 -> networkStates.postValue(
//                        NetworkStatus.Failed(getErrorMessage(response.errorBody())
//                        ?: "Resource not found.", ext,apiName))
//                    401 -> {
//                        // Session Expired
////                        getAuthenticationStatus().postValue(Pair(response.code(), "Authentication Token has Expired."))
////                        sessionLogout()
////                        getNetworkStates().postValue(NetworkStatus.Failed(getErrorMessage(response.errorBody()) ?: "Authentication Token has Expired."))
//                        getNetworkStates().postValue(
//                            NetworkStatus.SessionExpired(getErrorMessage(
//                            response.errorBody()) ?: "Authentication Token has Expired.", ext,apiName))
//                    }
//                    500 -> networkStates.postValue(
//                        NetworkStatus.Failed(getErrorMessage(response.errorBody())
//                        ?: "Internal server error.", ext,apiName))
//                    else -> networkStates.postValue(
//                        NetworkStatus.Failed(getErrorMessage(response.errorBody())
//                        ?: "Something went wrong", ext,apiName))
//                }
//
//                null
//            }
//        } catch (e: Exception) {
//            //Ooops we are in trouble lets dig our problem and post the status and return null as we didn't got any data
//            e.printStackTrace()
//            when (e) {
//                //Lets dive into the exceptions
//                is HttpException -> {
//                    val body = e.response()?.errorBody()
//                    networkStates.postValue(
//                        NetworkStatus.Failed(getErrorMessage(body)
//                        ?: "Something went wrong.", ext,apiName))
//                }
//                is SocketTimeoutException -> networkStates.postValue(
//                    NetworkStatus.Internet("Time out",
//                    ext,apiName))
//                is IOException -> networkStates.postValue(
//                    NetworkStatus.Internet(e.message
//                    ?: "Internet Connection not available.", ext,apiName))
//                is NoConnectivityException -> networkStates.postValue(
//                    NetworkStatus.Internet(e.message,
//                    ext,apiName))
//                else -> {
//                    networkStates.postValue(NetworkStatus.Failed(e.message ?: "", ext,apiName))
//                }
//            }
//            //We got nothing here so return null :( As our api has failed
//            null
//        }
//    }

    suspend inline fun <T> runApi(
        crossinline apiCall: suspend () -> Response<T>,
        apiName: String,
        crossinline successBlock: (T) -> Unit = { },
        networkStatus: MutableLiveData<NetworkStatus>? = null,
        ext: Any? = null
    ): T? {

        val networkStates = networkStatus ?: getNetworkStates()

        return try {
            networkStates.postValue(
                NetworkStatus.Running(
                    msg = "Running",
                    ext = ext,
                    apiName = apiName
                )
            )

            // Call API
            val response = apiCall()

            // --- SUCCESS RESPONSE ---
            if (response.isSuccessful) {

                val body = response.body()
                if (body != null) {

                    networkStates.postValue(
                        NetworkStatus.Success(
                            msg = body.toString(),
                            ext = ext,
                            apiName = apiName
                        )
                    )

                    successBlock(body)
                    return body
                }

                // API Returned Success but Data = null
                networkStates.postValue(
                    NetworkStatus.Failed(
                        msg = body.toString(),
                        ext = ext,
                        apiName = apiName
                    )
                )
                return null
            }

            // --- ERROR STATUS CODES ---
            val errorMessage = getErrorMessage(response.errorBody()) ?: "Something went wrong."

            when (response.code()) {
                400 -> networkStates.postValue(NetworkStatus.Failed(errorMessage, ext, apiName))
                401 -> networkStates.postValue(NetworkStatus.SessionExpired(errorMessage, ext, apiName))
                500 -> networkStates.postValue(NetworkStatus.Failed("Internal server error.", ext, apiName))
                else -> networkStates.postValue(NetworkStatus.Failed(errorMessage, ext, apiName))
            }

            null

        } catch (e: Exception) {
            e.printStackTrace()

            when (e) {
                is HttpException -> {
                    val body = e.response()?.errorBody()
                    networkStates.postValue(
                        NetworkStatus.Failed(
                            getErrorMessage(body) ?: "Something went wrong.",
                            ext,
                            apiName
                        )
                    )
                }
                is SocketTimeoutException -> networkStates.postValue(
                    NetworkStatus.Internet("Time out", ext, apiName)
                )
                is IOException -> networkStates.postValue(
                    NetworkStatus.Internet(
                        e.message ?: "Internet Connection not available.",
                        ext,
                        apiName
                    )
                )
                is NoConnectivityException -> networkStates.postValue(
                    NetworkStatus.Internet(e.message ?: "No internet", ext, apiName)
                )
                else -> networkStates.postValue(
                    NetworkStatus.Failed(e.message ?: "Unknown error", ext, apiName)
                )
            }
            null
        }
    }


    /**
     * This will
     */
    fun getErrorMessage(responseBody: ResponseBody?): String? {
        return try {
            val jsonObject = JSONObject(responseBody!!.string())
            when {
                jsonObject.has(MESSAGE_KEY) -> jsonObject.getString(MESSAGE_KEY)
                jsonObject.has(ERROR_KEY) -> jsonObject.getString(ERROR_KEY)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getNetworkStates() = networkStatus

    override fun getAuthenticationStatus() = authenticationStatus


    suspend inline fun <String> runApi1(
        crossinline apiCall: suspend () -> Response<String>,
        crossinline successBlock1: (kotlin.String) -> Unit = { },
        networkStatus: MutableLiveData<NetworkStatus>? = null,
        ext: Any? = null,
    ): kotlin.String? {
        val networkStates = networkStatus ?: getNetworkStates()
        return try {
            networkStates.postValue(NetworkStatus.Running("Running",ext,""))
            //Invoke the function
            val response = apiCall.invoke()
            if (response.isSuccessful) {
                if (response.body() != null) {
                    //Alright our api call was success so post success
//                    networkStates.postValue(NetworkStatus.Success(msg = msg,ext = ext))
//                    deserializedContainerObject = gson.fromJson(response,
//                        DeserializedContainer::class.java)
//                    if (myJSON != null) {
//                        myJSON = bufferedReaderObject.readLine();
//                        completeJSONdata += myJSON;
//                    }
                    val gson = Gson()
                    Log.e("responssgson", "------>" + gson.toJson(response))
                    networkStates.postValue(
                        NetworkStatus.Success(msg = "success",
                        ext = ext,""))
                    successBlock1.invoke(response.body().toString())
//                    Log.e("tagg","------->"+response.body().toString().toByteArray())
//                    Log.e("tagg1","------->"+response.body().toString().toByte())
                    Log.e("tagg2", "------->" + response.body().toString().toUByte())
//                    Log.e("tagg3","------->"+response.body().toString().encodeToByteArray())
//                    Log.e("tagg4","------->"+response.body().toString().commonAsUtf8ToByteArray())
                    return response.body().toString()
                } else {
                    //Alright our api call was success so post success
                    networkStates.postValue(NetworkStatus.Failed("Failed","",""))
                    return null
                }
            } else {

                when (response.code()) {
                    400 -> networkStates.postValue(
                        NetworkStatus.Failed(getErrorMessage(response.errorBody())
                        ?: "Resource not found.","",""))
                    401 -> {
                        // Session Expired
//                        getAuthenticationStatus().postValue(Pair(response.code(), "Authentication Token has Expired."))
//                        sessionLogout()
//                        getNetworkStates().postValue(NetworkStatus.Failed(getErrorMessage(response.errorBody()) ?: "Authentication Token has Expired."))
                        getNetworkStates().postValue(
                            NetworkStatus.SessionExpired(getErrorMessage(
                            response.errorBody()) ?: "Authentication Token has Expired.","",""))
                    }
                    500 -> networkStates.postValue(
                        NetworkStatus.Failed(getErrorMessage(response.errorBody())
                        ?: "Internal server error.","",""))
                    else -> networkStates.postValue(
                        NetworkStatus.Failed(getErrorMessage(response.errorBody())
                        ?: "Something went wrong","",""))
                }

                return null
            }
        } catch (e: Exception) {
            //Ooops we are in trouble lets dig our problem and post the status and return null as we didn't got any data
            e.printStackTrace()
            when (e) {
                //Lets dive into the exceptions
                is HttpException -> {
                    val body = e.response()?.errorBody()
                    networkStates.postValue(
                        NetworkStatus.Failed(getErrorMessage(body)
                        ?: "Something went wrong.","",""))
                }
                is SocketTimeoutException -> networkStates.postValue(NetworkStatus.Internet("Time out","",""))
                is IOException -> networkStates.postValue(
                    NetworkStatus.Internet(e.message
                    ?: "Internet Connection not available.","",""))
                is NoConnectivityException -> networkStates.postValue(NetworkStatus.Internet(e.message,"",""))
                else -> {
                    networkStates.postValue(NetworkStatus.Failed(e.message ?: "","",""))
                }
            }
            //We got nothing here so return null :( As our api has failed
            return null
        }
    }
}