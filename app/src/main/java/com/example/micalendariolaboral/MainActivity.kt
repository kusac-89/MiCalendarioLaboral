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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import kotlin.math.*

val iOSBlue = Color(0xFF007AFF)
val iOSBackground = Color(0xFFF2F2F7)
val iOSCardWhite = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    private lateinit var storage: StorageManager
    private lateinit var database: DatabaseReference
    private lateinit var reminderManager: ReminderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = StorageManager(this)
        database = FirebaseDatabase.getInstance().reference.child("teletrabajo")
        reminderManager = AndroidReminderManager(this)

        setContent {
            MaterialTheme(
                typography = Typography(
                    bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif, letterSpacing = (-0.3).sp),
                    titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
                ),
                colorScheme = lightColorScheme(
                    primary = iOSBlue,
                    background = iOSBackground,
                    surface = iOSCardWhite
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = iOSBackground) {
                    MainScreen(storage, database, reminderManager)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(storage: StorageManager, database: DatabaseReference, reminderManager: ReminderManager) {
    val context = LocalContext.current
    var nombreUsuario by remember { mutableStateOf(storage.getUserName()) }
    var ciudadUsuario by remember { mutableStateOf(storage.getCity() ?: "Detectando...") }
    var direccionOficina by remember { mutableStateOf(storage.getOfficeAddress()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    
    var showSetupDialog by remember { mutableStateOf(nombreUsuario == null || direccionOficina == null) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showRouteConfirmDialog by remember { mutableStateOf(false) }
    var teletrabajadores by remember { mutableStateOf(listOf<String>()) }
    var tiempoEstimado by remember { mutableStateOf("Calculando...") }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            obtenerCiudadGps(context) { ciudad ->
                ciudadUsuario = ciudad
                storage.saveCity(ciudad)
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(1000)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            obtenerCiudadGps(context) { ciudad ->
                ciudadUsuario = ciudad
                storage.saveCity(ciudad)
            }
        }
    }

    LaunchedEffect(direccionOficina, ciudadUsuario) {
        while(true) {
            if (direccionOficina != null) {
                calcularTiempoTrayecto(context, direccionOficina!!) { tiempo ->
                    tiempoEstimado = tiempo
                }
            }
            delay(600000) 
        }
    }

    LaunchedEffect(selectedDate, refreshTrigger) {
        val storageKey = getStorageKey(selectedDate)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<String>()
                for (user in snapshot.children) {
                    user.key?.let { list.add(it) }
                }
                teletrabajadores = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child(storageKey).addValueEventListener(listener)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Team Parking", fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = (-0.5).sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = iOSCardWhite)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = iOSCardWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(ciudadUsuario, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☀️", fontSize = 24.sp); Spacer(modifier = Modifier.width(8.dp))
                            Text("22°C", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Card(modifier = Modifier.weight(1f).clickable { showRouteConfirmDialog = true }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = iOSCardWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Tráfico Oficina", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚗", fontSize = 24.sp); Spacer(modifier = Modifier.width(8.dp))
                            Text(tiempoEstimado, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if(tiempoEstimado.contains("min")) Color(0xFF4CAF50) else Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = iOSCardWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = iOSBlue) }
                        Text(text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${currentMonth.year}", fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = (-0.3).sp)
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = iOSBlue) }
                    }
                    
                    val daysInMonth = currentMonth.lengthOfMonth()
                    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7
                    val totalGridItems = firstDayOfMonth + daysInMonth

                    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(260.dp)) {
                        items(7) { index ->
                            val day = listOf("D", "L", "M", "X", "J", "V", "S")[index]
                            Text(text = day, modifier = Modifier.padding(4.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.LightGray)
                        }
                        items(totalGridItems) { index ->
                            @Suppress("UNUSED_VARIABLE")
                            val trigger = refreshTrigger

                            if (index >= firstDayOfMonth) {
                                val dayNum = index - firstDayOfMonth + 1
                                val date = currentMonth.atDay(dayNum)
                                val storageKey = getStorageKey(date)
                                val type = storage.getDayType(storageKey)
                                val hasNote = storage.getNote(storageKey) != null
                                
                                Box(modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .background(
                                        color = when(type) {
                                            "teletrabajo" -> Color(0xFF4CAF50).copy(alpha = 0.4f)
                                            "festivo" -> Color.Red.copy(alpha = 0.4f)
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if(date == selectedDate) 2.dp else 0.dp, 
                                        color = if(date == selectedDate) iOSBlue else Color.Transparent, 
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedDate = date }, 
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = dayNum.toString(), fontSize = 15.sp, fontWeight = if(date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal, color = if(date == LocalDate.now()) iOSBlue else Color.Black)
                                        if(hasNote) Box(modifier = Modifier.size(4.dp).background(Color.Red, CircleShape))
                                    }
                                }
                            } else Spacer(modifier = Modifier.size(1.dp))
                        }
                    }
                    
                    TextButton(
                        onClick = { showMenuDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Opciones del día", color = iOSBlue, fontSize = 17.sp, fontWeight = FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "TELETRABAJAN EL ${selectedDate.dayOfMonth}/${selectedDate.monthValue}", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), letterSpacing = 0.5.sp)

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = iOSCardWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                if (teletrabajadores.isEmpty()) Text("Nadie teletrabaja hoy", modifier = Modifier.padding(20.dp), color = Color.LightGray, fontSize = 15.sp)
                else LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                    items(teletrabajadores) { user ->
                        Column {
                            Row(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("🏠", modifier = Modifier.padding(end = 12.dp), fontSize = 18.sp); Text(user, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            }
                            if (user != teletrabajadores.last()) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = iOSBackground)
                        }
                    }
                }
            }
        }
    }

    if (showRouteConfirmDialog) {
        AlertDialog(onDismissRequest = { showRouteConfirmDialog = false }, title = { Text("¿Abrir navegación?", fontWeight = FontWeight.Bold) }, text = { Text("¿Quieres ver la ruta completa y el tráfico en tiempo real usando Google Maps?") }, confirmButton = { TextButton(onClick = { direccionOficina?.let { abrirGoogleMaps(context, it) }; showRouteConfirmDialog = false }) { Text("Usar Google Maps", fontWeight = FontWeight.Bold, color = iOSBlue) } }, dismissButton = { TextButton(onClick = { showRouteConfirmDialog = false }) { Text("Cerrar", color = iOSBlue) } })
    }

    if (showSetupDialog) {
        var tempName by remember { mutableStateOf(nombreUsuario ?: "") }
        var tempOffice by remember { mutableStateOf(direccionOficina ?: "") }
        AlertDialog(onDismissRequest = { }, title = { Text("Configuración", fontWeight = FontWeight.Bold) }, text = { Column { OutlinedTextField(value = tempName, onValueChange = { tempName = it }, label = { Text("Tu nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)); Spacer(modifier = Modifier.height(12.dp)); OutlinedTextField(value = tempOffice, onValueChange = { tempOffice = it }, label = { Text("Dirección Oficina") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) } }, confirmButton = { Button(onClick = { if (tempName.isNotBlank() && tempOffice.isNotBlank()) { storage.saveUserName(tempName); storage.saveOfficeAddress(tempOffice); nombreUsuario = tempName; direccionOficina = tempOffice; showSetupDialog = false } }, shape = RoundedCornerShape(10.dp)) { Text("Guardar") } })
    }

    if (showMenuDialog) {
        val storageKey = getStorageKey(selectedDate)
        val notaExistente = storage.getNote(storageKey)
        AlertDialog(
            onDismissRequest = { showMenuDialog = false }, 
            title = { Text("Día ${selectedDate.dayOfMonth}/${selectedDate.monthValue}", fontWeight = FontWeight.Bold) }, 
            text = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (notaExistente != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = iOSBackground), modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                            Text(text = "Recordatorio: $notaExistente", modifier = Modifier.padding(12.dp), fontSize = 14.sp, color = Color.DarkGray)
                        }
                    }
                    listOf("Teletrabajo (Verde)" to "teletrabajo", "Día Festivo (Rojo)" to "festivo").forEach { (label, estado) ->
                        TextButton(onClick = { storage.saveDayType(storageKey, estado); val user = nombreUsuario ?: "Usuario"; if (estado == "teletrabajo") database.child(storageKey).child(user).setValue(true) else database.child(storageKey).child(user).removeValue(); refreshTrigger++; showMenuDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(label, color = iOSBlue, fontSize = 17.sp) }
                    }
                    TextButton(onClick = { showMenuDialog = false; showNoteDialog = true }, modifier = Modifier.fillMaxWidth()) { Text(if (notaExistente == null) "Añadir Recordatorio" else "Editar Recordatorio", color = iOSBlue, fontSize = 17.sp) }
                    TextButton(onClick = { val user = nombreUsuario ?: "Usuario"; database.child(storageKey).child(user).removeValue(); storage.removeDayData(storageKey); reminderManager.cancelReminder(selectedDate.hashCode()); refreshTrigger++; showMenuDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Limpiar Día", color = Color.Red, fontSize = 17.sp) }
                } 
            }, 
            confirmButton = { TextButton(onClick = { showMenuDialog = false }) { Text("Cerrar", color = iOSBlue, fontWeight = FontWeight.Bold) } }
        )
    }

    if (showNoteDialog) {
        val storageKey = getStorageKey(selectedDate)
        var notaTexto by remember { mutableStateOf(storage.getNote(storageKey) ?: "") }
        AlertDialog(onDismissRequest = { showNoteDialog = false }, title = { Text("Recordatorio", fontWeight = FontWeight.Bold) }, text = { OutlinedTextField(value = notaTexto, onValueChange = { notaTexto = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), placeholder = { Text("Escribe aquí...") }) }, confirmButton = { TextButton(onClick = { if (notaTexto.isNotBlank()) { val ahora = Calendar.getInstance(); TimePickerDialog(context, { _, h, m -> val cal = Calendar.getInstance().apply { set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth, h, m, 0) }; storage.saveNote(storageKey, notaTexto); reminderManager.scheduleReminder(selectedDate.hashCode(), notaTexto, cal.timeInMillis); refreshTrigger++; showNoteDialog = false }, ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE), true).show() } }) { Text("Siguiente", fontWeight = FontWeight.Bold, color = iOSBlue) } }, dismissButton = { if (storage.getNote(storageKey) != null) { TextButton(onClick = { storage.removeNote(storageKey); reminderManager.cancelReminder(selectedDate.hashCode()); refreshTrigger++; showNoteDialog = false }) { Text("Eliminar", color = Color.Red) } } })
    }
}

fun getStorageKey(date: LocalDate): String {
    return "CalendarDay{${date.year}-${date.monthValue - 1}-${date.dayOfMonth}}"
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
                    onResult("${(6371.0 * c * 4).roundToInt()} min")
                } else onResult("-- min")
            } catch (e: Exception) { onResult("Calculando...") }
        } else onResult("No GPS")
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
