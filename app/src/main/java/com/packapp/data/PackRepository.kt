package com.packapp.data

import com.packapp.network.OpenMeteoForecastService
import com.packapp.network.OpenMeteoGeocodingService
import kotlinx.coroutines.flow.Flow
import com.packapp.network.OpenMeteoApiFactory
import kotlin.math.roundToInt

class PackRepository(
    private val dao: PackDao,
    private val geocodingService: OpenMeteoGeocodingService = OpenMeteoApiFactory.geocodingService,
    private val forecastService: OpenMeteoForecastService = OpenMeteoApiFactory.forecastService
) {
    private companion object {
        const val WEATHER_CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L
    }

    fun observeListSummaries(): Flow<List<PackingListSummary>> = dao.observeListSummaries()

    fun observeLists(): Flow<List<PackingListEntity>> = dao.observeLists()

    suspend fun getAllListsOnce(): List<PackingListEntity> = dao.getAllListsOnce()

    fun observeList(listId: Long): Flow<PackingListEntity?> = dao.observeList(listId)

    suspend fun getListById(listId: Long): PackingListEntity? = dao.getListById(listId)

    fun observeItems(listId: Long): Flow<List<PackingItemEntity>> = dao.observeItemsForList(listId)

    fun observeTotalCount(listId: Long): Flow<Int> = dao.observeTotalCount(listId)

    fun observePackedCount(listId: Long): Flow<Int> = dao.observePackedCount(listId)

    fun observeListCount(): Flow<Int> = dao.observeListCount()

    fun observeItemCount(): Flow<Int> = dao.observeItemCount()

    fun observePackedItemCountTotal(): Flow<Int> = dao.observePackedItemCountTotal()

    fun observeActivityEventCount(): Flow<Int> = dao.observeActivityEventCount()

    fun observeRecentActivityEvents(limit: Int): Flow<List<ActivityEventEntity>> =
        dao.observeRecentActivityEvents(limit)

    fun observeRecentPackedItems(limit: Int): Flow<List<PackedHistoryEntry>> =
        dao.observeRecentPackedItems(limit)

    suspend fun addList(name: String) {
        val cleanName = name.trim()
        val listId = dao.insertList(PackingListEntity(name = cleanName))
        logEvent(
            eventType = "LIST_CREATED",
            listId = listId,
            listName = cleanName
        )
    }

    suspend fun renameList(list: PackingListEntity, newName: String) {
        val cleanName = newName.trim()
        dao.updateList(list.copy(name = cleanName))
        logEvent(
            eventType = "LIST_RENAMED",
            listId = list.id,
            listName = cleanName
        )
    }

    suspend fun updateListAutomationSettings(
        listId: Long,
        weatherLocation: String,
        remindersEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ): PackingListEntity? {
        val list = dao.getListById(listId) ?: return null
        val updated = list.copy(
            weatherLocation = weatherLocation.trim(),
            remindersEnabled = remindersEnabled,
            reminderHour = reminderHour.coerceIn(0, 23),
            reminderMinute = reminderMinute.coerceIn(0, 59)
        )
        dao.updateList(updated)
        return updated
    }

    suspend fun deleteList(listId: Long) {
        val list = dao.getListById(listId)
        dao.deleteListById(listId)
        logEvent(
            eventType = "LIST_DELETED",
            listId = listId,
            listName = list?.name
        )
    }

    suspend fun addItem(listId: Long, title: String, weightGrams: Int = 0) {
        val cleanTitle = title.trim()
        dao.insertItem(
            PackingItemEntity(
                listId = listId,
                title = cleanTitle,
                weightGrams = weightGrams
            )
        )
        val list = dao.getListById(listId)
        logEvent(
            eventType = "ITEM_ADDED",
            listId = listId,
            listName = list?.name,
            itemTitle = cleanTitle
        )
    }

    suspend fun setPacked(itemId: Long) {
        dao.markPacked(itemId, System.currentTimeMillis())
        val item = dao.getItemById(itemId)
        val list = item?.let { dao.getListById(it.listId) }
        logEvent(
            eventType = "ITEM_PACKED",
            listId = item?.listId,
            itemId = itemId,
            listName = list?.name,
            itemTitle = item?.title
        )
    }

    suspend fun setUnpacked(itemId: Long) {
        dao.markUnpacked(itemId)
        val item = dao.getItemById(itemId)
        val list = item?.let { dao.getListById(it.listId) }
        logEvent(
            eventType = "ITEM_UNPACKED",
            listId = item?.listId,
            itemId = itemId,
            listName = list?.name,
            itemTitle = item?.title
        )
    }

    suspend fun resetList(listId: Long) {
        dao.resetListItems(listId)
        val list = dao.getListById(listId)
        logEvent(
            eventType = "LIST_RESET",
            listId = listId,
            listName = list?.name
        )
    }

    // ===== PACKING SESSION METHODS =====
    suspend fun createPackingSession(listId: Long, listName: String): Long {
        return dao.insertPackingSession(
            PackingSessionEntity(
                listId = listId,
                listName = listName,
                startTime = System.currentTimeMillis(),
                totalItems = 0  // will be updated later
            )
        )
    }

    suspend fun completePackingSession(sessionId: Long, itemsPacked: Int, totalItems: Int) {
        val session = dao.getSessionById(sessionId) ?: return
        dao.updatePackingSession(
            session.copy(
                endTime = System.currentTimeMillis(),
                itemsPacked = itemsPacked,
                totalItems = totalItems
            )
        )
    }

    fun observeSessionsForList(listId: Long): Flow<List<PackingSessionEntity>> =
        dao.observeSessionsForList(listId)

    suspend fun getFastestSessionDuration(listId: Long): Long? =
        dao.getFastestSessionDuration(listId)

    suspend fun getAverageSessionDuration(listId: Long): Long? =
        dao.getAverageSessionDuration(listId)

    // ===== TRIP METHODS =====
    suspend fun createTrip(title: String, location: String, startDate: Long): Long {
        return dao.insertTrip(
            TripEntity(
                title = title,
                location = location,
                startDate = startDate
            )
        )
    }

    suspend fun updateTrip(trip: TripEntity) {
        dao.updateTrip(trip)
    }

    suspend fun deleteTrip(tripId: Long) {
        dao.deleteTrip(tripId)
    }

    fun observeTrips(): Flow<List<TripEntity>> = dao.observeTrips()

    suspend fun getTripById(tripId: Long): TripEntity? = dao.getTripById(tripId)

    fun observeTripCount(): Flow<Int> = dao.observeTripCount()

    fun observeUniqueLocationsCount(): Flow<Int> = dao.observeUniqueLocationsCount()

    // ===== WEIGHT METHODS =====
    fun observePackedWeightForList(listId: Long): Flow<Int> =
        dao.observePackedWeightForList(listId)

    fun observeTotalWeightForList(listId: Long): Flow<Int> =
        dao.observeTotalWeightForList(listId)

    // ===== EFFICIENCY METHODS =====
    fun observeUsedItemsCount(listId: Long): Flow<Int> =
        dao.observeUsedItemsCount(listId)

    fun observeUsedPackedItemCountTotal(): Flow<Int> =
        dao.observeUsedPackedItemCountTotal()

    fun observeFastestSessionDurationMillis(): Flow<Long?> =
        dao.observeFastestSessionDurationMillis()

    fun observeAverageSessionDurationMillis(): Flow<Double?> =
        dao.observeAverageSessionDurationMillis()

    suspend fun getWeatherForLocation(location: String, forceRefresh: Boolean = false): WeatherSnapshot? {
        val normalized = normalizeLocation(location)
        if (normalized.isBlank()) return null

        val now = System.currentTimeMillis()
        val cached = dao.getWeatherCache(normalized)
        if (!forceRefresh && cached != null && cached.expiresAt > now) {
            return cached.toSnapshot(fromCache = true)
        }

        return runCatching {
            val geocode = geocodingService.searchLocation(name = normalized)
            val topResult = geocode.results?.firstOrNull() ?: return@runCatching cached?.toSnapshot(fromCache = true)

            val forecast = forecastService.forecast(
                latitude = topResult.latitude,
                longitude = topResult.longitude
            )

            val daily = forecast.daily
            val minTemp = daily?.temperatureMin?.firstOrNull()
            val maxTemp = daily?.temperatureMax?.firstOrNull()
            val precip = daily?.precipitationProbabilityMax?.firstOrNull()
            if (minTemp == null || maxTemp == null || precip == null) {
                return@runCatching cached?.toSnapshot(fromCache = true)
            }

            val locationLabel = buildString {
                append(topResult.name)
                if (!topResult.country.isNullOrBlank()) {
                    append(", ")
                    append(topResult.country)
                }
            }

            val entity = WeatherCacheEntity(
                locationKey = normalized,
                locationLabel = locationLabel,
                fetchedAt = now,
                expiresAt = now + WEATHER_CACHE_TTL_MILLIS,
                latitude = topResult.latitude,
                longitude = topResult.longitude,
                temperatureMinC = minTemp,
                temperatureMaxC = maxTemp,
                precipitationProbabilityMax = precip
            )
            dao.upsertWeatherCache(entity)
            entity.toSnapshot(fromCache = false)
        }.getOrNull() ?: cached?.toSnapshot(fromCache = true)
    }

    suspend fun getWeatherForTrip(tripId: Long, forceRefresh: Boolean = false): WeatherSnapshot? {
        val trip = dao.getTripById(tripId) ?: return null
        return getWeatherForLocation(trip.location, forceRefresh)
    }

    suspend fun getCompletionForList(listId: Long): Pair<Int, Int> {
        val packed = dao.getPackedCountNow(listId)
        val total = dao.getTotalCountNow(listId)
        return packed to total
    }

    fun suggestItemsFromWeather(snapshot: WeatherSnapshot): List<String> {
        val suggestions = linkedSetOf<String>()

        if (snapshot.temperatureMaxC >= 25.0) {
            suggestions += "Sonnencreme"
            suggestions += "Sonnenbrille"
            suggestions += "Shorts"
        }
        if (snapshot.temperatureMinC <= 5.0) {
            suggestions += "Mütze"
            suggestions += "Handschuhe"
            suggestions += "Schal"
        }
        if (snapshot.precipitationProbabilityMax >= 40) {
            suggestions += "Regenschirm"
            suggestions += "Regenjacke"
        }

        if (suggestions.isEmpty()) {
            suggestions += "Standard-Outfit"
        }

        return suggestions.toList()
    }

    private fun normalizeLocation(location: String): String =
        location.trim().lowercase()

    private fun WeatherCacheEntity.toSnapshot(fromCache: Boolean): WeatherSnapshot {
        return WeatherSnapshot(
            locationLabel = locationLabel,
            latitude = latitude,
            longitude = longitude,
            temperatureMinC = (temperatureMinC * 10).roundToInt() / 10.0,
            temperatureMaxC = (temperatureMaxC * 10).roundToInt() / 10.0,
            precipitationProbabilityMax = precipitationProbabilityMax,
            fetchedAt = fetchedAt,
            expiresAt = expiresAt,
            isFromCache = fromCache
        )
    }

    private suspend fun logEvent(
        eventType: String,
        listId: Long? = null,
        itemId: Long? = null,
        listName: String? = null,
        itemTitle: String? = null
    ) {
        dao.insertActivityEvent(
            ActivityEventEntity(
                eventType = eventType,
                listId = listId,
                itemId = itemId,
                listName = listName,
                itemTitle = itemTitle
            )
        )
    }
}
