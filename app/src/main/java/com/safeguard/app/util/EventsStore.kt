package com.safeguard.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object EventsStore {
    private const val KEY = "events_v1"
    private const val MAX = 200
    private val gson = Gson()

    data class Ev(
        val ts: Long,
        val kind: String,
        val title: String,
        val detail: String
    )

    fun load(context: Context): List<Ev> {
        val raw = context.getSharedPreferences("safeguard", Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<MutableList<Ev>>() {}.type
            gson.fromJson<List<Ev>>(raw, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, kind: String, title: String, detail: String) {
        val sp = context.getSharedPreferences("safeguard", Context.MODE_PRIVATE)
        val list = load(context).toMutableList()
        list.add(0, Ev(System.currentTimeMillis(), kind, title, detail))
        while (list.size > MAX) list.removeAt(list.size - 1)
        sp.edit().putString(KEY, gson.toJson(list)).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences("safeguard", Context.MODE_PRIVATE)
            .edit().putString(KEY, "[]").apply()
    }
}
