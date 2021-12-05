package com.example.simpletotp

import android.util.Log
import com.example.simpletotp.totp.SafeTOTPEntry
import com.example.simpletotp.totp.TOTPWrapper

object Singleton {
    var TOTPWrapper: TOTPWrapper? = null
    var safeEntries: ArrayList<SafeTOTPEntry>? = null

    fun globalWrapper(): TOTPWrapper? {
        return TOTPWrapper
    }
    fun globalEntries(): ArrayList<SafeTOTPEntry>? {
        return safeEntries
    }
    fun setWrapper(wrapper: TOTPWrapper) {
        TOTPWrapper = wrapper
    }
    fun setEntries(entries: ArrayList<SafeTOTPEntry>) {
        safeEntries = entries
    }
    fun printEntries() {
        safeEntries?.forEach { Log.d("name is : ", it.name) }
    }
}