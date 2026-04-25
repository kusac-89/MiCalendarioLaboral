package com.example.micalendariolaboral

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.CurrentLocationRequest
import com.google.firebase.database.*
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.*
import kotlin.math.*

import com.example.micalendariolaboral.App
import com.example.micalendariolaboral.StorageManager

class MainActivity : ComponentActivity() {
    
    private lateinit var storage: StorageManager
    private lateinit var database: DatabaseReference
    private lateinit var reminderManager: ReminderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("AgendaLaboral", Context.MODE_PRIVATE)
        val settings = SharedPreferencesSettings(sharedPrefs)
        
        storage = StorageManager(settings)
        val firebaseUrl = "https://parking-team-89394-default-rtdb.firebaseio.com/"
        database = FirebaseDatabase.getInstance(firebaseUrl).reference.child("teletrabajo")
        reminderManager = AndroidReminderManager(this)

        setContent {
            var teletrabajadores by remember { mutableStateOf(listOf<String>()) }
            var todosLosMiembros by remember { mutableStateOf(listOf<String>()) }
            var ultimoAviso by remember { mutableStateOf<String?>(null) }
            var tiempoEstimado by remember { mutableStateOf("Calculando...") }
            var ciudadActual by remember { mutableStateOf(storage.getCity() ?: "Ubicación") }
            var climaTemp by remember { mutableStateOf(storage.getNote("temp_cache") ?: "22°C") }
            var climaIcon by remember { mutableStateOf(storage.getNote("icon_cache") ?: "☀️") }
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
                    when (fechaStr) {
                        "register" -> {
                            val name = estado
                            FirebaseDatabase.getInstance().reference.child("miembros").child(name).child("lastSeen").setValue(System.currentTimeMillis())
                            // Notificar registro
                            FirebaseDatabase.getInstance().reference.child("avisos").setValue("$name se ha unido al equipo")
                        }
                        "unregister" -> {
                            val name = storage.getUserName() ?: ""
                            if (name.isNotBlank()) {
                                FirebaseDatabase.getInstance().reference.child("miembros").child(name).removeValue()
                                // Notificar baja
                                FirebaseDatabase.getInstance().reference.child("avisos").setValue("$name ha dejado el equipo")
                                storage.saveUserName("")
                            }
                        }
                        else -> {
                            val fecha = LocalDate.parse(fechaStr)
                            actualizarEstadoFirebase(fecha, estado)
                        }
                    }
                },
                onDateSelected = { fechaStr ->
                    selectedDateState.value = LocalDate.parse(fechaStr)
                },
                teletrabajadores = teletrabajadores,
                todosLosMiembros = todosLosMiembros,
                ultimoAviso = ultimoAviso,
                tiempoEstimado = tiempoEstimado,
                ciudadActual = ciudadActual,
                climaTemp = climaTemp,
                climaIcon = climaIcon
            )

            // Listener de AVISOS (Notificaciones)
            LaunchedEffect(Unit) {
                var esPrimeraCarga = true // Flag para ignorar el aviso antiguo al arrancar
                FirebaseDatabase.getInstance().reference.child("avisos").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val aviso = snapshot.getValue(String::class.java)
                        if (aviso != null) {
                            if (!esPrimeraCarga) {
                                ultimoAviso = aviso
                            }
                            esPrimeraCarga = false
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            // Temporizador para ocultar el aviso
            LaunchedEffect(ultimoAviso) {
                if (ultimoAviso != null) {
                    delay(5000)
                    ultimoAviso = null
                }
            }
            
            // Listener de Teletrabajadores del día
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

            // Listener Global de MIEMBROS (EQUIPO)
            LaunchedEffect(Unit) {
                // AUTO-REGISTRO: Si ya tenemos nombre, actualizamos su última conexión
                storage.getUserName()?.let { name ->
                    if (name.isNotBlank()) {
                        FirebaseDatabase.getInstance().reference.child("miembros").child(name).child("lastSeen").setValue(System.currentTimeMillis())
                    }
                }

                FirebaseDatabase.getInstance().reference.child("miembros").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = mutableListOf<String>()
                        val ahora = System.currentTimeMillis()
                        val limiteInactividad = 10L * 24 * 60 * 60 * 1000 // Reducido a 10 días

                        for (userSnapshot in snapshot.children) {
                            val name = userSnapshot.key ?: continue
                            val lastSeen = userSnapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                            
                            // Solo añadimos si ha estado activo en los últimos 10 días
                            if (ahora - lastSeen < limiteInactividad) {
                                list.add(name)
                            }
                        }
                        todosLosMiembros = list.filter { it.isNotBlank() }.sorted()
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            // GPS, Trayecto y Clima
            LaunchedEffect(Unit) {
                // Ejecutar inmediatamente una vez al inicio
                obtenerCiudadGps(this@MainActivity) { ciudad ->
                    ciudadActual = ciudad
                    storage.saveCity(ciudad)
                }
                storage.getOfficeAddress()?.let { address ->
                    calcularTiempoTrayecto(this@MainActivity, address) { tiempo -> 
                        tiempoEstimado = tiempo 
                    }
                }

                // Bucle de actualización periódica
                while(true) {
                    val address = storage.getOfficeAddress()
                    if (address != null) {
                        calcularTiempoTrayecto(this@MainActivity, address) { tiempo -> tiempoEstimado = tiempo }
                    }
                    
                    obtenerClimaReal(this@MainActivity) { temp, icon ->
                        if (temp != "--°C") {
                            climaTemp = temp
                            climaIcon = icon
                            storage.saveNote("temp_cache", temp)
                            storage.saveNote("icon_cache", icon)
                        }
                    }
                    
                    delay(300000) // Actualizar cada 5 minutos
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

    private fun obtenerClimaReal(context: Context, onResult: (String, String) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val url = "https://api.open-meteo.com/v1/forecast?latitude=${loc.latitude}&longitude=${loc.longitude}&current_weather=true"
                    Thread {
                        try {
                            val response = java.net.URL(url).readText()
                            // Extracción más segura buscando patrones numéricos
                            val tempRegex = "\"temperature\":([0-9.-]+)".toRegex()
                            val codeRegex = "\"weathercode\":([0-9]+)".toRegex()
                            
                            val tempMatch = tempRegex.find(response)
                            val codeMatch = codeRegex.find(response)
                            
                            if (tempMatch != null && codeMatch != null) {
                                val tempValue = tempMatch.groupValues[1].toDouble().roundToInt()
                                val code = codeMatch.groupValues[1].toInt()
                                
                                val icon = when(code) {
                                    0 -> "☀️"
                                    1, 2, 3 -> "⛅"
                                    45, 48 -> "🌫️"
                                    51, 53, 55, 61, 63, 65 -> "🌧️"
                                    71, 73, 75 -> "❄️"
                                    95, 96, 99 -> "⛈️"
                                    else -> "☁️"
                                }
                                runOnUiThread { onResult("$tempValue°C", icon) }
                            } else {
                                runOnUiThread { onResult("--°C", "☁️") }
                            }
                        } catch (e: Exception) { 
                            runOnUiThread { onResult("--°C", "❓") } 
                        }
                    }.start()
                } else {
                    // Si no hay última localización, reintentamos con una petición fresca si fuera necesario
                    onResult("--°C", "📍?") 
                }
            }
        } catch (e: SecurityException) {
            onResult("Err", "🔒")
        }
    }
}

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
    val storage = StorageManager(SharedPreferencesSettings(context.getSharedPreferences("AgendaLaboral", Context.MODE_PRIVATE)))
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    // Intentar obtener la ubicación actual precisa de forma forzada
    val request = CurrentLocationRequest.Builder()
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMaxUpdateAgeMillis(60000) // Máximo 1 minuto de antigüedad
        .build()

    fusedLocationClient.getCurrentLocation(request, null).addOnSuccessListener { loc ->
        // Si falla la ubicación fresca, intentamos con la última conocida por si acaso
        if (loc == null) {
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) procesarUbicacion(context, lastLoc, storage, onResult)
                else onResult("GPS off")
            }
        } else {
            procesarUbicacion(context, loc, storage, onResult)
        }
    }.addOnFailureListener {
        onResult("Error GPS")
    }
}

private fun procesarUbicacion(context: Context, loc: android.location.Location, storage: StorageManager, onResult: (String) -> Unit) {
    try {
        @Suppress("DEPRECATION")
        val geocoder = Geocoder(context, Locale.getDefault())
        
        val officeAddress = storage.getOfficeAddress() ?: "C. Marie Curie, 17, 28521 Rivas-Vaciamadrid, Madrid"
        val homeAddress = storage.getHomeAddress() ?: ""
        
        val office = geocoder.getFromLocationName(officeAddress, 1)?.firstOrNull()
        
        if (office != null) {
            val lat1 = loc.latitude; val lon1 = loc.longitude
            val lat2 = office.latitude; val lon2 = office.longitude
            val distOficina = sqrt((lat1 - lat2).pow(2) + (lon1 - lon2).pow(2))
            
            val destinoFinal = if (distOficina < 0.002 && homeAddress.isNotBlank()) homeAddress else officeAddress
            val esVueltaACasa = destinoFinal == homeAddress
            
            val target = if (esVueltaACasa) geocoder.getFromLocationName(homeAddress, 1)?.firstOrNull() else office
            
            if (target != null) {
                val dLat = Math.toRadians(target.latitude - lat1); val dLon = Math.toRadians(target.longitude - lon1)
                val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(target.latitude)) * sin(dLon / 2).pow(2)
                val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                val distanciaKm = 6371.0 * c
                val tiempoEstimadoMin = (distanciaKm * 1.5).roundToInt() + 5
                
                val suffix = if (esVueltaACasa) " a casa" else " a oficina"
                onResult("$tiempoEstimadoMin min$suffix")
            } else onResult("-- min")
        } else onResult("-- min")
    } catch (e: Exception) { 
        onResult("Err")
    }
}
