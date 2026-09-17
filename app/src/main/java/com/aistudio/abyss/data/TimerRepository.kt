package com.aistudio.abyss.data

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.abyss.model.TimerEntry
import org.json.JSONArray
import org.json.JSONObject

class TimerRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("freetimer_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ITEMS = "freetimer_items_json"
    }

    fun loadItems(): List<TimerEntry> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val root = JSONObject(raw)
            val arr = root.optJSONArray("items") ?: return emptyList()
            val list = mutableListOf<TimerEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val entry = TimerEntry.fromJson(obj)
                if (entry != null) list.add(entry)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveItems(items: List<TimerEntry>) {
        try {
            val root = JSONObject()
            val arr = JSONArray()
            items.forEach { arr.put(it.toJson()) }
            root.put("items", arr)
            prefs.edit().putString(KEY_ITEMS, root.toString()).apply()
        } catch (_: Exception) {}
    }
}
