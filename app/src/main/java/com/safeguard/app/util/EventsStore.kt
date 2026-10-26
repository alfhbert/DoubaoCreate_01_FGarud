package com.safeguard.app.util

import android.content.Context
import android.content.SharedPreferences

object EventsStore {

    data class Event(val ts: Long, val title: String, val detail: String)

    private const val KEY_EVENTS = "events"
    private const val MAX = 200

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("safeguard_events", Context.MODE_PRIVATE)

    fun add(context: Context, kind: String, title: String, detail: String) {
        val list = load(context).toMutableList()
        list.add(0, Event(System.currentTimeMillis(), title, detail))
        val trimmed = if (list.size > MAX) list.subList(0, MAX) else list
        save(context, trimmed)
    }

    fun load(context: Context): List<Event> {
        val raw = prefs(context).getString(KEY_EVENTS, null) ?: return emptyList()
        return try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<Event>>() {}.type
            val list = gson.fromJson<List<Event>>(raw, type)
            list ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_EVENTS).apply()
    }

    private fun save(context: Context, list: List<Event>) {
        val gson = com.google.gson.Gson()
        prefs(context).edit().putString(KEY_EVENTS, gson.toJson(list)).apply()
    }
}
