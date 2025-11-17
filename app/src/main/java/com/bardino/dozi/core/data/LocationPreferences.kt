package com.bardino.dozi.core.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SavedLocation(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String = ""
)

object LocationPreferences {
    private const val PREF_NAME = "location_prefs"
    private const val KEY_LOCATIONS = "saved_locations"
    private val gson = Gson()

    fun saveLocations(context: Context, locations: List<SavedLocation>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(locations)
        prefs.edit()
            .putString(KEY_LOCATIONS, json)
            .apply()
    }

    fun getLocations(context: Context): List<SavedLocation> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LOCATIONS, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<SavedLocation>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addLocation(context: Context, location: SavedLocation) {
        val currentLocations = getLocations(context).toMutableList()
        currentLocations.add(location)
        saveLocations(context, currentLocations)
    }

    fun removeLocation(context: Context, locationId: String) {
        val currentLocations = getLocations(context).toMutableList()
        currentLocations.removeAll { it.id == locationId }
        saveLocations(context, currentLocations)
    }

    fun clearAllLocations(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_LOCATIONS)
            .apply()
    }
}
