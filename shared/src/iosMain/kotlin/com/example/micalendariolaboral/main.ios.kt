package com.example.micalendariolaboral

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

fun MainViewController(): UIViewController = ComposeUIViewController {
    val settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    val storage = StorageManager(settings)
    
    App(
        storage = storage,
        onOpenMaps = { /* Se implementará en el Mac del amigo */ },
        onSetReminder = { _, _ -> },
        onDeleteReminder = { _ -> },
        onUpdateStatus = { _, _ -> },
        onDateSelected = { _ -> },
        teletrabajadores = emptyList(),
        todosLosMiembros = listOf("Usuario iOS (Demo)"),
        ultimoAviso = "¡Bienvenido a Team Parking!",
        tiempoEstimado = "-- min",
        ciudadActual = "Madrid",
        climaTemp = "22°C",
        climaIcon = "☀️"
    )
}
