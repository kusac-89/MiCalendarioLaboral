package com.example.micalendariolaboral

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*

// Colores exactos del diseño
val ColorTeletrabajo = Color(0xFF4CAF50) // Verde
val ColorFestivo = Color(0xFFFF0000)    // Rojo
val ColorSeleccionado = Color(0xFF00CED1) // Cian
val ColorGrisTexto = Color(0xFF8E8E93)
val ColorFondoApp = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    storage: StorageManager,
    onOpenMaps: (String) -> Unit,
    onSetReminder: (String, String) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onDateSelected: (String) -> Unit,
    teletrabajadores: List<String>,
    tiempoEstimado: String,
    ciudadActual: String
) {
    val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var selectedDate by remember { mutableStateOf(hoy) }
    var displayedYear by remember { mutableStateOf(hoy.year) }
    var displayedMonth by remember { mutableStateOf(hoy.month) }
    
    var showMenuDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var showRouteConfirmDialog by remember { mutableStateOf(false) }
    
    // Nuevo: Diálogo para pedir el nombre si no existe
    var showNameDialog by remember { mutableStateOf(storage.getUserName() == null) }
    var userNameInput by remember { mutableStateOf("") }

    MaterialTheme {
        Scaffold(
            containerColor = ColorFondoApp,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Team Parking", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                
                // 1. Tarjetas de Info Superiores
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard(
                        modifier = Modifier.weight(1f),
                        label = ciudadActual,
                        value = "22°C",
                        icon = "☀️"
                    )
                    InfoCard(
                        modifier = Modifier.weight(1f),
                        label = "Tráfico a Oficina",
                        value = tiempoEstimado,
                        icon = "🚗",
                        valueColor = ColorTeletrabajo,
                        onClick = { showRouteConfirmDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Calendario
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF6FF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Cabecera Mes con navegación corregida
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { 
                                if (displayedMonth == Month.JANUARY) {
                                    displayedMonth = Month.DECEMBER
                                    displayedYear -= 1
                                } else {
                                    displayedMonth = Month(displayedMonth.number - 1)
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev")
                            }
                            Text(
                                text = "${monthName(displayedMonth)} $displayedYear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF555555)
                            )
                            IconButton(onClick = { 
                                if (displayedMonth == Month.DECEMBER) {
                                    displayedMonth = Month.JANUARY
                                    displayedYear += 1
                                } else {
                                    displayedMonth = Month(displayedMonth.number + 1)
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next")
                            }
                        }

                        // Días de la semana
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom").forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    color = ColorGrisTexto,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Cuadrícula de días
                        val daysInMonth = getDaysInMonth(displayedYear, displayedMonth)
                        val firstDayOfMonth = LocalDate(displayedYear, displayedMonth, 1)
                        val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.ordinal) // 0=Mon, 6=Sun

                        val totalCells = (daysInMonth + dayOfWeekOffset)
                        val rows = (totalCells + 6) / 7

                        for (row in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - dayOfWeekOffset + 1
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1.1f), contentAlignment = Alignment.Center) {
                                        if (dayNum in 1..daysInMonth) {
                                            val date = LocalDate(displayedYear, displayedMonth, dayNum)
                                            val type = storage.getDayType(date.toString())
                                            
                                            DayCell(
                                                day = dayNum.toString(),
                                                isSelected = date == selectedDate,
                                                type = type,
                                                onClick = { 
                                                    selectedDate = date
                                                    onDateSelected(date.toString())
                                                    // Ya no abrimos el diálogo automáticamente aquí
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Lista de teletrabajadores con Botón de Gestión
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TELETRABAJAN EL ${selectedDate.dayOfMonth}/${selectedDate.monthNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColorGrisTexto,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Nuevo botón para abrir las opciones
                    TextButton(
                        onClick = { showMenuDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Gestionar día",
                            color = ColorTeletrabajo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFF))
                ) {
                    if (teletrabajadores.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.CenterStart) {
                            Text("Nadie teletrabaja hoy", color = Color.LightGray, fontSize = 16.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            items(teletrabajadores) { user ->
                                Text("🏠 $user", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Diálogos
        if (showMenuDialog) {
            AlertDialog(
                onDismissRequest = { showMenuDialog = false },
                title = { Text("¿Qué harás el ${selectedDate.dayOfMonth}?") },
                text = {
                    Column {
                        Button(
                            onClick = { onUpdateStatus(selectedDate.toString(), "teletrabajo"); showMenuDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorTeletrabajo)
                        ) { Text("Teletrabajar") }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onUpdateStatus(selectedDate.toString(), "festivo"); showMenuDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorFestivo)
                        ) { Text("Festivo / Vacaciones") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showMenuDialog = false; showNoteDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Añadir Nota") }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { onUpdateStatus(selectedDate.toString(), "limpiar"); showMenuDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Limpiar día", color = Color.Red) }
                    }
                },
                confirmButton = {}
            )
        }

        if (showNoteDialog) {
            AlertDialog(
                onDismissRequest = { showNoteDialog = false },
                title = { Text("Nota para ${selectedDate.toString()}") },
                text = { TextField(value = noteText, onValueChange = { noteText = it }) },
                confirmButton = { TextButton(onClick = { onSetReminder(selectedDate.toString(), noteText); showNoteDialog = false }) { Text("Guardar") } }
            )
        }

        if (showRouteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showRouteConfirmDialog = false },
                title = { Text("Navegación") },
                text = { Text("¿Abrir GPS hacia la oficina?") },
                confirmButton = { TextButton(onClick = { storage.getOfficeAddress()?.let { onOpenMaps(it) }; showRouteConfirmDialog = false }) { Text("Ir") } },
                dismissButton = { TextButton(onClick = { showRouteConfirmDialog = false }) { Text("Cancelar") } }
            )
        }

        // Diálogo de Nombre (Solo se muestra la primera vez)
        if (showNameDialog) {
            AlertDialog(
                onDismissRequest = { /* Forzamos a que ponga un nombre */ },
                title = { Text("¡Bienvenido!") },
                text = {
                    Column {
                        Text("Introduce tu nombre para que tus compañeros te identifiquen:")
                        Spacer(Modifier.height(8.dp))
                        TextField(value = userNameInput, onValueChange = { userNameInput = it }, placeholder = { Text("Tu nombre...") })
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            if (userNameInput.isNotBlank()) {
                                storage.saveUserName(userNameInput)
                                showNameDialog = false 
                            }
                        }
                    ) { Text("Empezar") }
                }
            )
        }
    }
}

@Composable
fun InfoCard(modifier: Modifier, label: String, value: String, icon: String, valueColor: Color = Color.Black, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = ColorGrisTexto, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
            }
        }
    }
}

@Composable
fun DayCell(day: String, isSelected: Boolean, type: String?, onClick: () -> Unit) {
    val bgColor = when {
        isSelected -> ColorSeleccionado
        type == "teletrabajo" -> ColorTeletrabajo
        type == "festivo" -> ColorFestivo
        else -> Color.Transparent
    }
    
    val textColor = if (bgColor != Color.Transparent) Color.White else Color.Black
    val shape = if (type == "teletrabajo") RoundedCornerShape(8.dp) else CircleShape

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(bgColor, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = day, color = textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

fun getDaysInMonth(year: Int, month: Month): Int {
    return when (month) {
        Month.FEBRUARY -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        else -> 31
    }
}

fun monthName(month: Month) = when(month) {
    Month.JANUARY -> "enero"; Month.FEBRUARY -> "febrero"; Month.MARCH -> "marzo"
    Month.APRIL -> "abril"; Month.MAY -> "mayo"; Month.JUNE -> "junio"
    Month.JULY -> "julio"; Month.AUGUST -> "agosto"; Month.SEPTEMBER -> "septiembre"
    Month.OCTOBER -> "octubre"; Month.NOVEMBER -> "noviembre"; Month.DECEMBER -> "diciembre"
    else -> month.name
}
