package com.hectorscraper.app.api.model

sealed class NetworkStatus {
    data class Running(val msg: String = "Running", val ext: Any? = null,val apiName: String) : NetworkStatus()

    data class Success(override val msg: String = "Success", val ext: Any? = null, val apiName: String) : StatusMessage, NetworkStatus()

    data class Internet(override val msg: String = "Unable to connect check your connectivity settings.", val ext: Any? = null, val apiName: String) :
        StatusMessage, NetworkStatus()

    data class Failed(override val msg: String = "Server is temporary down! Please try after sometime.", val ext: Any? = null, val apiName: String) :
        StatusMessage, NetworkStatus()

    data class SessionExpired(override val msg: String = "Authentication Token has Expired.", val ext: Any? = null,  val apiName: String) :
        StatusMessage, NetworkStatus()
}

interface StatusMessage {
    val msg: String
}