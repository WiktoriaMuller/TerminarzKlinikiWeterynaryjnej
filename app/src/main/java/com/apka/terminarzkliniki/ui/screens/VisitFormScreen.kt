@file:OptIn(ExperimentalMaterial3Api::class)

package com.apka.terminarzkliniki.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.apka.terminarzkliniki.data.Visit
import java.util.Calendar

@Composable
fun VisitFormScreen(
    title: String,
    initial: Visit?,
    onCancel: () -> Unit,
    onSave: (Visit) -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    val reminderOptions = remember { listOf(10, 30, 60) }
    val speciesOptions = remember { listOf("Kot", "Pies", "Królik", "Fretka", "Gryzoń", "Ptak", "Gad", "Inne…") }

    // rememberSaveable – formularz nie resetuje się przy obrocie ekranu.
    var petName by rememberSaveable { mutableStateOf("") }
    var species by rememberSaveable { mutableStateOf("") }
    var speciesOther by rememberSaveable { mutableStateOf("") }
    var ownerName by rememberSaveable { mutableStateOf("") }
    var ownerPhone by rememberSaveable { mutableStateOf("") }
    var vetName by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    // Domyślnie OFF dla nowej wizyty.
    var remindEnabled by rememberSaveable { mutableStateOf(false) }
    var remindMinutes by rememberSaveable { mutableIntStateOf(10) }

    var dateTimeMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    // Żeby nie nadpisywać pól przy każdej rekompozycji.
    var initialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initial?.id) {
        if (initialized) return@LaunchedEffect

        if (initial != null) {
            petName = initial.petName
            ownerName = initial.ownerName
            ownerPhone = initial.ownerPhone
            vetName = initial.vetName
            notes = initial.notes
            remindEnabled = initial.remindEnabled
            remindMinutes = initial.remindMinutes
            dateTimeMillis = initial.dateTimeMillis

            val known = speciesOptions.dropLast(1)
            if (initial.species in known) {
                species = initial.species
                speciesOther = ""
            } else {
                species = "Inne…"
                speciesOther = initial.species
            }
        } else {
            remindEnabled = false
            remindMinutes = 10
            dateTimeMillis = null
            species = ""
            speciesOther = ""
        }

        initialized = true
    }

    fun pickDateTime() {
        val now = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, y, m, d ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)

                TimePickerDialog(
                    context,
                    { _, hh, mm ->
                        cal.set(Calendar.HOUR_OF_DAY, hh)
                        cal.set(Calendar.MINUTE, mm)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        dateTimeMillis = cal.timeInMillis
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Dół ekranu z przyciskami zawsze widoczny.
    val bottomBar: @Composable () -> Unit = {
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Anuluj")
                }
                Button(
                    onClick = {
                        val dt = dateTimeMillis
                        val realSpecies = if (species == "Inne…") speciesOther.trim() else species.trim()

                        if (dt == null) {
                            errorText = "Wybierz datę i godzinę wizyty."
                            return@Button
                        }
                        if (dt < System.currentTimeMillis()) {
                            errorText = "Data wizyty nie może być w przeszłości."
                            return@Button
                        }
                        if (petName.isBlank() || realSpecies.isBlank() || ownerName.isBlank()) {
                            errorText = "Uzupełnij pola oznaczone gwiazdką (*)."
                            return@Button
                        }

                        val visit = Visit(
                            id = initial?.id ?: 0,
                            dateTimeMillis = dt,
                            petName = petName.trim(),
                            species = realSpecies,
                            ownerName = ownerName.trim(),
                            ownerPhone = ownerPhone.trim(),
                            vetName = vetName.trim(),
                            notes = notes.trim(),
                            remindEnabled = remindEnabled,
                            remindMinutes = remindMinutes,
                            isArchived = initial?.isArchived ?: false
                        )

                        onSave(visit)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Zapisz")
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedButton(onClick = { pickDateTime() }) {
                Text(
                    text = if (dateTimeMillis == null) "Wybierz datę i godzinę"
                    else "Data: ${formatDateTime(dateTimeMillis!!)}"
                )
            }

            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Imię zwierzaka*") },
                modifier = Modifier.fillMaxWidth()
            )

            // Gatunek jako dropdown
            SpeciesDropdown(
                label = "Gatunek*",
                options = speciesOptions,
                value = species.ifBlank { "Wybierz..." },
                onChange = { species = it },
                modifier = Modifier.fillMaxWidth()
            )

            if (species == "Inne…") {
                OutlinedTextField(
                    value = speciesOther,
                    onValueChange = { speciesOther = it },
                    label = { Text("Podaj gatunek*") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("Imię i nazwisko właściciela*") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ownerPhone,
                onValueChange = { ownerPhone = it },
                label = { Text("Telefon (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vetName,
                onValueChange = { vetName = it },
                label = { Text("Lekarz (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Opis wizyty (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Powiadomienie + wybór min
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Switch(
                    checked = remindEnabled,
                    onCheckedChange = { remindEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    ))
                Spacer(Modifier.width(10.dp))
                Text("Powiadomienie")
            }

            if (remindEnabled) {
                ReminderMinutesPicker(
                    value = remindMinutes,
                    options = reminderOptions,
                    onChange = { remindMinutes = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            errorText?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(80.dp)) // zapas, żeby nie chowało pod bottomBar
        }
    }
}
