package com.rosseti.alert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosseti.alert.data.Outage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Главный экран — статус, ближайшее отключение, кнопки действий.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenSite: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Россети Уведомления",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Статус
        Text(
            text = state.status,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = if (state.status.startsWith("✗"))
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurface
        )

        // Время последней проверки
        state.lastCheckTime?.let { time ->
            val formatted = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date(time))
            Text(
                text = "Последняя проверка: $formatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Прогресс
        if (state.isLoading) {
            CircularProgressIndicator()
        }

        // Карточка ближайшего отключения
        state.nextOutage?.let { outage ->
            OutageCard(outage)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Кнопки
        Button(
            onClick = { viewModel.checkNow() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            Text("🔄 Проверить сейчас")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f)
            ) {
                Text("⚙️ Настройки")
            }

            OutlinedButton(
                onClick = onOpenSite,
                modifier = Modifier.weight(1f)
            ) {
                Text("🌐 Открыть сайт")
            }
        }
    }
}

@Composable
private fun OutageCard(outage: Outage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚠️ Ближайшее отключение",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    "Дата: ",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${outage.dateStart.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row {
                Text(
                    "Время: ",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${outage.timeStart} — ${outage.timeFinish}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (outage.gorod.isNotBlank()) {
                Row {
                    Text(
                        "Нас. пункт: ",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(outage.gorod, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (outage.street.isNotBlank()) {
                Row {
                    Text(
                        "Улица: ",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(outage.street, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (outage.res.isNotBlank()) {
                Text(
                    "РЭС: ${outage.res}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}