package com.packapp.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.packapp.data.ActivityEventEntity
import com.packapp.data.PackDatabase
import com.packapp.data.PackRepository
import com.packapp.data.PackingItemEntity
import com.packapp.data.PackingListEntity
import com.packapp.data.PackingListSummary
import com.packapp.data.TripEntity
import com.packapp.data.WeatherSnapshot
import com.packapp.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class BottomTab {
    LISTS,
    PACKING,
    SETTINGS
}

enum class DesignMode {
    MINIMAL,
    SPORTLICH
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val tag: String) {
    SYSTEM("system"),
    DE("de"),
    EN("en"),
    ES("es"),
    FR("fr"),
    IT("it")
}

enum class HomeSortMode {
    NEWEST,
    OLDEST,
    NAME_AZ,
    NAME_ZA,
    PROGRESS_HIGH
}

enum class HomeFilterMode {
    ALL,
    IN_PROGRESS,
    COMPLETED,
    EMPTY
}

data class HomeUiState(
    val lists: List<PackingListSummary> = emptyList(),
    val newListName: String = "",
    val editingListId: Long? = null,
    val editingListName: String = "",
    val searchQuery: String = "",
    val sortMode: HomeSortMode = HomeSortMode.NEWEST,
    val filterMode: HomeFilterMode = HomeFilterMode.ALL
)

data class DetailUiState(
    val list: PackingListEntity? = null,
    val items: List<PackingItemEntity> = emptyList(),
    val newItemTitle: String = "",
    val newItemWeight: String = "",
    val packedCount: Int = 0,
    val totalCount: Int = 0,
    val packedWeight: Int = 0,
    val totalWeight: Int = 0,
    val currentSessionId: Long? = null,
    val currentSessionStartTime: Long? = null,
    val showSpeedrunToggle: Boolean = false,
    val weather: WeatherSnapshot? = null,
    val weatherSuggestions: List<String> = emptyList(),
    val weatherLoading: Boolean = false,
    val weatherError: String? = null
) {
    val isComplete: Boolean = totalCount > 0 && packedCount == totalCount
    
    val weightFormatted: String
        get() = if (totalWeight == 0) "0 g" else {
            if (totalWeight >= 1000) {
                String.format("%.1f kg", totalWeight / 1000f)
            } else {
                "$totalWeight g"
            }
        }
}

data class AccountUiState(
    val totalLists: Int = 0,
    val totalItems: Int = 0,
    val packedItems: Int = 0,
    val completionRatePercent: Int = 0,
    val totalActions: Int = 0,
    val actionsToday: Int = 0,
    val actionsLast7Days: Int = 0,
    val packedLast7Days: Int = 0,
    val recentEvents: List<ActivityEventEntity> = emptyList(),
    val trips: List<TripEntity> = emptyList(),
    val tripCount: Int = 0,
    val uniqueLocations: Int = 0,
    val fastestSessionDurationSeconds: Long? = null,
    val averageSessionDurationSeconds: Long? = null,
    val efficiencyScorePercent: Int = 0
)

data class SettingsUiState(
    val designMode: DesignMode = DesignMode.MINIMAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val luggageLimitKg: Int = 10,
    val language: AppLanguage = AppLanguage.SYSTEM
)

data class FirstOpenSetupDialogState(
    val listId: Long,
    val listName: String,
    val weatherLocation: String,
    val weatherForecastEpochDay: Long?,
    val remindersEnabled: Boolean,
    val reminderHour: String,
    val reminderMinute: String,
    val reminderDate: String,
    val reminderTime: String
)

private data class DetailBaseState(
    val list: PackingListEntity?,
    val items: List<PackingItemEntity>,
    val packedCount: Int,
    val totalCount: Int,
    val packedWeight: Int
)

private data class AccountBaseMetrics(
    val totalLists: Int,
    val totalItems: Int,
    val packedItems: Int,
    val totalActions: Int,
    val recentEvents: List<ActivityEventEntity>
)

private data class TripMetrics(
    val trips: List<TripEntity>,
    val tripCount: Int,
    val uniqueLocations: Int
)

private data class PerformanceMetrics(
    val fastestMillis: Long?,
    val averageMillis: Double?,
    val usedPackedItems: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val PREF_KEY_CONFIGURED_LIST_IDS = "configured_list_ids"
        const val PREF_KEY_LANGUAGE = "language"
    }

    private val app = application
    private val prefs = application.getSharedPreferences("packapp_prefs", Application.MODE_PRIVATE)
    private val repository = PackRepository(PackDatabase.getInstance(application).packDao())

    private val selectedListIdMutable = MutableStateFlow<Long?>(null)
    val selectedListId: StateFlow<Long?> = selectedListIdMutable.asStateFlow()

    private val selectedTabMutable = MutableStateFlow(BottomTab.LISTS)
    val selectedTab: StateFlow<BottomTab> = selectedTabMutable.asStateFlow()

    private val showOnboardingMutable = MutableStateFlow(!prefs.getBoolean("onboarding_seen", false))
    val showOnboarding: StateFlow<Boolean> = showOnboardingMutable.asStateFlow()

    private val showLanguagePickerMutable = MutableStateFlow(prefs.getString(PREF_KEY_LANGUAGE, null) == null)
    val showLanguagePicker: StateFlow<Boolean> = showLanguagePickerMutable.asStateFlow()

    private val firstOpenSetupDialogMutable = MutableStateFlow<FirstOpenSetupDialogState?>(null)
    val firstOpenSetupDialog: StateFlow<FirstOpenSetupDialogState?> = firstOpenSetupDialogMutable.asStateFlow()

    private val settingsMutable = MutableStateFlow(
        SettingsUiState(
            designMode = prefs.getString("design_mode", DesignMode.MINIMAL.name)
                ?.let { value -> DesignMode.entries.find { it.name == value } }
                ?: DesignMode.MINIMAL,
            themeMode = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
                ?.let { value -> ThemeMode.entries.find { it.name == value } }
                ?: ThemeMode.SYSTEM,
            luggageLimitKg = prefs.getInt("luggage_limit_kg", 10),
            language = prefs.getString(PREF_KEY_LANGUAGE, AppLanguage.SYSTEM.tag)
                ?.let { code -> AppLanguage.entries.find { it.tag == code } }
                ?: AppLanguage.SYSTEM
        )
    )
    val settingsUiState: StateFlow<SettingsUiState> = settingsMutable.asStateFlow()

    private val homeInputState = MutableStateFlow(HomeUiState())
    private val detailInputState = MutableStateFlow(DetailUiState())

    init {
        applyLanguage(settingsMutable.value.language)
        synchronizeAllReminderWorkers()
    }

    val homeUiState: StateFlow<HomeUiState> = combine(
        repository.observeListSummaries(),
        homeInputState
    ) { listSummaries, input ->
        val query = input.searchQuery.trim().lowercase()

        val filtered = listSummaries
            .asSequence()
            .filter { summary ->
                when (input.filterMode) {
                    HomeFilterMode.ALL -> true
                    HomeFilterMode.IN_PROGRESS -> summary.totalCount > 0 && summary.packedCount < summary.totalCount
                    HomeFilterMode.COMPLETED -> summary.totalCount > 0 && summary.packedCount == summary.totalCount
                    HomeFilterMode.EMPTY -> summary.totalCount == 0
                }
            }
            .filter { summary ->
                if (query.isBlank()) true
                else summary.list.name.lowercase().contains(query)
            }
            .toList()

        val sorted = when (input.sortMode) {
            HomeSortMode.NEWEST -> filtered.sortedByDescending { it.list.createdAt }
            HomeSortMode.OLDEST -> filtered.sortedBy { it.list.createdAt }
            HomeSortMode.NAME_AZ -> filtered.sortedBy { it.list.name.lowercase() }
            HomeSortMode.NAME_ZA -> filtered.sortedByDescending { it.list.name.lowercase() }
            HomeSortMode.PROGRESS_HIGH -> filtered.sortedByDescending {
                if (it.totalCount == 0) 0f else it.packedCount.toFloat() / it.totalCount.toFloat()
            }
        }

        input.copy(lists = sorted)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState()
    )

    private val detailBaseFlow = combine(
        selectedListIdMutable.flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.observeList(id)
        },
        selectedListIdMutable.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeItems(id)
        },
        selectedListIdMutable.flatMapLatest { id ->
            if (id == null) flowOf(0) else repository.observePackedCount(id)
        },
        selectedListIdMutable.flatMapLatest { id ->
            if (id == null) flowOf(0) else repository.observeTotalCount(id)
        },
        selectedListIdMutable.flatMapLatest { id ->
            if (id == null) flowOf(0) else repository.observePackedWeightForList(id)
        }
    ) { list, items, packedCount, totalCount, packedWeight ->
        DetailBaseState(
            list = list,
            items = items,
            packedCount = packedCount,
            totalCount = totalCount,
            packedWeight = packedWeight
        )
    }

    val detailUiState: StateFlow<DetailUiState> = combine(
        detailBaseFlow,
        selectedListIdMutable.flatMapLatest { id ->
            if (id == null) flowOf(0) else repository.observeTotalWeightForList(id)
        },
        detailInputState
    ) { base, totalWeight, input ->
        input.copy(
            list = base.list,
            items = base.items,
            packedCount = base.packedCount,
            totalCount = base.totalCount,
            packedWeight = base.packedWeight,
            totalWeight = totalWeight
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DetailUiState()
    )

    private val accountBaseFlow = combine(
        repository.observeListCount(),
        repository.observeItemCount(),
        repository.observePackedItemCountTotal(),
        repository.observeActivityEventCount(),
        repository.observeRecentActivityEvents(40)
    ) { totalLists, totalItems, packedItems, totalActions, recentEvents ->
        AccountBaseMetrics(
            totalLists = totalLists,
            totalItems = totalItems,
            packedItems = packedItems,
            totalActions = totalActions,
            recentEvents = recentEvents
        )
    }

    private val tripMetricsFlow = combine(
        repository.observeTrips(),
        repository.observeTripCount(),
        repository.observeUniqueLocationsCount()
    ) { trips, tripCount, uniqueLocations ->
        TripMetrics(
            trips = trips,
            tripCount = tripCount,
            uniqueLocations = uniqueLocations
        )
    }

    private val performanceMetricsFlow = combine(
        repository.observeFastestSessionDurationMillis(),
        repository.observeAverageSessionDurationMillis(),
        repository.observeUsedPackedItemCountTotal()
    ) { fastestMillis, averageMillis, usedPackedItems ->
        PerformanceMetrics(
            fastestMillis = fastestMillis,
            averageMillis = averageMillis,
            usedPackedItems = usedPackedItems
        )
    }

    val accountUiState: StateFlow<AccountUiState> = combine(
        accountBaseFlow,
        tripMetricsFlow,
        performanceMetricsFlow
    ) { base, tripMetrics, performance ->
        val now = System.currentTimeMillis()
        val oneDayAgo = now - TimeUnit.DAYS.toMillis(1)
        val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
        val actionsToday = base.recentEvents.count { it.timestamp >= oneDayAgo }
        val actionsLast7Days = base.recentEvents.count { it.timestamp >= sevenDaysAgo }
        val packedLast7Days = base.recentEvents.count {
            it.timestamp >= sevenDaysAgo && it.eventType == "ITEM_PACKED"
        }
        val completionRate = if (base.totalItems == 0) 0 else ((base.packedItems * 100f) / base.totalItems).toInt()
        val efficiency = if (base.packedItems == 0) 0 else ((performance.usedPackedItems * 100f) / base.packedItems).toInt()
        AccountUiState(
            totalLists = base.totalLists,
            totalItems = base.totalItems,
            packedItems = base.packedItems,
            completionRatePercent = completionRate,
            totalActions = base.totalActions,
            actionsToday = actionsToday,
            actionsLast7Days = actionsLast7Days,
            packedLast7Days = packedLast7Days,
            recentEvents = base.recentEvents,
            trips = tripMetrics.trips,
            tripCount = tripMetrics.tripCount,
            uniqueLocations = tripMetrics.uniqueLocations,
            fastestSessionDurationSeconds = performance.fastestMillis?.div(1000),
            averageSessionDurationSeconds = performance.averageMillis?.toLong()?.div(1000),
            efficiencyScorePercent = efficiency
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AccountUiState()
    )

    fun selectTab(tab: BottomTab) {
        selectedTabMutable.value = tab
    }

    fun setDesignMode(mode: DesignMode) {
        settingsMutable.value = settingsMutable.value.copy(designMode = mode)
        prefs.edit().putString("design_mode", mode.name).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsMutable.value = settingsMutable.value.copy(themeMode = mode)
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setLuggageLimitKg(value: Int) {
        val safeValue = value.coerceIn(1, 99)
        settingsMutable.value = settingsMutable.value.copy(luggageLimitKg = safeValue)
        prefs.edit().putInt("luggage_limit_kg", safeValue).apply()
    }

    fun setLanguage(language: AppLanguage) {
        settingsMutable.value = settingsMutable.value.copy(language = language)
        prefs.edit().putString(PREF_KEY_LANGUAGE, language.tag).apply()
        applyLanguage(language)
        showLanguagePickerMutable.value = false
    }

    fun setRemindersEnabled(enabled: Boolean) {
        val list = detailUiState.value.list ?: return
        saveCurrentListAutomationSettings(
            weatherLocation = list.weatherLocation,
            weatherForecastEpochDay = list.weatherForecastEpochDay,
            remindersEnabled = enabled,
            reminderHour = list.reminderHour,
            reminderMinute = list.reminderMinute,
            reminderTriggerAtMillis = list.reminderTriggerAtMillis
        )
    }

    fun setReminderTime(hour: Int, minute: Int) {
        val list = detailUiState.value.list ?: return
        saveCurrentListAutomationSettings(
            weatherLocation = list.weatherLocation,
            weatherForecastEpochDay = list.weatherForecastEpochDay,
            remindersEnabled = list.remindersEnabled,
            reminderHour = hour,
            reminderMinute = minute,
            reminderTriggerAtMillis = list.reminderTriggerAtMillis
        )
    }

    fun setWeatherLocation(value: String) {
        val list = detailUiState.value.list ?: return
        saveCurrentListAutomationSettings(
            weatherLocation = value,
            weatherForecastEpochDay = list.weatherForecastEpochDay,
            remindersEnabled = list.remindersEnabled,
            reminderHour = list.reminderHour,
            reminderMinute = list.reminderMinute,
            reminderTriggerAtMillis = list.reminderTriggerAtMillis
        )
    }

    fun refreshWeather(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val selectedListId = selectedListIdMutable.value
            val list = detailUiState.value.list ?: selectedListId?.let { repository.getListById(it) }
            val location = list?.weatherLocation?.trim().orEmpty()
            if (location.isBlank()) {
                detailInputState.update {
                    it.copy(
                        weather = null,
                        weatherSuggestions = emptyList(),
                        weatherError = "Bitte in dieser Packliste einen Ort setzen."
                    )
                }
                return@launch
            }

            detailInputState.update { it.copy(weatherLoading = true, weatherError = null) }
            val weather = repository.getWeatherForLocation(
                location = location,
                forceRefresh = forceRefresh,
                forecastEpochDay = list?.weatherForecastEpochDay
            )
            if (weather == null) {
                detailInputState.update {
                    it.copy(
                        weatherLoading = false,
                        weather = null,
                        weatherSuggestions = emptyList(),
                        weatherError = "Wetterdaten konnten nicht geladen werden."
                    )
                }
            } else {
                detailInputState.update {
                    it.copy(
                        weatherLoading = false,
                        weather = weather,
                        weatherSuggestions = repository.suggestItemsFromWeather(weather),
                        weatherError = null
                    )
                }
            }
        }
    }

    fun dismissOnboarding() {
        showOnboardingMutable.value = false
        prefs.edit().putBoolean("onboarding_seen", true).apply()
    }

    fun onNewListNameChange(value: String) {
        homeInputState.update { it.copy(newListName = value) }
    }

    fun onHomeSearchQueryChange(value: String) {
        homeInputState.update { it.copy(searchQuery = value) }
    }

    fun setHomeSortMode(mode: HomeSortMode) {
        homeInputState.update { it.copy(sortMode = mode) }
    }

    fun setHomeFilterMode(mode: HomeFilterMode) {
        homeInputState.update { it.copy(filterMode = mode) }
    }

    fun addList() {
        val name = homeInputState.value.newListName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            repository.addList(name)
            homeInputState.update { it.copy(newListName = "") }
        }
    }

    fun startRenameList(list: PackingListEntity) {
        homeInputState.update {
            it.copy(editingListId = list.id, editingListName = list.name)
        }
    }

    fun onEditingListNameChange(value: String) {
        homeInputState.update { it.copy(editingListName = value) }
    }

    fun cancelRenameList() {
        homeInputState.update { it.copy(editingListId = null, editingListName = "") }
    }

    fun commitRenameList(list: PackingListEntity) {
        val newName = homeInputState.value.editingListName.trim()
        if (newName.isEmpty()) return
        viewModelScope.launch {
            repository.renameList(list, newName)
            cancelRenameList()
        }
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch {
            repository.deleteList(listId)
            if (selectedListIdMutable.value == listId) {
                selectedListIdMutable.value = null
                detailInputState.update { DetailUiState() }
            }
        }
    }

    fun openList(listId: Long) {
        selectedListIdMutable.value = listId
        selectedTabMutable.value = BottomTab.PACKING
        refreshWeather(forceRefresh = false)
    }

    fun closeList() {
        selectedListIdMutable.value = null
        firstOpenSetupDialogMutable.value = null
        detailInputState.update {
            it.copy(
                newItemTitle = "",
                weather = null,
                weatherSuggestions = emptyList(),
                weatherLoading = false,
                weatherError = null
            )
        }
        selectedTabMutable.value = BottomTab.LISTS
    }

    fun onNewItemTitleChange(value: String) {
        detailInputState.update { it.copy(newItemTitle = value) }
    }

    fun onNewItemWeightChange(value: String) {
        detailInputState.update { it.copy(newItemWeight = value) }
    }

    fun addItem() {
        val listId = selectedListIdMutable.value ?: return
        val title = detailInputState.value.newItemTitle.trim()
        if (title.isEmpty()) return
        val weight = detailInputState.value.newItemWeight.trim().toIntOrNull() ?: 0
        viewModelScope.launch {
            repository.addItem(listId, title, weight)
            detailInputState.update { it.copy(newItemTitle = "", newItemWeight = "") }
        }
    }

    fun togglePacked(item: PackingItemEntity, packed: Boolean) {
        viewModelScope.launch {
            val detail = detailUiState.value
            if (packed) {
                if (detail.currentSessionId == null && detail.totalCount > 0 && detail.packedCount == 0) {
                    startSpeedrunSession(item.listId, detail.list?.name ?: "Liste")
                }
                repository.setPacked(item.id)

                val willBePackedCount = detail.packedCount + 1
                if (detail.totalCount > 0 && willBePackedCount >= detail.totalCount) {
                    endSpeedrunSession(itemsPacked = detail.totalCount, totalItems = detail.totalCount)
                }
            } else {
                repository.setUnpacked(item.id)
            }
        }
    }

    fun markPackedBySwipe(item: PackingItemEntity) {
        if (item.isPacked) return
        viewModelScope.launch {
            repository.setPacked(item.id)
        }
    }

    fun resetCurrentList() {
        val listId = selectedListIdMutable.value ?: return
        viewModelScope.launch {
            repository.resetList(listId)
        }
    }

    fun duplicateListLayout(list: PackingListEntity) {
        viewModelScope.launch {
            repository.duplicateListLayout(list)
        }
    }

    fun onFirstOpenSetupLocationChange(value: String) {
        firstOpenSetupDialogMutable.update { current ->
            current?.copy(weatherLocation = value)
        }
    }

    fun onFirstOpenSetupRemindersEnabledChange(value: Boolean) {
        firstOpenSetupDialogMutable.update { current ->
            current?.copy(remindersEnabled = value)
        }
    }

    fun onFirstOpenSetupHourChange(value: String) {
        if (!value.all { it.isDigit() } || value.length > 2) return
        firstOpenSetupDialogMutable.update { current ->
            current?.copy(reminderHour = value)
        }
    }

    fun onFirstOpenSetupMinuteChange(value: String) {
        if (!value.all { it.isDigit() } || value.length > 2) return
        firstOpenSetupDialogMutable.update { current ->
            current?.copy(reminderMinute = value)
        }
    }

    fun onFirstOpenSetupWeatherForecastEpochDayChange(value: Long?) {
        firstOpenSetupDialogMutable.update { current ->
            current?.copy(weatherForecastEpochDay = value)
        }
    }

    fun onFirstOpenSetupReminderDateChange(value: String) {
        firstOpenSetupDialogMutable.update { current ->
            current?.copy(reminderDate = value)
        }
    }

    fun onFirstOpenSetupReminderTimeChange(value: String) {
        firstOpenSetupDialogMutable.update { current ->
            current?.copy(reminderTime = autoInsertTimeColon(value))
        }
    }

    fun dismissFirstOpenSetupDialog() {
        val listId = firstOpenSetupDialogMutable.value?.listId ?: return
        markInitialSetupSeen(listId)
        firstOpenSetupDialogMutable.value = null
    }

    fun confirmFirstOpenSetupDialog() {
        val dialog = firstOpenSetupDialogMutable.value ?: return
        val hour = dialog.reminderHour.toIntOrNull() ?: 19
        val minute = dialog.reminderMinute.toIntOrNull() ?: 0
        val reminderTriggerAtMillis = parseReminderTriggerMillis(
            dateValue = dialog.reminderDate,
            timeValue = dialog.reminderTime
        )
        viewModelScope.launch {
            saveListAutomationSettings(
                listId = dialog.listId,
                weatherLocation = dialog.weatherLocation.trim(),
                weatherForecastEpochDay = dialog.weatherForecastEpochDay,
                remindersEnabled = dialog.remindersEnabled,
                reminderHour = hour,
                reminderMinute = minute,
                reminderTriggerAtMillis = reminderTriggerAtMillis,
                refreshIfCurrentList = true
            )
            markInitialSetupSeen(dialog.listId)
            firstOpenSetupDialogMutable.value = null
        }
    }

    // ===== TRIP MANAGEMENT =====
    fun createTrip(title: String, location: String, startDate: Long) {
        viewModelScope.launch {
            repository.createTrip(title, location, startDate)
        }
    }

    fun completeTrip(trip: TripEntity) {
        viewModelScope.launch {
            repository.updateTrip(trip.copy(completedDate = System.currentTimeMillis()))
        }
    }

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            repository.deleteTrip(trip.id)
        }
    }

    // ===== SPEEDRUN SESSION =====
    fun startSpeedrunSession(listId: Long, listName: String) {
        viewModelScope.launch {
            val sessionId = repository.createPackingSession(listId, listName)
            detailInputState.update {
                it.copy(
                    currentSessionId = sessionId,
                    currentSessionStartTime = System.currentTimeMillis()
                )
            }
        }
    }

    fun endSpeedrunSession(itemsPacked: Int, totalItems: Int) {
        viewModelScope.launch {
            val currentSessionId = detailInputState.value.currentSessionId ?: return@launch
            repository.completePackingSession(currentSessionId, itemsPacked, totalItems)
            detailInputState.update {
                it.copy(
                    currentSessionId = null,
                    currentSessionStartTime = null
                )
            }
        }
    }

    private fun rescheduleReminderWorker() {
        // no-op: reminders are scheduled per list
    }

    fun saveCurrentListAutomationSettings(
        weatherLocation: String,
        weatherForecastEpochDay: Long?,
        remindersEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        reminderTriggerAtMillis: Long?
    ) {
        val listId = selectedListIdMutable.value ?: return
        viewModelScope.launch {
            saveListAutomationSettings(
                listId = listId,
                weatherLocation = weatherLocation,
                weatherForecastEpochDay = weatherForecastEpochDay,
                remindersEnabled = remindersEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                reminderTriggerAtMillis = reminderTriggerAtMillis,
                refreshIfCurrentList = true
            )
        }
    }

    private suspend fun saveListAutomationSettings(
        listId: Long,
        weatherLocation: String,
        weatherForecastEpochDay: Long?,
        remindersEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        reminderTriggerAtMillis: Long?,
        refreshIfCurrentList: Boolean
    ) {
        val updated = repository.updateListAutomationSettings(
            listId = listId,
            weatherLocation = weatherLocation,
            weatherForecastEpochDay = weatherForecastEpochDay,
            remindersEnabled = remindersEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            reminderTriggerAtMillis = reminderTriggerAtMillis
        ) ?: return

        if (updated.remindersEnabled) {
            val triggerAtMillis = updated.reminderTriggerAtMillis
            if (triggerAtMillis != null && triggerAtMillis > System.currentTimeMillis()) {
                ReminderScheduler.scheduleOneTimeForList(
                    app,
                    listId = updated.id,
                    listName = updated.name,
                    triggerAtMillis = triggerAtMillis
                )
            } else {
                ReminderScheduler.cancelForList(app, updated.id)
            }
        } else {
            ReminderScheduler.cancelForList(app, updated.id)
        }

        if (refreshIfCurrentList && selectedListIdMutable.value == updated.id) {
            refreshWeather(forceRefresh = false)
        }
    }

    private fun isInitialSetupRequired(listId: Long): Boolean {
        val configuredIds = prefs.getStringSet(PREF_KEY_CONFIGURED_LIST_IDS, emptySet()).orEmpty()
        return !configuredIds.contains(listId.toString())
    }

    private fun markInitialSetupSeen(listId: Long) {
        val updated = prefs.getStringSet(PREF_KEY_CONFIGURED_LIST_IDS, emptySet())
            .orEmpty()
            .toMutableSet()
            .apply { add(listId.toString()) }
        prefs.edit().putStringSet(PREF_KEY_CONFIGURED_LIST_IDS, updated).apply()
    }

    private fun synchronizeAllReminderWorkers() {
        viewModelScope.launch {
            repository.getAllListsOnce().forEach { list ->
                if (list.remindersEnabled) {
                    val triggerAtMillis = list.reminderTriggerAtMillis
                    if (triggerAtMillis != null && triggerAtMillis > System.currentTimeMillis()) {
                        ReminderScheduler.scheduleOneTimeForList(
                            app,
                            listId = list.id,
                            listName = list.name,
                            triggerAtMillis = triggerAtMillis
                        )
                    } else {
                        ReminderScheduler.cancelForList(app, list.id)
                    }
                } else {
                    ReminderScheduler.cancelForList(app, list.id)
                }
            }
        }
    }

    private fun autoInsertTimeColon(input: String): String {
        val trimmed = input.trim()
        val digitsOnly = trimmed.filter { it.isDigit() }
        return when {
            trimmed.contains(':') -> trimmed
            digitsOnly.length <= 2 -> digitsOnly
            else -> digitsOnly.take(2) + ":" + digitsOnly.drop(2).take(2)
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        val locales = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private fun formatReminderDateInput(triggerAtMillis: Long?): String {
        if (triggerAtMillis == null) return ""
        return java.time.Instant.ofEpochMilli(triggerAtMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }

    private fun formatReminderTimeInput(triggerAtMillis: Long?, fallbackHour: Int, fallbackMinute: Int): String {
        return if (triggerAtMillis == null) {
            String.format(java.util.Locale.GERMANY, "%02d:%02d", fallbackHour, fallbackMinute)
        } else {
            val localTime = java.time.Instant.ofEpochMilli(triggerAtMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalTime()
            String.format(java.util.Locale.GERMANY, "%02d:%02d", localTime.hour, localTime.minute)
        }
    }

    private fun parseReminderTriggerMillis(dateValue: String, timeValue: String): Long? {
        val date = runCatching { java.time.LocalDate.parse(dateValue.trim()) }.getOrNull() ?: return null
        val match = Regex("^(\\d{1,2}):(\\d{1,2})$").find(timeValue.trim()) ?: return null
        val hour = match.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return null
        val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return null
        return date.atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
