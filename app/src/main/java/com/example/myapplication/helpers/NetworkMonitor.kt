package com.example.myapplication.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Перевірка наявності інтернету перед відправкою в хмару (вимога ТЗ Л.Р. №3). */
class NetworkMonitor(private val context: Context) {
    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}