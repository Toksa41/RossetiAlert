package com.rosseti.alert.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosseti.alert.data.Outage
import com.rosseti.alert.data.OutageFilter
import com.rosseti.alert.data.OutageRepository
import com.rosseti.alert.data.OutageResult
import com.rosseti.alert.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Главный экран — отображение статуса и ручная проверка.
 */
class MainViewModel(
    private val repository: OutageRepository = OutageRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    // Кеш фильтра из DataStore
    private var cachedRegionCode: String = ""
    private var cachedRaion: String = ""
    private var cachedGorod: String = ""
    private var cachedStreet: String = ""

    /** Читает настройки из DataStore (при старте и после возврата из настроек) */
    fun initSettings(store: SettingsStore) {
        viewModelScope.launch {
            val filter = store.filterFlow.first()
            cachedRegionCode = filter.regionCode
            cachedRaion = filter.raion
            cachedGorod = filter.gorod
            cachedStreet = filter.street
        }
    }

    /** Вызывается при возврате из экрана настроек — перечитывает фильтр */
    fun refreshSettings(store: SettingsStore) {
        initSettings(store)
    }

    /** Ручная проверка */
    fun checkNow() {
        val street = cachedStreet
        if (street.isBlank()) {
            _state.value = _state.value.copy(
                status = "Настройте адрес в разделе «Настройки»"
            )
            return
        }

        _state.value = _state.value.copy(isLoading = true, status = "Проверка...")

        viewModelScope.launch {
            val filter = OutageFilter(
                regionCode = cachedRegionCode,
                raion = cachedRaion,
                gorod = cachedGorod,
                street = cachedStreet
            )
            val result = repository.findNextOutage(filter)
            _state.value = _state.value.copy(isLoading = false)

            when (result) {
                is OutageResult.Success -> {
                    if (result.next != null) {
                        _state.value = _state.value.copy(
                            nextOutage = result.next,
                            status = "✓ Последняя проверка: сейчас",
                            lastCheckTime = System.currentTimeMillis()
                        )
                    } else {
                        _state.value = _state.value.copy(
                            nextOutage = null,
                            status = "✓ Отключений по вашему адресу не найдено",
                            lastCheckTime = System.currentTimeMillis()
                        )
                    }
                }
                is OutageResult.Error -> {
                    _state.value = _state.value.copy(
                        status = "✗ ${result.message}"
                    )
                }
            }
        }
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val nextOutage: Outage? = null,
    val status: String = "Нажмите «Проверить» для поиска отключений",
    val lastCheckTime: Long? = null
)