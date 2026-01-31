@file:OptIn(ExperimentalMaterial3Api::class)

package com.apka.terminarzkliniki.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.apka.terminarzkliniki.data.Visit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VisitListScreen(
    nav: NavHostController,
    visitsAll: List<Visit>,
    filter: ListFilter,
    onFilterChange: (ListFilter) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var query by rememberSaveable { mutableStateOf("") }
    var pendingCsv by remember { mutableStateOf<String?>(null) }

    // Żeby lista sama przefiltrowała wizyty, gdy minie ich godzina.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000) // odśwież co 30s
            nowMillis = System.currentTimeMillis()
        }
    }

    // Eksport CSV
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val csv = pendingCsv ?: return@rememberLauncherForActivityResult

        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(csv.toByteArray(Charsets.UTF_8))
            }
            scope.launch { snackbar.showSnackbar("Zapisano CSV") }
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar("Nie udało się zapisać CSV: ${e.message}") }
        } finally {
            pendingCsv = null
        }
    }

    fun isEffectivelyArchived(v: Visit): Boolean =
        v.isArchived || v.dateTimeMillis < nowMillis

    val baseList =
        when (filter) {
            ListFilter.CURRENT -> visitsAll.filter { !isEffectivelyArchived(it) }
            ListFilter.ARCHIVE -> visitsAll.filter { isEffectivelyArchived(it) }
        }

    val filtered = run {
        val q = query.trim().lowercase()
        if (q.isBlank()) baseList
        else baseList.filter { v ->
            listOf(v.petName, v.ownerName, v.species, v.vetName, v.notes, v.ownerPhone)
                .any { it.lowercase().contains(q) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminarz wizyt") },
                actions = {
                    IconButton(
                        onClick = {
                            pendingCsv = buildCsv(visitsAll)
                            exportLauncher.launch("wizyty_terminarz.csv")
                        }
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "Eksport CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Screen.Add.route) }) {
                Text("+")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filter == ListFilter.CURRENT,
                    onClick = { onFilterChange(ListFilter.CURRENT) },
                    label = { Text("Aktualne") }
                )
                FilterChip(
                    selected = filter == ListFilter.ARCHIVE,
                    onClick = { onFilterChange(ListFilter.ARCHIVE) },
                    label = { Text("Archiwum") }
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Szukaj") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Brak wizyt do wyświetlenia.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = filtered, key = { it.id }) { v ->
                        val effectiveArchived = isEffectivelyArchived(v)
                        VisitRow(
                            visit = v,
                            effectiveArchived = effectiveArchived,
                            onClick = { nav.navigate("details/${v.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitRow(
    visit: Visit,
    effectiveArchived: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${formatDateTime(visit.dateTimeMillis)} • ${visit.petName}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(isArchived = effectiveArchived)
                ReminderPill(enabled = visit.remindEnabled, minutes = visit.remindMinutes)
            }

            Spacer(Modifier.height(8.dp))
            Text("Właściciel: ${visit.ownerName}")
            if (visit.vetName.isNotBlank()) Text("Lekarz: ${visit.vetName}")
        }
    }
}
