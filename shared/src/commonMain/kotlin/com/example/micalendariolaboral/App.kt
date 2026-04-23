package com.example.micalendariolaboral

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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

val ColorBgApp = Color(0xFF0C0C0C)
val ColorBgCard = Color(0xFF1C1C1E)
val ColorAccentGreen = Color(0xFF34C759)
val ColorTextSecondary = Color(0xFF8E8E93)
val ColorDayTeletrabajo = Color(0xFF34C759) 
val ColorDayOficina = Color(0xFFFF3B30) 

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
    todosLosMiembros: List<String> = emptyList(),
    tiempoEstimado: String,
    ciudadActual: String,
    climaTemp: String = "22°C",
    climaIcon: String = "☀️",
    ultimoAviso: String? = null // Nuevo: Para mostrar notificaciones del equipo
) {
    val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var selectedDate by remember { mutableStateOf(hoy) }
    var displayedYear by remember { mutableStateOf(hoy.year) }
    var displayedMonth by remember { mutableStateOf(hoy.month) }
    
    var showMenuDialog by remember { mutableStateOf(false) }
    var showRouteConfirmDialog by remember { mutableStateOf(false) }
    var showAllTeamDialog by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    var showNameDialog by remember { mutableStateOf(storage.getUserName() == null || storage.getOfficeAddress() == null) }
    var userNameInput by remember { mutableStateOf(storage.getUserName() ?: "") }
    var officeInput by remember { mutableStateOf(storage.getOfficeAddress() ?: "") }

    MaterialTheme(colorScheme = darkColorScheme(background = ColorBgApp, surface = ColorBgCard)) {
        Scaffold(
            containerColor = ColorBgApp,
            bottomBar = { TeamBottomNavigation(onEquipoClick = { showAllTeamDialog = true }) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
                    
                    Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                        Spacer(modifier = Modifier.height(20.dp))

                        // Cabecera
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Team Parking", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                val frases = listOf(
                                    "Haz de hoy tu mejor día.",
                                    "El éxito es la suma de pequeños esfuerzos.",
                                    "Tu actitud determina tu dirección.",
                                    "Cree en ti y todo será posible.",
                                    "La constancia vence lo que la dicha no alcanza.",
                                    "Cada día es una nueva oportunidad.",
                                    "Trabaja duro en silencio, deja que el éxito haga el ruido.",
                                    "No cuentes los días, haz que los días cuenten.",
                                    "La motivación nos impulsa a empezar, el hábito nos permite seguir.",
                                    "Hoy es un buen día para tener un gran día."
                                )
                                val fraseDelDia = frases[hoy.dayOfMonth % frases.size]
                                Text(fraseDelDia, color = ColorTextSecondary, fontSize = 14.sp)
                            }
                            Surface(
                                modifier = Modifier.size(48.dp).clickable { showProfileMenu = true },
                                shape = CircleShape, color = ColorBgCard, border = BorderStroke(1.dp, Color(0x33FFFFFF))
                            ) { Icon(Icons.Default.Person, null, tint = ColorAccentGreen, modifier = Modifier.padding(10.dp)) }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tarjetas Info
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoCardStyled(modifier = Modifier.weight(1f), label = ciudadActual.uppercase(), value = climaTemp, subtitle = "Despejado", iconContent = { Text(climaIcon, fontSize = 28.sp) })
                            InfoCardStyled(modifier = Modifier.weight(1f), label = "TRÁFICO A OFICINA", value = tiempoEstimado, subtitle = "Tráfico fluido", iconContent = { Icon(Icons.Default.Place, null, tint = ColorAccentGreen, modifier = Modifier.size(28.dp)) }, onClick = { showRouteConfirmDialog = true })
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Calendario
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ColorBgCard)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (displayedMonth == Month.JANUARY) { displayedMonth = Month.DECEMBER; displayedYear -= 1 } else { displayedMonth = Month(displayedMonth.number - 1) } }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = ColorAccentGreen) }
                                    Text("${monthName(displayedMonth)} $displayedYear", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                    IconButton(onClick = { if (displayedMonth == Month.DECEMBER) { displayedMonth = Month.JANUARY; displayedYear += 1 } else { displayedMonth = Month(displayedMonth.number + 1) } }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = ColorAccentGreen) }
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom").forEach { Text(it, color = ColorTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
                                }
                                val daysInMonth = getDaysInMonth(displayedYear, displayedMonth)
                                val firstDayOfMonth = LocalDate(displayedYear, displayedMonth, 1)
                                val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.ordinal)
                                val rows = (daysInMonth + dayOfWeekOffset + 6) / 7
                                for (row in 0 until rows) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        for (col in 0 until 7) {
                                            val dayNum = row * 7 + col - dayOfWeekOffset + 1
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                                if (dayNum in 1..daysInMonth) {
                                                    val date = LocalDate(displayedYear, displayedMonth, dayNum)
                                                    CalendarDayCell(day = dayNum.toString(), isSelected = date == selectedDate, type = storage.getDayType(date.toString()), onClick = { selectedDate = date; onDateSelected(date.toString()) })
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp)); HorizontalDivider(color = Color(0x1AFFFFFF)); Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    LegendItem("Teletrabajo", ColorAccentGreen); LegendItem("Festivo", ColorDayOficina)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Lista del día
                        Text("TELETRABAJAN EL ${selectedDate.dayOfMonth} DE ${monthName(selectedDate.month).uppercase()}", color = ColorTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth().background(ColorBgCard, RoundedCornerShape(20.dp)).padding(16.dp)) {
                            if (teletrabajadores.isEmpty()) Text("Nadie teletrabaja hoy", color = ColorTextSecondary, fontSize = 14.sp)
                            else teletrabajadores.forEach { user -> TeamMemberRow(user) }
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                    }

                    // Botón Fijo
                    Button(onClick = { showMenuDialog = true }, modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp), shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorAccentGreen)) {
                        Icon(Icons.Default.Add, null, tint = Color.Black); Spacer(Modifier.width(8.dp)); Text("Gestionar mi día", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Banner de Avisos (Animado)
                AnimatedVisibility(
                    visible = ultimoAviso != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorAccentGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Color.Black)
                            Spacer(Modifier.width(12.dp))
                            Text(ultimoAviso ?: "", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // --- DIÁLOGOS ---
        if (showProfileMenu) {
            AlertDialog(
                onDismissRequest = { showProfileMenu = false },
                containerColor = ColorBgCard,
                title = { Text("Mi Perfil", color = Color.White) },
                text = {
                    Column {
                        Text("Usuario: ${storage.getUserName()}", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onUpdateStatus("unregister", ""); showProfileMenu = false; showNameDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorDayOficina)
                        ) { Text("Darse de baja del equipo", color = Color.White) }
                        Text("Esto notificará al grupo y borrará tu nombre.", color = ColorTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                },
                confirmButton = { TextButton(onClick = { showProfileMenu = false }) { Text("Cerrar", color = ColorAccentGreen) } }
            )
        }

        if (showAllTeamDialog) {
            AlertDialog(
                onDismissRequest = { showAllTeamDialog = false },
                containerColor = ColorBgCard,
                title = { Text("Miembros de Team Parking", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                        if (todosLosMiembros.isEmpty()) Text("Cargando equipo...", color = ColorTextSecondary)
                        else todosLosMiembros.forEach { miembro -> TeamMemberRow(miembro); Spacer(Modifier.height(8.dp)) }
                    }
                },
                confirmButton = { TextButton(onClick = { showAllTeamDialog = false }) { Text("Cerrar", color = ColorAccentGreen) } }
            )
        }

        if (showMenuDialog) {
            AlertDialog(
                onDismissRequest = { showMenuDialog = false },
                containerColor = ColorBgCard,
                title = { Text("¿Qué harás el ${selectedDate.dayOfMonth}?", color = Color.White) },
                text = {
                    Column {
                        Button(onClick = { onUpdateStatus(selectedDate.toString(), "teletrabajo"); showMenuDialog = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ColorAccentGreen)) { Text("Teletrabajar", color = Color.Black) }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onUpdateStatus(selectedDate.toString(), "festivo"); showMenuDialog = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ColorDayOficina)) { Text("Indicar día Festivo", color = Color.White) }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { onUpdateStatus(selectedDate.toString(), "limpiar"); showMenuDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Limpiar selección", color = Color.Gray) }
                    }
                },
                confirmButton = {}
            )
        }

        if (showNameDialog) {
            AlertDialog(
                onDismissRequest = { },
                containerColor = ColorBgCard,
                title = { Text("Configuración", color = Color.White) },
                text = {
                    Column {
                        TextField(value = userNameInput, onValueChange = { userNameInput = it }, label = { Text("Tu nombre") })
                        Spacer(Modifier.height(12.dp))
                        TextField(value = officeInput, onValueChange = { officeInput = it }, label = { Text("Dirección Oficina") })
                    }
                },
                confirmButton = { 
                    Button(onClick = { 
                        if(userNameInput.isNotBlank() && officeInput.isNotBlank()) { 
                            storage.saveUserName(userNameInput)
                            storage.saveOfficeAddress(officeInput)
                            onUpdateStatus("register", userNameInput)
                            showNameDialog = false 
                        } 
                    }, colors = ButtonDefaults.buttonColors(containerColor = ColorAccentGreen)) { Text("Guardar", color = Color.Black) } 
                }
            )
        }
    }
}

@Composable
fun TeamMemberRow(user: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp)) {
            Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = ColorBgApp, border = BorderStroke(1.5.dp, ColorAccentGreen)) {
                Box(contentAlignment = Alignment.Center) { Text(user.take(1).uppercase(), color = ColorAccentGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(user, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Miembro del equipo", color = ColorTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun InfoCardStyled(modifier: Modifier, label: String, value: String, subtitle: String, iconContent: @Composable () -> Unit, onClick: (() -> Unit)? = null) {
    Card(modifier = modifier.height(100.dp).then(if (onClick != null) Modifier.clickable { onClick() } else Modifier), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ColorBgCard), border = BorderStroke(1.dp, Color(0x1AFFFFFF))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp), color = Color(0x1AFFFFFF)) { Box(contentAlignment = Alignment.Center) { iconContent() } }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 9.sp, color = ColorTextSecondary, fontWeight = FontWeight.Bold)
                Text(value, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 10.sp, color = ColorTextSecondary)
            }
        }
    }
}

@Composable
fun CalendarDayCell(day: String, isSelected: Boolean, type: String?, onClick: () -> Unit) {
    val bgColor = when(type) { "teletrabajo" -> ColorDayTeletrabajo; "festivo" -> ColorDayOficina; else -> Color.Transparent }
    Surface(modifier = Modifier.size(36.dp).clickable { onClick() }, shape = CircleShape, color = bgColor, border = if (isSelected) BorderStroke(2.dp, ColorAccentGreen) else null) {
        Box(contentAlignment = Alignment.Center) { Text(day, color = if (isSelected || type != null) Color.White else ColorTextSecondary, fontSize = 14.sp) }
    }
}

@Composable
fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = color) {}
        Spacer(Modifier.width(6.dp))
        Text(text, color = ColorTextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun TeamBottomNavigation(onEquipoClick: () -> Unit) {
    NavigationBar(containerColor = ColorBgApp, contentColor = ColorAccentGreen) {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Calendario") })
        NavigationBarItem(selected = false, onClick = onEquipoClick, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Equipo") })
    }
}

fun getDaysInMonth(year: Int, month: Month): Int = when (month) {
    Month.FEBRUARY -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
    Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
    else -> 31
}

fun monthName(month: Month) = when(month) {
    Month.JANUARY -> "enero"; Month.FEBRUARY -> "febrero"; Month.MARCH -> "marzo"
    Month.APRIL -> "abril"; Month.MAY -> "mayo"; Month.JUNE -> "junio"
    Month.JULY -> "julio"; Month.AUGUST -> "agosto"; Month.SEPTEMBER -> "septiembre"
    Month.OCTOBER -> "octubre"; Month.NOVEMBER -> "noviembre"; Month.DECEMBER -> "diciembre"
    else -> month.name
}
