@file:OptIn(ExperimentalMaterial3Api::class)

package com.apka.terminarzkliniki.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apka.terminarzkliniki.data.DbProvider
import com.apka.terminarzkliniki.data.Visit
import com.apka.terminarzkliniki.notifications.cancelReminder
import com.apka.terminarzkliniki.notifications.hasPostNotificationsPermission
import com.apka.terminarzkliniki.notifications.scheduleReminder
import kotlinx.coroutines.launch

// Routing ekranów
sealed class Screen(val route: String) {
    data object List : Screen("list")
    data object Add : Screen("add")

    data class Details(val id: Long) : Screen("details/$id") {
        companion object { const val ROUTE_PATTERN = "details/{id}" }
    }

    data class Edit(val id: Long) : Screen("edit/$id") {
        companion object { const val ROUTE_PATTERN = "edit/{id}" }
    }
}

// Filtr listy
enum class ListFilter { CURRENT, ARCHIVE }

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val context = LocalContext.current

    // Prośba o zgodę na powiadomienia (Android 13+)
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    var asked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!asked && Build.VERSION.SDK_INT >= 33 && !hasPostNotificationsPermission(context)) {
            asked = true
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val db = remember { DbProvider.get(context) }
    val dao = remember { db.visitDao() }
    val visitsAll by dao.observeAll().collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf(ListFilter.CURRENT) }

    NavHost(navController = nav, startDestination = Screen.List.route) {

        composable(Screen.List.route) {
            VisitListScreen(
                nav = nav,
                visitsAll = visitsAll,
                filter = filter,
                onFilterChange = { filter = it }
            )
        }

        composable(Screen.Add.route) {
            VisitFormScreen(
                title = "Dodaj wizytę",
                initial = null,
                onCancel = { nav.popBackStack() },
                onSave = { draft ->
                    scope.launch {
                        val newId = dao.insert(draft.copy(id = 0))
                        if (!draft.isArchived && draft.remindEnabled) {
                            scheduleReminder(context, newId, draft.dateTimeMillis, draft.remindMinutes)
                        }
                        nav.popBackStack()
                    }
                }
            )
        }

        composable(Screen.Details.ROUTE_PATTERN) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: -1L
            val visit by dao.observeById(id).collectAsState(initial = null)

            VisitDetailsScreen(
                nav = nav,
                visit = visit,
                onDelete = { v ->
                    scope.launch {
                        dao.delete(v)
                        cancelReminder(context, v.id)
                        nav.popBackStack()
                    }
                },
                onToggleArchive = { v ->
                    scope.launch {
                        val updated = v.copy(isArchived = !v.isArchived)
                        dao.update(updated)

                        // Dla archiwum anulujemy scheduled work
                        cancelReminder(context, v.id)
                        if (!updated.isArchived && updated.remindEnabled) {
                            scheduleReminder(context, updated.id, updated.dateTimeMillis, updated.remindMinutes)
                        }
                    }
                }
            )
        }

        composable(Screen.Edit.ROUTE_PATTERN) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: -1L
            val visit by dao.observeById(id).collectAsState(initial = null)

            if (visit == null) {
                SimpleMessageScreen(
                    title = "Edycja",
                    message = "Nie znaleziono wizyty.",
                    onBack = { nav.popBackStack() }
                )
            } else {
                VisitFormScreen(
                    title = "Edytuj wizytę",
                    initial = visit,
                    onCancel = { nav.popBackStack() },
                    onSave = { edited ->
                        scope.launch {
                            dao.update(edited)
                            cancelReminder(context, edited.id)
                            if (!edited.isArchived && edited.remindEnabled) {
                                scheduleReminder(context, edited.id, edited.dateTimeMillis, edited.remindMinutes)
                            }
                            nav.popBackStack()
                        }
                    }
                )
            }
        }
    }
}
