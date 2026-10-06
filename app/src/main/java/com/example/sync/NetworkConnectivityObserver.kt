package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkConnectivityObserver(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isNetworkAvailable = MutableStateFlow(checkInitialConnection())
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private fun checkInitialConnection(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun startListening(onConnected: () -> Unit) {
        if (networkCallback != null) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("NetworkObserver", "Internet connection became available")
                _isNetworkAvailable.value = true
                CoroutineScope(Dispatchers.Main).launch {
                    onConnected()
                }
            }

            override fun onLost(network: Network) {
                Log.d("NetworkObserver", "Internet connection lost")
                _isNetworkAvailable.value = false
            }
        }

        networkCallback = callback
        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e("NetworkObserver", "Failed to register network callback", e)
        }
    }

    fun stopListening() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.e("NetworkObserver", "Failed to unregister network callback", e)
            }
            networkCallback = null
        }
    }
}
