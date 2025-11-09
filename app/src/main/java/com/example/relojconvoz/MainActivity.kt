package com.example.relojconvoz


import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UIPrincipal()
        }
    }
}

//Variables globales
var mediaPlayers: MutableList<MediaPlayer> = mutableListOf()
var currentAudioIndex = 0

@Composable
fun UIPrincipal() {
    var horaTexto by remember { mutableStateOf("Presiona para ver la hora") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                val horaActual = obtenerHoraActual12Horas()
                horaTexto = "Hora: $horaActual"
                reproducirHoraEnVoz(context, horaActual)
            }
        ) {
            Text(text = horaTexto)
        }
    }
}

fun obtenerHoraActual12Horas(): String {
    //Reloj de 12 horas
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date())
}

fun reproducirHoraEnVoz(context: Context, hora: String) {
    val partesHora = descomponerHoraParaAudio(hora)
    println("Audios a reproducir: $partesHora")

    //Borrar la reproduccion anterior
    detenerReproduccion()

    try {
        mediaPlayers = partesHora.mapNotNull { parte ->
            val resourceId = obtenerResourceIdDeAudio(context, parte)
            if (resourceId != 0) {
                MediaPlayer.create(context, resourceId).apply {
                    setOnCompletionListener {
                        reproducirSiguienteAudio()
                    }
                }
            } else {
                null
            }
        }.toMutableList()

        currentAudioIndex = 0
        if (mediaPlayers.isNotEmpty()) {
            mediaPlayers[currentAudioIndex].start()
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun reproducirSiguienteAudio() {
    currentAudioIndex++
    if (currentAudioIndex < mediaPlayers.size) {
        mediaPlayers[currentAudioIndex].start()
    } else {
        detenerReproduccion()
    }
}

fun detenerReproduccion() {
    mediaPlayers.forEach { player ->
        if (player.isPlaying) {
            player.stop()
        }
        player.release()
    }
    mediaPlayers.clear()
    currentAudioIndex = 0
}

fun descomponerHoraParaAudio(horaCompleta: String): List<String> {
    //La hora viene en formato "h:mm a" (ej: "3:45 PM")
    val partes = horaCompleta.split(" ")
    val horaMinuto = partes[0].split(":")
    val amPm = partes[1]

    val horaInt = horaMinuto[0].toInt()
    val minutoInt = horaMinuto[1].toInt()

    return when {
        horaInt == 12 && minutoInt == 0 && amPm == "AM" -> listOf("son_las", "doce", "am")
        horaInt == 12 && minutoInt == 0 && amPm == "PM" -> listOf("son_las", "doce", "pm")
        amPm == "AM" -> descomponerHoraAM(horaInt, minutoInt)
        else -> descomponerHoraPM(horaInt, minutoInt)
    }
}

fun descomponerHoraAM(hora: Int, minuto: Int): List<String> {
    val partes = mutableListOf<String>()

    //Determina si es "son_la" (para la 1) o "son_las" (para las demás horas), en AM
    if (hora == 1) {
        partes.add("son_la")
        partes.add("una")
    } else {
        partes.add("son_las")
        partes.add(convertirNumeroATexto(hora))
    }

    //Agrega minutos si no es en punto
    if (minuto > 0) {
        partes.addAll(convertirMinutosATexto(minuto))
    }

    partes.add("am")
    return partes
}

fun descomponerHoraPM(hora: Int, minuto: Int): List<String> {
    val partes = mutableListOf<String>()

    //Determina si es "son_la" (para la 1) o "son_las" (para las demás), en PM
    if (hora == 1) {
        partes.add("son_la")
        partes.add("una")
    } else {
        partes.add("son_las")
        partes.add(convertirNumeroATexto(hora))
    }

    //Agrega minutos si no es en punto
    if (minuto > 0) {
        partes.addAll(convertirMinutosATexto(minuto))
    }

    partes.add("pm")
    return partes
}

fun convertirNumeroATexto(numero: Int): String {
    return when (numero) {
        1 -> "una"
        2 -> "dos"
        3 -> "tres"
        4 -> "cuatro"
        5 -> "cinco"
        6 -> "seis"
        7 -> "siete"
        8 -> "ocho"
        9 -> "nueve"
        10 -> "diez"
        11 -> "once"
        12 -> "doce"
        else -> numero.toString()
    }
}

fun convertirMinutosATexto(minutos: Int): List<String> {
    return when {
        minutos == 1 -> listOf("con_y", "un_minuto")
        minutos == 15 -> listOf("con_y", "quince", "minutos")
        minutos == 30 -> listOf("con_y", "treinta", "minutos")
        minutos == 45 -> listOf("con_y", "cuarenta", "y", "cinco", "minutos")
        minutos in 2..14 -> listOf("con_y", convertirNumeroATexto(minutos), "minutos")
        minutos in 16..29 -> {
            val nombreAudio = obtenerNombreAudioMinutoEspecial(minutos)
            listOf("con_y", nombreAudio, "minutos")
        }
        minutos in 31..44 -> {
            val nombreAudio = obtenerNombreAudioDecena(minutos)
            if (nombreAudio.contains("_y_")) {
                listOf("con_y", nombreAudio, "minutos")
            } else {
                listOf("con_y", nombreAudio, "minutos")
            }
        }
        minutos in 46..59 -> {
            val nombreAudio = obtenerNombreAudioDecena(minutos)
            if (nombreAudio.contains("_y_")) {
                listOf("con_y", nombreAudio, "minutos")
            } else {
                listOf("con_y", nombreAudio, "minutos")
            }
        }
        else -> listOf("con_y", minutos.toString())
    }
}

fun obtenerNombreAudioMinutoEspecial(minutos: Int): String {
    return when (minutos) {
        13 -> "trece"
        14 -> "catorce"
        16 -> "dieciseis"
        17 -> "diecisiete"
        18 -> "dieciocho"
        19 -> "diecinueve"
        20 -> "veinte"
        21 -> "veintiuno"
        22 -> "veintidós"
        23 -> "veintitrés"
        24 -> "veinticuatro"
        25 -> "veinticinco"
        26 -> "veintiséis"
        27 -> "veintisiete"
        28 -> "veintiocho"
        29 -> "veintinueve"
        else -> minutos.toString()
    }
}

fun obtenerNombreAudioDecena(minutos: Int): String {
    return when (minutos) {
        31 -> "treinta_y_uno"
        32 -> "treinta_y_dos"
        33 -> "treinta_y_tres"
        34 -> "treinta_y_cuatro"
        35 -> "treinta_y_cinco"
        36 -> "treinta_y_seis"
        37 -> "treinta_y_siete"
        38 -> "treinta_y_ocho"
        39 -> "treinta_y_nueve"
        40 -> "cuarenta"
        41 -> "cuarenta_y_uno"
        42 -> "cuarenta_y_dos"
        43 -> "cuarenta_y_tres"
        44 -> "cuarenta_y_cuatro"
        45 -> "cuarenta_y_cinco"
        46 -> "cuarenta_y_seis"
        47 -> "cuarenta_y_siete"
        48 -> "cuarenta_y_ocho"
        49 -> "cuarenta_y_nueve"
        50 -> "cincuenta"
        51 -> "cincuenta_y_uno"
        52 -> "cincuenta_y_dos"
        53 -> "cincuenta_y_tres"
        54 -> "cincuenta_y_cuatro"
        55 -> "cincuenta_y_cinco"
        56 -> "cincuenta_y_seis"
        57 -> "cincuenta_y_siete"
        58 -> "cincuenta_y_ocho"
        59 -> "cincuenta_y_nueve"
        else -> minutos.toString()
    }
}

fun obtenerResourceIdDeAudio(context: Context, parteAudio: String): Int {
    return try {
        val resourceName = parteAudio.lowercase(Locale.getDefault())
        val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        resourceId
    } catch (e: Exception) {
        println("No se encontró el audio: $parteAudio")
        0
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUIPrincipal() {
    UIPrincipal()
}