package com.apka.terminarzkliniki.ui.screens

import com.apka.terminarzkliniki.data.Visit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Pomocnicze formatowanie daty.
fun formatDateTime(millis: Long): String {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return df.format(Date(millis))
}

fun minutesLabel(mins: Int): String = when (mins) {
    10 -> "10 min"
    30 -> "30 min"
    60 -> "1 godz."
    else -> "$mins min"
}

// CSV helpers
private fun csvEscape(s: String): String {
    val v = s.replace("\r", " ").replace("\n", " ").trim()
    val needsQuotes = v.contains(',') || v.contains('"')

    return if (!needsQuotes) {
        v
    } else {
        // CSV: jeśli są przecinki/cudzysłowy, owijamy w "..."
        // a wewnętrzne " zamieniamy na "" (podwójny cudzysłów)
        "\"" + v.replace("\"", "\"\"") + "\""
    }
}

fun buildCsv(visits: List<Visit>): String {
    val header = listOf(
        "id", "dateTime", "petName", "species", "ownerName", "ownerPhone",
        "vetName", "notes", "remindEnabled", "remindMinutes", "status"
    ).joinToString(",")

    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    val rows = visits.joinToString("\n") { v ->
        listOf(
            v.id.toString(),
            csvEscape(df.format(Date(v.dateTimeMillis))),
            csvEscape(v.petName),
            csvEscape(v.species),
            csvEscape(v.ownerName),
            csvEscape(v.ownerPhone),
            csvEscape(v.vetName),
            csvEscape(v.notes),
            v.remindEnabled.toString(),
            v.remindMinutes.toString(),
            if (v.isArchived) "archiwum" else "aktualna"
        ).joinToString(",")
    }

    return header + "\n" + rows + "\n"
}
