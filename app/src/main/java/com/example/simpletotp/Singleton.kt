package com.example.simpletotp

import android.util.Log
import com.example.simpletotp.totp.SafeTOTPEntry
import com.example.simpletotp.totp.TOTPWrapper

object Singleton {
    var TOTPWrapper: TOTPWrapper? = null
    var safeEntries: ArrayList<SafeTOTPEntry>? = null
    var favEntries: MutableList<SafeTOTPEntry>? = mutableListOf()

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
        safeEntries = quicksort(entries)


    }

    fun printEntries() {
        safeEntries?.forEach { Log.d("name is : ", it.name) }
    }

    fun setFavEntry(entry: SafeTOTPEntry) {
        favEntries?.add(entry)
        safeEntries?.remove(entry)
        safeEntries?.removeAll(favEntries!!)
        favEntries = quicksort(favEntries)
        var toSet = arrayListOf<SafeTOTPEntry>()
        toSet.addAll(favEntries!!)
        toSet.addAll(safeEntries!!)
        safeEntries = toSet

    }

    fun removerFavEntry(entry: SafeTOTPEntry){
        favEntries?.remove(entry)
        safeEntries?.removeAll(favEntries!!)
        safeEntries = quicksort(safeEntries)
        var toSet = arrayListOf<SafeTOTPEntry>()
        toSet.addAll(favEntries!!)
        toSet.addAll(safeEntries!!)
        safeEntries = toSet
    }

    fun quicksort(items: ArrayList<SafeTOTPEntry>?): ArrayList<SafeTOTPEntry> {
        if (items?.count()!! < 2) {
            return items
        }
        val pivot = items[items.count() / 2]

        val equal = items.filter { it.name.compareTo(pivot.name) == 0 }
//    println("pivot value is : "+equal)

        val less = items.filter { it.name.compareTo(pivot.name) < 0 }
//    println("Lesser values than pivot : "+less)

        val greater = items.filter { it.name.compareTo(pivot.name) > 0 }
//    println("Greater values than pivot : "+greater)

        var lessThan = arrayListOf<SafeTOTPEntry>()
        lessThan.addAll(less)
        var greaterThan = arrayListOf<SafeTOTPEntry>()
        greaterThan.addAll(greater)
        var equalThan = arrayListOf<SafeTOTPEntry>()
        equalThan.addAll(equal)

        var final = arrayListOf<SafeTOTPEntry>()

        lessThan = quicksort(lessThan)
        //equalThan doesn't need to change
        greaterThan = quicksort(greaterThan)

        final.addAll(lessThan + equalThan + greaterThan)

        //return quicksort(lessThan) + equalThan + quicksort(greaterThan)
        return final
    }

    fun quicksort(items: MutableList<SafeTOTPEntry>?): MutableList<SafeTOTPEntry> {
        if (items?.count()!! < 2) {
            return items
        }
        val pivot = items[items.count() / 2]

        val equal = items.filter { it.name.compareTo(pivot.name) == 0 }
//    println("pivot value is : "+equal)

        val less = items.filter { it.name.compareTo(pivot.name) < 0 }
//    println("Lesser values than pivot : "+less)

        val greater = items.filter { it.name.compareTo(pivot.name) > 0 }
//    println("Greater values than pivot : "+greater)

        var lessThan = arrayListOf<SafeTOTPEntry>()
        lessThan.addAll(less)
        var greaterThan = arrayListOf<SafeTOTPEntry>()
        greaterThan.addAll(greater)
        var equalThan = arrayListOf<SafeTOTPEntry>()
        equalThan.addAll(equal)

        var final = arrayListOf<SafeTOTPEntry>()

        lessThan = quicksort(lessThan)
        //equalThan doesn't need to change
        greaterThan = quicksort(greaterThan)

        final.addAll(lessThan + equalThan + greaterThan)

        //return quicksort(lessThan) + equalThan + quicksort(greaterThan)
        return final
    }
}