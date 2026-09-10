package com.rosseti.alert.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosseti.alert.data.NotifSettings
import com.rosseti.alert.data.OutageFilter
import com.rosseti.alert.data.OutageRepository
import com.rosseti.alert.data.Region
import com.rosseti.alert.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Экран настроек — выбор адреса из выпадающих списков + настройки уведомлений.
 */
class SettingsViewModel : ViewModel() {

    private var settingsStore: SettingsStore? = null
    private val repository = OutageRepository()

    // Регионы
    private val _regions = MutableStateFlow<List<Region>>(emptyList())
    val regions: StateFlow<List<Region>> = _regions.asStateFlow()

    // Районы для выбранного региона
    private val _raions = MutableStateFlow<List<String>>(emptyList())
    val raions: StateFlow<List<String>> = _raions.asStateFlow()

    // Нас. пункты
    private val _gorods = MutableStateFlow<List<String>>(emptyList())
    val gorods: StateFlow<List<String>> = _gorods.asStateFlow()

    // Улицы
    private val _streets = MutableStateFlow<List<String>>(emptyList())
    val streets: StateFlow<List<String>> = _streets.asStateFlow()

    // Выбранные значения
    private val _selectedRegion = MutableStateFlow("")
    val selectedRegion: StateFlow<String> = _selectedRegion.asStateFlow()

    private val _selectedRaion = MutableStateFlow("")
    val selectedRaion: StateFlow<String> = _selectedRaion.asStateFlow()

    private val _selectedGorod = MutableStateFlow("")
    val selectedGorod: StateFlow<String> = _selectedGorod.asStateFlow()

    private val _selectedStreet = MutableStateFlow("")
    val selectedStreet: StateFlow<String> = _selectedStreet.asStateFlow()

    // Настройки уведомлений
    private val _notifEnabled = MutableStateFlow(true)
    val notifEnabled: StateFlow<Boolean> = _notifEnabled.asStateFlow()

    private val _notifHour = MutableStateFlow("08:00")
    val notifHour: StateFlow<String> = _notifHour.asStateFlow()

    // Статус
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveResult = MutableStateFlow<String?>(null)
    val saveResult: StateFlow<String?> = _saveResult.asStateFlow()

    fun init(store: SettingsStore) {
        settingsStore = store
        viewModelScope.launch {
            // Загружаем сохранённые настройки
            val filter = store.filterFlow.first()
            if (filter.regionCode.isNotBlank()) {
                _selectedRegion.value = filter.regionCode
                _selectedRaion.value = filter.raion
                _selectedGorod.value = filter.gorod
                _selectedStreet.value = filter.street
            }

            // Настройки уведомлений
            val notif = store.notifFlow.first()
            _notifEnabled.value = notif.enabled
            _notifHour.value = notif.hour

            // Загружаем список регионов
            loadRegions()
        }
    }

    private suspend fun loadRegions() {
        _isLoading.value = true
        try {
            _regions.value = repository.getRegions()
            // Если регион уже выбран — загружаем районы
            val reg = _selectedRegion.value
            if (reg.isNotBlank()) {
                loadRaions(reg)
            }
        } catch (e: Exception) {
            _saveResult.value = "Ошибка загрузки: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun onRegionSelected(code: String) {
        _selectedRegion.value = code
        _selectedRaion.value = ""
        _selectedGorod.value = ""
        _selectedStreet.value = ""
        _raions.value = emptyList()
        _gorods.value = emptyList()
        _streets.value = emptyList()

        if (code.isNotBlank()) {
            viewModelScope.launch {
                loadRaions(code)
            }
        }
    }

    private suspend fun loadRaions(code: String) {
        try {
            _raions.value = repository.getRaionsForRegion(code)
        } catch (e: Exception) {
            _raions.value = emptyList()
        }
    }

    fun onRaionSelected(raion: String) {
        _selectedRaion.value = raion
        _selectedGorod.value = ""
        _selectedStreet.value = ""
        _gorods.value = emptyList()
        _streets.value = emptyList()

        if (raion.isNotBlank()) {
            viewModelScope.launch {
                loadGorods()
            }
        }
    }

    private suspend fun loadGorods() {
        try {
            _gorods.value = repository.getGorodsFor(
                _selectedRegion.value,
                _selectedRaion.value
            )
        } catch (e: Exception) {
            _gorods.value = emptyList()
        }
    }

    fun onGorodSelected(gorod: String) {
        _selectedGorod.value = gorod
        _selectedStreet.value = ""
        _streets.value = emptyList()

        if (gorod.isNotBlank()) {
            viewModelScope.launch {
                loadStreets()
            }
        }
    }

    private suspend fun loadStreets() {
        try {
            _streets.value = repository.getStreetsFor(
                _selectedRegion.value,
                _selectedRaion.value,
                _selectedGorod.value
            )
        } catch (e: Exception) {
            _streets.value = emptyList()
        }
    }

    fun onStreetSelected(street: String) {
        _selectedStreet.value = street
    }

    fun setNotifEnabled(enabled: Boolean) {
        _notifEnabled.value = enabled
    }

    fun setNotifHour(hour: String) {
        _notifHour.value = hour
    }

    fun save() {
        viewModelScope.launch {
            val store = settingsStore ?: return@launch
            store.saveFilter(
                OutageFilter(
                    regionCode = _selectedRegion.value,
                    raion = _selectedRaion.value,
                    gorod = _selectedGorod.value,
                    street = _selectedStreet.value
                )
            )
            store.saveNotifSettings(
                NotifSettings(
                    enabled = _notifEnabled.value,
                    hour = _notifHour.value
                )
            )
            _saveResult.value = "Сохранено ✓"
        }
    }

    fun clearSaveMessage() {
        _saveResult.value = null
    }
}