// MainScreen.kt
package ru.sonic.zabbix.zbx

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    servers: List<ZabbixServer>,
    onSettingsClick: () -> Unit,
    preferencesManager: PreferencesManager
) {
    val coroutineScope = rememberCoroutineScope()

    // Загружаем сохраненное состояние - используем его как единственный источник правды
    val dashboardState by preferencesManager.getDashboardState().collectAsState(initial = DashboardState())

    // Локальное состояние только для выбранного сервера (он зависит от списка серверов)
    var selectedServer by remember {
        mutableStateOf<ZabbixServer?>(null)
    }

    var triggers by remember { mutableStateOf(emptyList<ZabbixTrigger>()) }

    // Эффект для обновления выбранного сервера при загрузке серверов
    LaunchedEffect(servers, dashboardState.selectedServerId) {
        if (servers.isNotEmpty() && dashboardState.selectedServerId != 0L) {
            val server = servers.find { it.id == dashboardState.selectedServerId }
            if (server != null && selectedServer?.id != server.id) {
                selectedServer = server
            }
        }
    }

    // Функция для сохранения состояния
    fun saveState(
        newSelectedServer: ZabbixServer? = selectedServer,
        newShowAcknowledged: Boolean? = null,
        newShowInMaintenance: Boolean? = null
    ) {
        coroutineScope.launch {
            preferencesManager.saveDashboardState(
                DashboardState(
                    selectedServerId = newSelectedServer?.id ?: 0,
                    showAcknowledged = newShowAcknowledged ?: dashboardState.showAcknowledged,
                    showInMaintenance = newShowInMaintenance ?: dashboardState.showInMaintenance
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Верхняя панель с кнопкой настроек - ОБНОВИМ ОТЛАДОЧНУЮ ИНФОРМАЦИЮ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Zabbix Dashboard",
                    style = MaterialTheme.typography.headlineSmall
                )
                // Отладочная информация - ПОКАЖЕМ СОСТОЯНИЕ ЧЕКБОКСОВ
                Text(
                    text = "Серверов: ${servers.size}, ID: ${dashboardState.selectedServerId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ack: ${dashboardState.showAcknowledged}, Maint: ${dashboardState.showInMaintenance}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(
                    onClick = {
                        // TODO: Refresh triggers
                        saveState()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSettingsClick) {
                    Text("Settings")
                }
            }
        }

        // Разделитель
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Панель фильтров
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Комбо-бокс выбора сервера
            ServerDropdown(
                servers = servers,
                selectedServer = selectedServer,
                onServerSelected = { server ->
                    selectedServer = server
                    // Сохраняем только сервер
                    saveState(newSelectedServer = server)
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Чек-боксы фильтров - ИСПОЛЬЗУЕМ dashboardState НАПРЯМУЮ
            Column {
                FilterCheckbox(
                    text = "Ack",
                    checked = dashboardState.showAcknowledged,
                    onCheckedChange = { newValue ->
                        // Сохраняем только измененный чекбокс
                        saveState(newShowAcknowledged = newValue)
                    }
                )
                FilterCheckbox(
                    text = "Maint",
                    checked = dashboardState.showInMaintenance,
                    onCheckedChange = { newValue ->
                        // Сохраняем только измененный чекбокс
                        saveState(newShowInMaintenance = newValue)
                    }
                )
            }
        }

        // Разделитель
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Список триггеров
        if (selectedServer == null) {
            // Сообщение при отсутствии выбранного сервера
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Выберите сервер для отображения триггеров",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            TriggerList(
                triggers = triggers,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDropdown(
    servers: List<ZabbixServer>,
    selectedServer: ZabbixServer?,
    onServerSelected: (ZabbixServer?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedServer?.name ?: "Выберите сервер",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text("Сервер") }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (servers.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Нет серверов") },
                    onClick = { expanded = false }
                )
            } else {
                servers.forEach { server ->
                    DropdownMenuItem(
                        text = { Text(server.name) },
                        onClick = {
                            onServerSelected(server)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun TriggerList(
    triggers: List<ZabbixTrigger>,
    modifier: Modifier = Modifier
) {
    if (triggers.isEmpty()) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Нет активных триггеров",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(triggers) { trigger ->
                TriggerItem(trigger = trigger)
                Divider()
            }
        }
    }
}

@Composable
fun TriggerItem(trigger: ZabbixTrigger) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = trigger.description,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Severity: ${trigger.severity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Host: ${trigger.host}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ZabbixAppTheme(darkTheme = false) {
        MainScreen(
            servers = listOf(
                ZabbixServer(1, "Production", "https://zabbix.com", "user", "pass", false, ""),
                ZabbixServer(2, "Testing", "https://test.zabbix.com", "user", "pass", false, "")
            ),
            onSettingsClick = {},
            preferencesManager = PreferencesManager(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}