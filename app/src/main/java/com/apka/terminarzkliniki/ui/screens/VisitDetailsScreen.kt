@file:OptIn(ExperimentalMaterial3Api::class)

package com.apka.terminarzkliniki.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.apka.terminarzkliniki.data.Visit

@Composable
fun VisitDetailsScreen(
    nav: NavHostController,
    visit: Visit?,
    onDelete: (Visit) -> Unit,
    onToggleArchive: (Visit) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Szczegóły wizyty") },
                actions = {
                    if (visit != null) {
                        IconButton(onClick = { nav.navigate("edit/${visit.id}") }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edytuj")
                        }
                        IconButton(onClick = { onToggleArchive(visit) }) {
                            Icon(
                                if (visit.isArchived) Icons.Filled.Restore else Icons.Filled.Archive,
                                contentDescription = if (visit.isArchived) "Przywróć" else "Archiwizuj"
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Usuń")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (visit == null) {
            SimpleMessageScreen(
                title = "Szczegóły",
                message = "Nie znaleziono wizyty.",
                onBack = { nav.popBackStack() },
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Data: ${formatDateTime(visit.dateTimeMillis)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Zwierzak: ${visit.petName} (${visit.species})")
                Text("Właściciel: ${visit.ownerName}")
                if (visit.ownerPhone.isNotBlank()) Text("Telefon: ${visit.ownerPhone}")
                if (visit.vetName.isNotBlank()) Text("Lekarz: ${visit.vetName}")
                if (visit.notes.isNotBlank()) Text("Opis: ${visit.notes}")

                Text(
                    text = if (visit.remindEnabled) "Powiadomienie: ${minutesLabel(visit.remindMinutes)} przed"
                    else "Powiadomienie: wyłączone"
                )

                Spacer(Modifier.height(10.dp))
                Button(onClick = { nav.popBackStack() }) { Text("Wróć") }
            }
        }
    }

    if (showDeleteConfirm && visit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Usunąć wizytę?") },
            text = { Text("Tej operacji nie da się cofnąć.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(visit)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}
