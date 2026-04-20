package com.example.micalendariolaboral

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.*

// IMPORTANTE: Usamos el StorageManager compartido
import com.example.micalendariolaboral.App
import com.example.micalendariolaboral.StorageManager

fun getStorageKeyShared(fecha: String): String = fecha // Simplificado para consistencia

class MainActivity : ComponentActivity() {
    
    private lateinit var storage: StorageManager
    private lateinit var database: DatabaseReference
    private lateinit var reminderManager: ReminderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("AgendaLaboral", Context.MODE_PRIVATE)
        val settings = SharedPreferencesSettings(sharedPrefs)
        
        // Inicializamos el StorageManager compartido
        storage = StorageManager(settings)
        val firebaseUrl = "https://parking-team-89394-default-rtdb.firebaseio.com/"
        database = FirebaseDatabase.getInstance(firebaseUrl).reference.child("teletrabajo")
        reminderManager = AndroidReminderManager(this)

        setContent {
            var teletrabajadores by remember { mutableStateOf(listOf<String>()) }
            var tiempoEstimado by remember { mutableStateOf("Calculando...") }
            var ciudadActual by remember { mutableStateOf(storage.getCity() ?: "Ubicación") }
            val selectedDateState = remember { mutableStateOf(LocalDate.now()) }

            App(
                storage = storage,
                onOpenMaps = { direccion -> abrirGoogleMaps(this, direccion) },
                onSetReminder = { fechaStr, mensaje ->
                    val fecha = LocalDate.parse(fechaStr)
                    configurarAlarmaAndroid(fecha, mensaje)
                },
                onDeleteReminder = { fechaStr -> reminderManager.cancelReminder(fechaStr.hashCode()) },
                onUpdateStatus = { fechaStr, estado ->
                    val fecha = LocalDate.parse(fechaStr)
                    actualizarEstadoFirebase(fecha, estado)
                },
                onDateSelected = { fechaStr ->
                    selectedDateState.value = LocalDate.parse(fechaStr)
                },
                teletrabajadores = teletrabajadores,
                tiempoEstimado = tiempoEstimado,
                ciudadActual = ciudadActual
            )
            
            // Escuchador de Firebase para sincronización grupal
            LaunchedEffect(selectedDateState.value) {
                val storageKey = selectedDateState.value.toString()
                database.child(storageKey).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = mutableListOf<String>()
                        for (user in snapshot.children) user.key?.let { list.add(it) }
                        teletrabajadores = list
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            // GPS y Trayecto
            LaunchedEffect(Unit) {
                obtenerCiudadGps(this@MainActivity) { ciudad ->
                    ciudadActual = ciudad
                    storage.saveCity(ciudad) // Guardamos para la próxima vez
                }
                while(true) {
                    val address = storage.getOfficeAddress()
                    if (address != null) {
                        calcularTiempoTrayecto(this@MainActivity, address) { tiempo -> 
                            tiempoEstimado = tiempo 
                        }
                    } else {
                        tiempoEstimado = "Sin oficina"
                    }
                    delay(600000) // Cada 10 min
                }
            }
        }
    }

    private fun actualizarEstadoFirebase(fecha: LocalDate, estado: String) {
        val storageKey = fecha.toString()
        val user = storage.getUserName() ?: "Invitado"
        
        when (estado) {
            "teletrabajo" -> {
                database.child(storageKey).child(user).setValue(true)
                storage.saveDayType(storageKey, "teletrabajo")
            }
            "festivo" -> {
                // Si cambia a festivo, lo borramos de la lista de teletrabajo de los demás
                database.child(storageKey).child(user).removeValue()
                storage.saveDayType(storageKey, "festivo")
            }
            "limpiar" -> {
                database.child(storageKey).child(user).removeValue()
                storage.removeDayData(storageKey)
            }
        }
    }

    private fun configurarAlarmaAndroid(fecha: LocalDate, mensaje: String) {
        val ahora = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            val cal = Calendar.getInstance().apply { set(fecha.year, fecha.monthValue - 1, fecha.dayOfMonth, h, m, 0) }
            storage.saveNote(fecha.toString(), mensaje)
            reminderManager.scheduleReminder(fecha.hashCode(), mensaje, cal.timeInMillis)
        }, ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE), true).show()
    }
}

// FUNCIONES AUXILIARES PARA ANDROID
fun abrirGoogleMaps(context: Context, direccion: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(direccion)}")).setPackage("com.google.android.apps.maps")
    try { context.startActivity(intent) } catch (e: Exception) { 
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(direccion)}"))) 
    }
}

@SuppressLint("MissingPermission")
fun obtenerCiudadGps(context: Context, onResult: (String) -> Unit) {
    LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
        if (loc != null) {
            try { 
                @Suppress("DEPRECATION")
                onResult(Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()?.locality ?: "Desconocida") 
            } catch (e: Exception) { onResult("Error") }
        }
    }
}

@SuppressLint("MissingPermission")
fun calcularTiempoTrayecto(context: Context, direccion: String, onResult: (String) -> Unit) {
    LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
        if (loc != null) {
            try {
                @Suppress("DEPRECATION")
                val office = Geocoder(context, Locale.getDefault()).getFromLocationName(direccion, 1)?.firstOrNull()
                if (office != null) {
                    val lat1 = loc.latitude; val lon1 = loc.longitude
                    val lat2 = office.latitude; val lon2 = office.longitude
                    val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1)
                    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
                    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                    val distanciaKm = 6371.0 * c
                    // Ajustamos el factor: 1.3 es aprox 45-50 km/h de media (más realista para ciudad/periferia)
                    val tiempoEstimadoMin = (distanciaKm * 1.3).roundToInt() + 5 // +5 min por semáforos/aparcamiento
                    onResult("$tiempoEstimadoMin min")
                } else onResult("-- min")
            } catch (e: Exception) { onResult("Err") }
        } else onResult("No GPS")
    }
}
