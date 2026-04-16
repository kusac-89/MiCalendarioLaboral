package com.example.micalendariolaboral

import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import android.content.Context

class StorageManager(context: Context) {
    private val sharedPrefs = context.getSharedPreferences("AgendaLaboral", Context.MODE_PRIVATE)
    private val settings: Settings = SharedPreferencesSettings(sharedPrefs)

    fun saveUserName(name: String) = settings.putString("nombre_usuario", name)
    fun getUserName(): String? = settings.getStringOrNull("nombre_usuario")

    fun saveCity(city: String) = settings.putString("ciudad_usuario", city)
    fun getCity(): String? = settings.getStringOrNull("ciudad_usuario")

    fun saveOfficeAddress(address: String) = settings.putString("oficina_usuario", address)
    fun getOfficeAddress(): String? = settings.getStringOrNull("oficina_usuario")

    fun saveDayType(day: String, type: String) = settings.putString("tipo_$day", type)
    fun getDayType(day: String): String? = settings.getStringOrNull("tipo_$day")

    fun saveNote(day: String, note: String) = settings.putString("nota_$day", note)
    fun getNote(day: String): String? = settings.getStringOrNull("nota_$day")

    fun removeDayData(day: String) {
        settings.remove("tipo_$day")
        settings.remove("nota_$day")
    }

    fun removeNote(day: String) = settings.remove("nota_$day")
    fun getAll(): Map<String, *> = sharedPrefs.all
}
