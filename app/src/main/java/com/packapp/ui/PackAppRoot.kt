package com.packapp.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packapp.data.ActivityEventEntity
import com.packapp.data.PackingItemEntity
import com.packapp.data.PackingListEntity
import com.packapp.data.WeatherSnapshot
import com.packapp.R
import com.packapp.ui.theme.PackPilotTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat

@Composable
fun PackPilotRoot(viewModel: AppViewModel = viewModel()) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val homeUi by viewModel.homeUiState.collectAsStateWithLifecycle()
    val detailUi by viewModel.detailUiState.collectAsStateWithLifecycle()
    val settingsUi by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val showOnboarding by viewModel.showOnboarding.collectAsStateWithLifecycle()

    var showAddListDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    PackPilotTheme(
        designMode = settingsUi.designMode,
        themeMode = settingsUi.themeMode
    ) {
    Scaffold(
        floatingActionButton = {
            when (selectedTab) {
                BottomTab.LISTS -> {
                    FloatingActionButton(onClick = { showAddListDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Liste hinzufügen")
                    }
                }

                BottomTab.PACKING -> {
                    if (detailUi.list != null) {
                        FloatingActionButton(onClick = { showAddItemDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Item hinzufügen")
                        }
                    }
                }

                BottomTab.SETTINGS -> Unit
            }
        },
        bottomBar = {
            Surface(shadowElevation = 10.dp, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                AppBottomNavItem(
                    selected = selectedTab == BottomTab.LISTS,
                    label = "Listen",
                    icon = { tint -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = tint) },
                    onClick = { viewModel.selectTab(BottomTab.LISTS) }
                )
                AppBottomNavItem(
                    selected = selectedTab == BottomTab.PACKING,
                    label = "Packen",
                    icon = { tint -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tint) },
                    onClick = { viewModel.selectTab(BottomTab.PACKING) }
                )
                AppBottomNavItem(
                    selected = selectedTab == BottomTab.SETTINGS,
                    label = "Settings",
                    icon = { tint -> Icon(Icons.Default.Settings, contentDescription = null, tint = tint) },
                    onClick = { viewModel.selectTab(BottomTab.SETTINGS) }
                )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            BottomTab.LISTS -> {
                HomeScreen(
                    uiState = homeUi,
                    contentPadding = innerPadding,
                    onSearchQueryChange = viewModel::onHomeSearchQueryChange,
                    onSortModeChange = viewModel::setHomeSortMode,
                    onFilterModeChange = viewModel::setHomeFilterMode,
                    onOpenList = viewModel::openList,
                    onDeleteList = viewModel::deleteList,
                    onStartRename = viewModel::startRenameList,
                    onDuplicateList = viewModel::duplicateListLayout,
                    onRenameInputChange = viewModel::onEditingListNameChange,
                    onCommitRename = viewModel::commitRenameList,
                    onCancelRename = viewModel::cancelRenameList
                )
            }

            BottomTab.PACKING -> {
                DetailScreen(
                    uiState = detailUi,
                    contentPadding = innerPadding,
                    onBack = viewModel::closeList,
                    onTogglePacked = viewModel::togglePacked,
                    onSwipePacked = viewModel::markPackedBySwipe,
                    onReset = viewModel::resetCurrentList,
                    onRefreshWeather = { force -> viewModel.refreshWeather(forceRefresh = force) },
                    onSaveAutomationSettings = viewModel::saveCurrentListAutomationSettings
                )
            }

            BottomTab.SETTINGS -> SettingsScreen(
                contentPadding = innerPadding,
                uiState = settingsUi,
                onDesignModeSelected = viewModel::setDesignMode,
                onThemeModeSelected = viewModel::setThemeMode,
                onLuggageLimitKgChanged = viewModel::setLuggageLimitKg
            )
        }

        if (showAddListDialog) {
            AddNameDialog(
                title = "Neue Packliste",
                label = "Name der Liste",
                value = homeUi.newListName,
                confirmText = "Erstellen",
                onValueChange = viewModel::onNewListNameChange,
                onDismiss = {
                    showAddListDialog = false
                    viewModel.onNewListNameChange("")
                },
                onConfirm = {
                    viewModel.addList()
                    if (homeUi.newListName.trim().isNotEmpty()) {
                        showAddListDialog = false
                    }
                }
            )
        }

        if (showAddItemDialog && detailUi.list != null) {
            AddItemDialog(
                title = "Neues Item",
                label = "Was musst du einpacken?",
                value = detailUi.newItemTitle,
                weightValue = detailUi.newItemWeight,
                confirmText = "Hinzufügen",
                onValueChange = viewModel::onNewItemTitleChange,
                onWeightChange = viewModel::onNewItemWeightChange,
                onDismiss = {
                    showAddItemDialog = false
                    viewModel.onNewItemTitleChange("")
                    viewModel.onNewItemWeightChange("")
                },
                onConfirm = {
                    viewModel.addItem()
                    if (detailUi.newItemTitle.trim().isNotEmpty()) {
                        showAddItemDialog = false
                    }
                }
            )
        }

        if (showOnboarding) {
            OnboardingOverlay(onDismiss = viewModel::dismissOnboarding)
        }

    }
    }
}

@Composable
private fun AppBottomNavItem(
    selected: Boolean,
    label: String,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon(contentColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    contentPadding: PaddingValues,
    onSearchQueryChange: (String) -> Unit,
    onSortModeChange: (HomeSortMode) -> Unit,
    onFilterModeChange: (HomeFilterMode) -> Unit,
    onOpenList: (Long) -> Unit,
    onDeleteList: (Long) -> Unit,
    onStartRename: (PackingListEntity) -> Unit,
    onDuplicateList: (PackingListEntity) -> Unit,
    onRenameInputChange: (String) -> Unit,
    onCommitRename: (PackingListEntity) -> Unit,
    onCancelRename: () -> Unit
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Meine Packlisten", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${uiState.lists.size} Listen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(Icons.Default.Search, contentDescription = "Suche")
                }
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter einstellen")
                }
            }
        )

        if (showSearch) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onSearchQueryChange(if (it.isEmpty()) it else it[0].uppercase() + it.drop(1)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Listen durchsuchen") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        }

        if (showFilters) {
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Filter", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.filterMode == HomeFilterMode.ALL,
                            onClick = { onFilterModeChange(HomeFilterMode.ALL) },
                            label = { Text("Alle") }
                        )
                        FilterChip(
                            selected = uiState.filterMode == HomeFilterMode.IN_PROGRESS,
                            onClick = { onFilterModeChange(HomeFilterMode.IN_PROGRESS) },
                            label = { Text("Aktiv") }
                        )
                        FilterChip(
                            selected = uiState.filterMode == HomeFilterMode.COMPLETED,
                            onClick = { onFilterModeChange(HomeFilterMode.COMPLETED) },
                            label = { Text("Fertig") }
                        )
                        FilterChip(
                            selected = uiState.filterMode == HomeFilterMode.EMPTY,
                            onClick = { onFilterModeChange(HomeFilterMode.EMPTY) },
                            label = { Text("Leer") }
                        )
                    }

                    Text("Sortierung", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.sortMode == HomeSortMode.NEWEST,
                            onClick = { onSortModeChange(HomeSortMode.NEWEST) },
                            label = { Text("Neu") }
                        )
                        FilterChip(
                            selected = uiState.sortMode == HomeSortMode.OLDEST,
                            onClick = { onSortModeChange(HomeSortMode.OLDEST) },
                            label = { Text("Alt") }
                        )
                        FilterChip(
                            selected = uiState.sortMode == HomeSortMode.NAME_AZ,
                            onClick = { onSortModeChange(HomeSortMode.NAME_AZ) },
                            label = { Text("A-Z") }
                        )
                        FilterChip(
                            selected = uiState.sortMode == HomeSortMode.PROGRESS_HIGH,
                            onClick = { onSortModeChange(HomeSortMode.PROGRESS_HIGH) },
                            label = { Text("Fortschritt") }
                        )
                    }
                }
            }
        }

        if (uiState.lists.isEmpty()) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                EmptyState(
                    if (uiState.searchQuery.isNotBlank()) {
                        "Keine Liste passt zu deiner Suche/Filterung."
                    } else {
                        "Noch keine Liste vorhanden. Tippe auf +, um zu starten."
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 92.dp)
            ) {
                items(uiState.lists, key = { it.list.id }) { summary ->
                    ListCard(
                        list = summary.list,
                        totalCount = summary.totalCount,
                        packedCount = summary.packedCount,
                        editing = uiState.editingListId == summary.list.id,
                        editingValue = uiState.editingListName,
                        onOpen = { onOpenList(summary.list.id) },
                        onDelete = { onDeleteList(summary.list.id) },
                        onStartRename = { onStartRename(summary.list) },
                        onDuplicateLayout = { onDuplicateList(summary.list) },
                        onRenameInputChange = onRenameInputChange,
                        onCommitRename = { onCommitRename(summary.list) },
                        onCancelRename = onCancelRename
                    )
                }
            }
        }
    }
}

@Composable
private fun ListCard(
    list: PackingListEntity,
    totalCount: Int,
    packedCount: Int,
    editing: Boolean,
    editingValue: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onStartRename: () -> Unit,
    onDuplicateLayout: () -> Unit,
    onRenameInputChange: (String) -> Unit,
    onCommitRename: () -> Unit,
    onCancelRename: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (editing) {
                OutlinedTextField(
                    value = editingValue,
                    onValueChange = { onRenameInputChange(if (it.isEmpty()) it else it[0].uppercase() + it.drop(1)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Listenname") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCommitRename) { Text("Speichern") }
                    OutlinedButton(onClick = onCancelRename) { Text("Abbrechen") }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (totalCount == 0) {
                                "Noch keine Items"
                            } else {
                                "$packedCount von $totalCount gepackt"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onStartRename) {
                        Icon(Icons.Default.Edit, contentDescription = "Umbenennen")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mehr")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Umbenennen") },
                                onClick = {
                                    menuExpanded = false
                                    onStartRename()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Layout kopieren") },
                                onClick = {
                                    menuExpanded = false
                                    onDuplicateLayout()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Löschen") },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                        }
                    }
                }
                Button(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Text("Öffnen")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    uiState: DetailUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onTogglePacked: (PackingItemEntity, Boolean) -> Unit,
    onSwipePacked: (PackingItemEntity) -> Unit,
    onReset: () -> Unit,
    onRefreshWeather: (Boolean) -> Unit,
    onSaveAutomationSettings: (String, Long?, Boolean, Int, Int, Long?) -> Unit
) {
    var showSuccess by remember(uiState.list?.id) { mutableStateOf(false) }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) showSuccess = true
    }

    if (uiState.list == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                EmptyState("Öffne zuerst eine Liste im Tab Listen.")
            }
        }
        return
    }

    var weatherLocation by remember(uiState.list.id) { mutableStateOf(uiState.list.weatherLocation) }
    var weatherForecastDate by remember(uiState.list.id) {
        mutableStateOf(formatEpochDayForInput(uiState.list.weatherForecastEpochDay))
    }
    var remindersEnabled by remember(uiState.list.id) { mutableStateOf(uiState.list.remindersEnabled) }
    var reminderHour by remember(uiState.list.id) { mutableStateOf(uiState.list.reminderHour.toString()) }
    var reminderMinute by remember(uiState.list.id) { mutableStateOf(uiState.list.reminderMinute.toString()) }
    var reminderDate by remember(uiState.list.id) {
        mutableStateOf(formatReminderDateForInput(uiState.list.reminderTriggerAtMillis))
    }
    var reminderTime by remember(uiState.list.id) {
        mutableStateOf(formatReminderTimeForInput(uiState.list.reminderTriggerAtMillis, uiState.list.reminderHour, uiState.list.reminderMinute))
    }
    var automationExpanded by rememberSaveable(uiState.list.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            remindersEnabled = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp)
    ) {
        item {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.list.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        Text("Reset")
                    }
                }
            )
        }

        item {
            ProgressStrip(
                packedCount = uiState.packedCount,
                totalCount = uiState.totalCount,
                packedWeight = uiState.packedWeight,
                totalWeight = uiState.totalWeight
            )
        }

        item {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { automationExpanded = !automationExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.automation_title), style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = if (automationExpanded) {
                                    stringResource(R.string.automation_hint_expanded)
                                } else {
                                    stringResource(R.string.automation_hint_collapsed)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (automationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    if (automationExpanded) {
                        OutlinedTextField(
                            value = weatherLocation,
                            onValueChange = { weatherLocation = it.replaceFirstChar { c -> c.uppercase() } },
                            label = { Text("Wetter-Ort (z. B. Wien)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth()
                        )

                        PickerField(
                            value = weatherForecastDate,
                            label = stringResource(R.string.weather_day_label),
                            icon = Icons.Default.DateRange,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showDatePickerDialog(
                                    context = context,
                                    initialDate = parseLocalDateOrNull(weatherForecastDate)
                                ) { selectedDate ->
                                    weatherForecastDate = selectedDate.format(isoDateFormatter)
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.reminders_enabled_label))
                            Switch(
                                checked = remindersEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !hasNotificationPermission(context)) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        remindersEnabled = hasNotificationPermission(context)
                                    } else {
                                        remindersEnabled = enabled
                                    }
                                }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PickerField(
                                value = reminderDate,
                                label = stringResource(R.string.push_day_label),
                                icon = Icons.Default.DateRange,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    showDatePickerDialog(
                                        context = context,
                                        initialDate = parseLocalDateOrNull(reminderDate)
                                    ) { selectedDate ->
                                        reminderDate = selectedDate.format(isoDateFormatter)
                                    }
                                }
                            )
                            PickerField(
                                value = reminderTime,
                                label = stringResource(R.string.push_time_label),
                                icon = Icons.Default.AccessTime,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val (initialHour, initialMinute) = parseHourMinuteOrNull(reminderTime)
                                        ?: Pair(reminderHour.toIntOrNull() ?: 19, reminderMinute.toIntOrNull() ?: 0)
                                    showTimePickerDialog(
                                        context = context,
                                        initialHour = initialHour,
                                        initialMinute = initialMinute
                                    ) { selectedHour, selectedMinute ->
                                        reminderTime = autoInsertTimeColon(String.format(Locale.GERMANY, "%02d%02d", selectedHour, selectedMinute))
                                    }
                                }
                            )
                        }

                        Button(
                            onClick = {
                                val weatherDay = parseEpochDayFromInput(weatherForecastDate)
                                val oneTimeReminder = if (remindersEnabled) {
                                    parseReminderTriggerMillis(reminderDate, reminderTime)
                                } else {
                                    null
                                }
                                val (hourFromTime, minuteFromTime) = parseHourMinuteOrNull(reminderTime)
                                    ?: Pair(reminderHour.toIntOrNull() ?: 19, reminderMinute.toIntOrNull() ?: 0)
                                onSaveAutomationSettings(
                                    weatherLocation.trim(),
                                    weatherDay,
                                    remindersEnabled,
                                    hourFromTime,
                                    minuteFromTime,
                                    oneTimeReminder
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Für diese Liste speichern")
                        }
                    }
                }
            }
        }

        item {
            WeatherCard(
                weather = uiState.weather,
                forecastEpochDay = uiState.list.weatherForecastEpochDay,
                suggestions = uiState.weatherSuggestions,
                loading = uiState.weatherLoading,
                error = uiState.weatherError,
                onRefresh = { onRefreshWeather(true) }
            )
        }

        if (uiState.items.isEmpty()) {
            item {
                EmptyState("Tippe auf +, um dein erstes Item hinzuzufügen.")
            }
        } else {
            items(uiState.items, key = { it.id }) { item ->
                SwipePackItem(
                    item = item,
                    onTogglePacked = { checked -> onTogglePacked(item, checked) },
                    onSwipePacked = { onSwipePacked(item) }
                )
            }
        }
    }

    if (showSuccess) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSuccess = false },
            title = { Text("Alles gepackt") },
            text = { Text("Stark, deine Liste ist vollständig. Du kannst jetzt resetten oder zur Übersicht zurück.") },
            confirmButton = {
                Button(onClick = {
                    showSuccess = false
                    onBack()
                }) {
                    Text("Zur Listenübersicht")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showSuccess = false
                    onReset()
                }) {
                    Text("Liste zurücksetzen")
                }
            }
        )
    }
}

@Composable
private fun WeatherCard(
    weather: WeatherSnapshot?,
    forecastEpochDay: Long?,
    suggestions: List<String>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wetter", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = onRefresh) { Text(stringResource(R.string.weather_reload)) }
            }

            if (weather != null) {
                Text(
                    text = stringResource(R.string.weather_updated_at, formatUpdatedTime(weather.fetchedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when {
                loading -> Text(stringResource(R.string.weather_loading), style = MaterialTheme.typography.bodyMedium)
                error != null -> Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                weather == null -> Text(stringResource(R.string.weather_unavailable), style = MaterialTheme.typography.bodyMedium)
                else -> {
                    Text(
                        weather.locationLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    forecastEpochDay
                        ?.takeIf { it > LocalDate.now(ZoneId.systemDefault()).toEpochDay() }
                        ?.let {
                            Text(
                                text = stringResource(
                                    R.string.weather_forecast_for,
                                    LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY))
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    Text(
                        "Min ${weather.temperatureMinC}°C · Max ${weather.temperatureMaxC}°C · Regen ${weather.precipitationProbabilityMax}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (suggestions.isNotEmpty()) {
                        Text("Vorschläge: ${suggestions.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressStrip(
    packedCount: Int,
    totalCount: Int,
    packedWeight: Int,
    totalWeight: Int
) {
    val percent = if (totalCount == 0) 0 else ((packedCount * 100f) / totalCount).toInt()
    val packedWeightText = if (packedWeight >= 1000) {
        String.format("%.1f kg", packedWeight / 1000f)
    } else {
        "$packedWeight g"
    }
    val totalWeightText = if (totalWeight >= 1000) {
        String.format("%.1f kg", totalWeight / 1000f)
    } else {
        "$totalWeight g"
    }
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Fortschritt", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "$packedCount von $totalCount gepackt ($percent%)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Gewicht: $packedWeightText / $totalWeightText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipePackItem(
    item: PackingItemEntity,
    onTogglePacked: (Boolean) -> Unit,
    onSwipePacked: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (item.isPacked) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 280),
        label = "packItemColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (item.isPacked) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 280),
        label = "packItemTextColor"
    )

    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onSwipePacked()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DoneAll, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Gepackt")
                }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isPacked,
                    onCheckedChange = onTogglePacked
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    textDecoration = if (item.isPacked) TextDecoration.LineThrough else null,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (item.isPacked) 0.6f else 1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNameDialog(
    title: String,
    label: String,
    value: String,
    confirmText: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(if (it.isEmpty()) it else it[0].uppercase() + it.drop(1)) },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemDialog(
    title: String,
    label: String,
    value: String,
    weightValue: String,
    confirmText: String,
    onValueChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(if (it.isEmpty()) it else it[0].uppercase() + it.drop(1)) },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightValue,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            onWeightChange(input)
                        }
                    },
                    label = { Text("Gewicht in Gramm (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun PickerField(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    OutlinedTextField(
        value = value,
        onValueChange = { },
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(icon, contentDescription = label)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    )
}

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds < 0) return "--:--"
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

private val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private fun formatEpochDayForInput(epochDay: Long?): String {
    return epochDay?.let { LocalDate.ofEpochDay(it).format(isoDateFormatter) }.orEmpty()
}

private fun formatReminderDateForInput(triggerAtMillis: Long?): String {
    if (triggerAtMillis == null) return ""
    val localDate = LocalDateTime.ofInstant(
        Date(triggerAtMillis).toInstant(),
        ZoneId.systemDefault()
    ).toLocalDate()
    return localDate.format(isoDateFormatter)
}

private fun formatReminderTimeForInput(triggerAtMillis: Long?, fallbackHour: Int, fallbackMinute: Int): String {
    if (triggerAtMillis == null) {
        return String.format(Locale.GERMANY, "%02d:%02d", fallbackHour, fallbackMinute)
    }
    val localDateTime = LocalDateTime.ofInstant(
        Date(triggerAtMillis).toInstant(),
        ZoneId.systemDefault()
    )
    return String.format(Locale.GERMANY, "%02d:%02d", localDateTime.hour, localDateTime.minute)
}

private fun parseEpochDayFromInput(value: String): Long? {
    val normalized = value.trim()
    if (normalized.isEmpty()) return null
    return runCatching { LocalDate.parse(normalized, isoDateFormatter).toEpochDay() }.getOrNull()
}

private fun parseLocalDateOrNull(value: String): LocalDate? {
    val normalized = value.trim()
    if (normalized.isEmpty()) return null
    return runCatching { LocalDate.parse(normalized, isoDateFormatter) }.getOrNull()
}

private fun parseHourMinuteOrNull(value: String): Pair<Int, Int>? {
    val match = Regex("^(\\d{1,2}):(\\d{1,2})$").find(value.trim()) ?: return null
    val hour = match.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return null
    val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return null
    return hour to minute
}

private fun autoInsertTimeColon(input: String): String {
    val trimmed = input.trim()
    val digits = trimmed.filter { it.isDigit() }
    return when {
        trimmed.contains(':') -> trimmed
        digits.length <= 2 -> digits
        else -> digits.take(2) + ":" + digits.drop(2).take(2)
    }
}

private fun parseReminderTriggerMillis(dateValue: String, timeValue: String): Long? {
    val date = runCatching { LocalDate.parse(dateValue.trim(), isoDateFormatter) }.getOrNull() ?: return null
    val match = Regex("^(\\d{1,2}):(\\d{1,2})$").find(timeValue.trim()) ?: return null
    val hour = match.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return null
    val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return null
    val localDateTime = date.atTime(hour, minute)
    return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun formatUpdatedTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(timestamp))
}

private fun showDatePickerDialog(
    context: Context,
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val safeDate = initialDate ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        safeDate.year,
        safeDate.monthValue - 1,
        safeDate.dayOfMonth
    ).show()
}

private fun showTimePickerDialog(
    context: Context,
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimeSelected(hourOfDay, minute)
        },
        initialHour.coerceIn(0, 23),
        initialMinute.coerceIn(0, 59),
        true
    ).show()
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
private fun OnboardingOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Willkommen bei PackPilot", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Kurzstart:", style = MaterialTheme.typography.titleSmall)
                Text("1. Im Tab Listen per + eine neue Liste erstellen.")
                Text("2. Liste öffnen und im Tab Packen per + Items ergänzen.")
                Text("3. In Einstellungen passt du Design und Limits an.")
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Loslegen")
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, title: String, value: String) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HistoryCard(entry: ActivityEventEntity) {
    val headline = when (entry.eventType) {
        "LIST_CREATED" -> "Liste erstellt"
        "LIST_RENAMED" -> "Liste umbenannt"
        "LIST_DELETED" -> "Liste gelöscht"
        "LIST_LAYOUT_COPIED" -> "Layout kopiert"
        "ITEM_ADDED" -> "Item hinzugefügt"
        "ITEM_PACKED" -> "Item gepackt"
        "ITEM_UNPACKED" -> "Item entpackt"
        "LIST_RESET" -> "Liste zurückgesetzt"
        else -> entry.eventType
    }
    val detail = listOfNotNull(entry.listName, entry.itemTitle)
        .joinToString(" · ")
        .ifBlank { "Aktion ohne weitere Details" }
    val timeLabel = formatEventTime(entry.timestamp)
    val badgeLabel = eventBadgeLabel(entry.eventType)
    val badgeColor = eventBadgeColor(entry.eventType)

    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(headline, fontWeight = FontWeight.SemiBold)
            }
            AssistChip(
                onClick = { },
                enabled = false,
                label = { Text(badgeLabel) },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = badgeColor.copy(alpha = 0.18f),
                    disabledLabelColor = badgeColor
                )
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun eventBadgeLabel(eventType: String): String = when (eventType) {
    "ITEM_PACKED" -> "Packen"
    "ITEM_UNPACKED" -> "Entpacken"
    "ITEM_ADDED" -> "Item"
    "LIST_CREATED" -> "Liste"
    "LIST_RENAMED" -> "Liste"
    "LIST_DELETED" -> "Liste"
    "LIST_LAYOUT_COPIED" -> "Kopie"
    "LIST_RESET" -> "Reset"
    else -> "Event"
}

private fun eventBadgeColor(eventType: String): Color = when (eventType) {
    "ITEM_PACKED" -> Color(0xFF2E7D32)
    "ITEM_UNPACKED" -> Color(0xFFEF6C00)
    "ITEM_ADDED" -> Color(0xFF1565C0)
    "LIST_CREATED" -> Color(0xFF6A1B9A)
    "LIST_RENAMED" -> Color(0xFF283593)
    "LIST_DELETED" -> Color(0xFFC62828)
    "LIST_LAYOUT_COPIED" -> Color(0xFF00897B)
    "LIST_RESET" -> Color(0xFF455A64)
    else -> Color(0xFF546E7A)
}

private fun formatEventTime(timestamp: Long): String {
    val eventDate = Date(timestamp)
    val now = Calendar.getInstance()
    val eventCal = Calendar.getInstance().apply { timeInMillis = timestamp }

    val datePrefix = when {
        now.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == eventCal.get(Calendar.DAY_OF_YEAR) -> "Heute"
        now.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - eventCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Gestern"
        else -> SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(eventDate)
    }

    val time = SimpleDateFormat("HH:mm", Locale.GERMANY).format(eventDate)
    return "$datePrefix · $time"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    contentPadding: PaddingValues,
    uiState: SettingsUiState,
    onDesignModeSelected: (DesignMode) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onLuggageLimitKgChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TopAppBar(title = { Text("Einstellungen", fontWeight = FontWeight.SemiBold) })

        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Designmodus", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Wähle den Stil für die gesamte App.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.designMode == DesignMode.MINIMAL,
                        onClick = { onDesignModeSelected(DesignMode.MINIMAL) },
                        label = { Text("Minimal") }
                    )
                    FilterChip(
                        selected = uiState.designMode == DesignMode.SPORTLICH,
                        onClick = { onDesignModeSelected(DesignMode.SPORTLICH) },
                        label = { Text("Sportlich") }
                    )
                }
                HorizontalDivider()
                Text(
                    "Aktiver Modus: ${if (uiState.designMode == DesignMode.MINIMAL) "Minimal" else "Sportlich"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()
                Text("Darstellung", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Wähle Hell-/Dunkelmodus für die App.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeSelected(ThemeMode.SYSTEM) },
                        label = { Text("System") }
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeSelected(ThemeMode.LIGHT) },
                        label = { Text("Hell") }
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeSelected(ThemeMode.DARK) },
                        label = { Text("Dunkel") }
                    )
                }

                HorizontalDivider()
                Text("Gewichtslimit", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Lege ein Kofferlimit fest. Die Gewichtsanzeige hilft dir beim Einhalten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = uiState.luggageLimitKg.toString(),
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            input.toIntOrNull()?.let(onLuggageLimitKgChanged)
                        }
                    },
                    label = { Text("Limit (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessScreen(
    onBackToHome: () -> Unit,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(0.95f)
            )
            Text(
                text = "Alles gepackt!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sehr gut gemacht. Du bist bereit.",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onBackToHome, modifier = Modifier.fillMaxWidth()) {
                Text("Zur Listenübersicht")
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Liste zurücksetzen")
            }
        }
    }
}
