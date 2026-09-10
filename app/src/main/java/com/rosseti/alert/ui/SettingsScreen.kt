package com.rosseti.alert.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val regions by viewModel.regions.collectAsState()
    val raions by viewModel.raions.collectAsState()
    val gorods by viewModel.gorods.collectAsState()
    val streets by viewModel.streets.collectAsState()

    val selectedRegion by viewModel.selectedRegion.collectAsState()
    val selectedRaion by viewModel.selectedRaion.collectAsState()
    val selectedGorod by viewModel.selectedGorod.collectAsState()
    val selectedStreet by viewModel.selectedStreet.collectAsState()

    val notifEnabled by viewModel.notifEnabled.collectAsState()
    val notifHour by viewModel.notifHour.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()

    LaunchedEffect(saveResult) {
        if (saveResult != null) {
            onBack()
        }
    }

    // Regio
    val regionNames = regions.associate { it.code to it.name }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки адреса",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Выберите адрес для отслеживания отключений",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Регион
        DropdownField(
            label = "Регион",
            value = if (selectedRegion.isBlank()) "" else regionNames[selectedRegion] ?: selectedRegion,
            options = regions.map { it.name },
            onOptionSelected = { name ->
                val code = regions.find { it.name == name }?.code ?: ""
                viewModel.onRegionSelected(code)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Район (доступен только если выбран регион)
        DropdownField(
            label = "Район",
            value = selectedRaion,
            options = raions,
            onOptionSelected = { viewModel.onRaionSelected(it) },
            enabled = selectedRegion.isNotBlank()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Нас. пункт
        DropdownField(
            label = "Населённый пункт",
            value = selectedGorod,
            options = gorods,
            onOptionSelected = { viewModel.onGorodSelected(it) },
            enabled = selectedRaion.isNotBlank()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Улица
        DropdownField(
            label = "Улица",
            value = selectedStreet,
            options = streets,
            onOptionSelected = { viewModel.onStreetSelected(it) },
            enabled = selectedGorod.isNotBlank()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Настройки уведомлений", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Вкл/выкл уведомления
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Уведомления", modifier = Modifier.weight(1f))
            Switch(
                checked = notifEnabled,
                onCheckedChange = { viewModel.setNotifEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Время проверки
        OutlinedTextField(
            value = notifHour,
            onValueChange = { viewModel.setNotifHour(it) },
            label = { Text("Время проверки (ЧЧ:ММ)") },
            placeholder = { Text("08:00") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = notifEnabled
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.save() },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedRegion.isNotBlank() && selectedStreet.isNotBlank()
        ) {
            Text("💾 Сохранить")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = enabled),
            enabled = enabled,
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}