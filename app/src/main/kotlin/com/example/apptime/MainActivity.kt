// === Файл: MainActivity.kt ===
package com.example.apptime

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        val prefs = getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE)
        val usageHelper = UsageStatsHelper(applicationContext)
        val repository = FirebaseRepository()
        val factory = MainViewModel.Factory(usageHelper, repository, prefs)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        lifecycleScope.launch {
            GitHubUpdater.checkForUpdates(this@MainActivity)
        }

        setContent {
            TimeTrackerRoot(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTrackerRoot(viewModel: MainViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ui = viewModel.ui

    var selectedScreen by remember { mutableStateOf(ScreenTab.TRACKER) }
    var manualRefreshTrigger by remember { mutableIntStateOf(0) }

    val animatedCornerRadius by animateFloatAsState(
        targetValue = ui.cornerRadius,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "CornerRadiusAnim"
    )

    fun triggerHaptic() {
        if (ui.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocalUsage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(manualRefreshTrigger, ui.hideSystemApps, ui.showWeeklyChart) {
        while (true) {
            viewModel.refreshLocalUsage()
            delay(5000L)
        }
    }

    val achievementsList = viewModel.buildAchievements()
    val readyToClaimCount = achievementsList.count { it.isUnlocked && !it.isClaimed }

    val colorScheme = rememberAnimatedAppColorScheme(ui)
    val typography = rememberAppTypography(ui)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val currentDensity = LocalDensity.current

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = ui.fontScale
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            // ОКНО ВВОДА УНИКАЛЬНОГО ЮЗЕРНЕЙМА
            if (viewModel.needsNickname) {
                var nameInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Создай свой юзернейм") },
                    text = {
                        Column {
                            Text("Он будет отображаться в Топ-50. Выбери уникальный.")
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { 
                                    val filtered = it.filter { char -> char.isLetterOrDigit() || char == '_' }
                                    nameInput = filtered 
                                },
                                leadingIcon = { Text("@", style = MaterialTheme.typography.titleMedium) },
                                label = { Text("username") },
                                singleLine = true,
                                isError = viewModel.usernameError != null,
                                enabled = !viewModel.isCheckingUsername
                            )
                            if (viewModel.usernameError != null) {
                                Text(
                                    text = viewModel.usernameError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = nameInput.isNotBlank() && !viewModel.isCheckingUsername,
                            onClick = { viewModel.registerUsername(nameInput) },
                            shape = CircleShape
                        ) {
                            if (viewModel.isCheckingUsername) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp), 
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Занять @${nameInput}")
                            }
                        }
                    }
                )
            }

            // ОКНО ВВОДА ПАРОЛЯ АДМИНА
            if (viewModel.showAdminPasswordDialog) {
                var passwordInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { viewModel.cancelAdminAccess() },
                    title = { Text("Вход в Админ-панель") },
                    text = {
                        Column {
                            Text("Введите секретный пароль:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Пароль") },
                                isError = viewModel.adminPasswordError != null
                            )
                            if (viewModel.adminPasswordError != null) {
                                Text(
                                    text = viewModel.adminPasswordError ?: "",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.verifyAdminPassword(passwordInput) }) {
                            Text("Войти")
                        }
                    }
                )
            }

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .let { mod ->
                        if (ui.topBarStyle == TopBarStyle.LARGE) {
                            mod.nestedScroll(scrollBehavior.nestedScrollConnection)
                        } else mod
                    },
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    ExpressiveTopBar(
                        selectedScreen = selectedScreen,
                        ui = ui,
                        readyToClaimCount = readyToClaimCount,
                        scrollBehavior = scrollBehavior,
                        onOpenProfile = {
                            triggerHaptic()
                            selectedScreen = ScreenTab.PROFILE
                        },
                        onRefresh = {
                            triggerHaptic()
                            manualRefreshTrigger++
                            viewModel.refreshLocalUsage()
                            if (selectedScreen == ScreenTab.LEADERBOARD) {
                                viewModel.loadLeaderboard()
                            }
                            Toast.makeText(context, "Статистика обновлена", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                bottomBar = {
                    FloatingExpressiveNavigationBar(
                        selectedScreen = selectedScreen,
                        ui = ui,
                        animatedCornerRadius = animatedCornerRadius,
                        readyBadgesCount = readyToClaimCount,
                        onSelectScreen = { tab ->
                            if (selectedScreen != tab) {
                                triggerHaptic()
                                selectedScreen = tab
                            }
                        }
                    )
                }
            ) { innerPadding ->
                
                // ЕСЛИ ВКЛЮЧЕН РЕЖИМ АДМИНА - ПОКАЗЫВАЕМ АДМИНКУ
                if (viewModel.isAdminMode) {
                    AdminScreen(viewModel, innerPadding, animatedCornerRadius, ::triggerHaptic)
                } else {
                    AnimatedContent(
                        targetState = selectedScreen,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            when (ui.navAnimation) {
                                NavAnimationType.SLIDE -> {
                                    val direction = if (targetState.index >= initialState.index) 1 else -1
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        initialOffsetX = { w -> (direction * w * 0.35f).roundToInt() }
                                    ) + fadeIn(tween(240))).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                                            targetOffsetX = { w -> (-direction * w * 0.25f).roundToInt() }
                                        ) + fadeOut(tween(180))
                                    )
                                }
                                NavAnimationType.FADE -> {
                                    (fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260)))
                                        .togetherWith(fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)))
                                }
                                NavAnimationType.NONE -> {
                                    ContentTransform(fadeIn(tween(0)), fadeOut(tween(0)))
                                }
                            }
                        },
                        label = "ScreenNavigation"
                    ) { screen ->
                        val contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = innerPadding.calculateTopPadding() + 4.dp,
                            bottom = innerPadding.calculateBottomPadding() + 24.dp
                        )

                        when (screen) {
                            ScreenTab.TRACKER -> TrackerScreen(
                                viewModel = viewModel,
                                contentPadding = contentPadding,
                                cornerRadius = animatedCornerRadius,
                                onOpenCustomPeriod = {
                                    triggerHaptic()
                                    selectedScreen = ScreenTab.CUSTOM_PERIOD
                                },
                                onOpenLeaderboard = {
                                    triggerHaptic()
                                    selectedScreen = ScreenTab.LEADERBOARD
                                },
                                onHaptic = ::triggerHaptic
                            )
                            ScreenTab.CUSTOM_PERIOD -> CustomPeriodScreen(
                                viewModel = viewModel,
                                contentPadding = contentPadding,
                                cornerRadius = animatedCornerRadius,
                                manualRefreshTrigger = manualRefreshTrigger,
                                onHaptic = ::triggerHaptic
                            )
                            ScreenTab.LEADERBOARD -> LeaderboardScreen(
                                viewModel = viewModel,
                                contentPadding = contentPadding,
                                cornerRadius = animatedCornerRadius,
                                onHaptic = ::triggerHaptic
                            )
                            ScreenTab.PROFILE -> ProfileScreen(
                                viewModel = viewModel,
                                achievements = achievementsList,
                                contentPadding = contentPadding,
                                cornerRadius = animatedCornerRadius,
                                onNavigateToThemes = {
                                    triggerHaptic()
                                    selectedScreen = ScreenTab.SETTINGS
                                },
                                onHaptic = ::triggerHaptic
                            )
                            ScreenTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                contentPadding = contentPadding,
                                cornerRadius = animatedCornerRadius,
                                onHaptic = ::triggerHaptic
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- ЭКРАН АДМИНКИ ---
@Composable
fun AdminScreen(viewModel: MainViewModel, innerPadding: PaddingValues, cornerRadius: Float, onHaptic: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                // ВОТ ОН, ТВОЙ ТЕКУЩИЙ UID!
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Твой текущий UID: ${viewModel.currentUid}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Админка (${viewModel.adminUsers.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row {
                        Button(onClick = { viewModel.loadAllUsersForAdmin() }, shape = CircleShape) { Text("Обновить") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { viewModel.exitAdminMode() }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Выйти") }
                    }
                }
                Text("Удаление освобождает ник!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        itemsIndexed(viewModel.adminUsers) { _, user ->
            ThemedCard(ui = viewModel.ui, cornerRadius = cornerRadius) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = user.name, fontWeight = FontWeight.Bold)
                        Text(text = "@${user.username}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        Text(text = "UID: ${user.uid.take(10)}... | ${user.minutes} мин", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onHaptic(); viewModel.deleteUserAdmin(user.uid, user.username) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Удалить") }
                }
            }
        }
    }
}

@Composable
fun TrackerScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    cornerRadius: Float,
    onOpenCustomPeriod: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onHaptic: () -> Unit
) {
    val context = LocalContext.current
    val ui = viewModel.ui

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.TIME) }
    var minMinutesFilter by remember { mutableIntStateOf(0) }
    var selectedAppForDialog by remember { mutableStateOf<AppUsageModel?>(null) }

    val filteredAndSortedApps = remember(viewModel.usageList, searchQuery, sortOption, minMinutesFilter) {
        val minMillis = minMinutesFilter * 60_000L
        viewModel.usageList
            .filter { app ->
                app.timeInMillis >= minMillis &&
                    (searchQuery.isBlank() || app.appName.contains(searchQuery, ignoreCase = true))
            }
            .let { list ->
                when (sortOption) {
                    SortOption.TIME -> list.sortedByDescending { it.timeInMillis }
                    SortOption.NAME -> list.sortedBy { it.appName.lowercase(Locale.getDefault()) }
                }
            }
    }

    val totalTimeMillis = remember(viewModel.usageList) {
        viewModel.usageList.sumOf { it.timeInMillis }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ui.listDensity.spacingDp.dp)
    ) {
        item(key = "author_banner") {
            AuthorCreditTopBanner(onHaptic = onHaptic)
        }

        if (!viewModel.hasPermission) {
            item(key = "permission_banner") {
                ThemedCard(ui = ui, cornerRadius = cornerRadius) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Доступ к статистике",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Предоставьте разрешение Usage Access в настройках Android, чтобы приложение могло считать экранное время и участвовать в Топ-50.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                onHaptic()
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            },
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Открыть настройки доступа",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item(key = "search_bar") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newValue ->
                    if (searchQuery.isEmpty() && newValue.isNotEmpty()) {
                        ui.searchUsedCount += 1
                    }
                    searchQuery = newValue
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Поиск по названию приложения...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = AppIcons.Search,
                        contentDescription = "Поиск",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = AppIcons.Close,
                                contentDescription = "Очистить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }

        item(key = "period_selector") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentedPeriodSelector(
                    selectedPeriod = viewModel.selectedPeriod,
                    onSelectPeriod = { period ->
                        onHaptic()
                        viewModel.selectPeriod(period)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenCustomPeriod,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = AppIcons.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "С установки",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onOpenLeaderboard,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = AppIcons.Trophy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Топ-50 Онлайн",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item(key = "total_summary") {
            TotalTimeSummaryCard(
                totalMillis = totalTimeMillis,
                previousMillis = viewModel.previousTotalMillis,
                period = viewModel.selectedPeriod,
                ui = ui,
                cornerRadius = cornerRadius
            )
        }

        if (viewModel.selectedPeriod == StatsPeriod.WEEK && ui.showWeeklyChart) {
            item(key = "weekly_chart") {
                WeeklyUsageBarChartCard(
                    dailyMillis = viewModel.weeklyBreakdown,
                    ui = ui,
                    cornerRadius = cornerRadius
                )
            }
        }

        item(key = "filters_row") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdaptiveChoiceGrid(
                    items = SortOption.values().toList(),
                    selectedItem = sortOption,
                    labelProvider = { it.label },
                    columns = 2,
                    onSelect = { option ->
                        onHaptic()
                        sortOption = option
                    }
                )

                val timeThresholds = remember {
                    listOf(0 to "Все", 1 to "> 1 мин", 5 to "> 5 мин", 15 to "> 15 мин")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    timeThresholds.forEach { (mins, title) ->
                        val selected = minMinutesFilter == mins
                        val chipBg by animateColorAsState(
                            targetValue = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            animationSpec = tween(200),
                            label = "TimeFilterBg_$mins"
                        )
                        val chipText by animateColorAsState(
                            targetValue = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(200),
                            label = "TimeFilterTxt_$mins"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(chipBg)
                                .clickable {
                                    onHaptic()
                                    minMinutesFilter = mins
                                    if (mins > 0) ui.searchUsedCount += 1
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = chipText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (filteredAndSortedApps.isEmpty()) {
            item(key = "empty_state") {
                ThemedCard(ui = ui, cornerRadius = cornerRadius) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = AppIcons.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (viewModel.hasPermission) "Нет активных приложений по фильтру" else "Ожидание разрешения статистики",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val maxAppTime = (filteredAndSortedApps.maxOfOrNull { it.timeInMillis } ?: 1L).coerceAtLeast(1L)
            itemsIndexed(
                items = filteredAndSortedApps,
                key = { _, item -> item.packageName }
            ) { index, appModel ->
                val appLimitMins = ui.appLimitsMap[appModel.packageName] ?: 0
                AppUsageItemCard(
                    rankIndex = index,
                    app = appModel,
                    limitMinutes = appLimitMins,
                    totalPeriodMillis = totalTimeMillis.coerceAtLeast(1L),
                    maxTimeMillis = maxAppTime,
                    ui = ui,
                    cornerRadius = cornerRadius,
                    showInstallDate = false,
                    showRankMedal = ui.showRankBadges && sortOption == SortOption.TIME,
                    onClick = {
                        onHaptic()
                        ui.appInspectCount += 1
                        selectedAppForDialog = appModel
                    }
                )
            }
        }
    }

    selectedAppForDialog?.let { app ->
        AppDetailsAndLimitDialog(
            app = app,
            periodLabel = viewModel.selectedPeriod.genitiveTitle,
            totalTimeMillis = totalTimeMillis,
            timeFormatStyle = ui.timeFormatStyle,
            existingLimitMinutes = ui.appLimitsMap[app.packageName] ?: 0,
            onDismiss = { selectedAppForDialog = null },
            onSaveLimit = { mins ->
                onHaptic()
                viewModel.setAppLimit(app.packageName, mins)
                selectedAppForDialog = null
                Toast.makeText(context, "Параметры приложения сохранены", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun CustomPeriodScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    cornerRadius: Float,
    manualRefreshTrigger: Int,
    onHaptic: () -> Unit
) {
    val context = LocalContext.current
    val ui = viewModel.ui
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    var selectedPreset by remember { mutableStateOf(CustomPeriodPreset.SINCE_TRACKER_INSTALL) }
    var startFraction by remember { mutableFloatStateOf(0f) }
    var endFraction by remember { mutableFloatStateOf(1f) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.TIME) }
    var selectedAppForDialog by remember { mutableStateOf<AppUsageModel?>(null) }

    val trackerInstallTimeMillis = viewModel.trackerInstallTimeMillis
    val nowMillis = System.currentTimeMillis()
    val totalInstallSpan = max(60_000L, nowMillis - trackerInstallTimeMillis)

    val (customStartMillis, customEndMillis) = remember(
        selectedPreset,
        startFraction,
        endFraction,
        trackerInstallTimeMillis,
        manualRefreshTrigger
    ) {
        val currentNow = System.currentTimeMillis()
        when (selectedPreset) {
            CustomPeriodPreset.SINCE_TRACKER_INSTALL -> trackerInstallTimeMillis to currentNow
            CustomPeriodPreset.LAST_24H -> max(trackerInstallTimeMillis, currentNow - 24L * 3600_000L) to currentNow
            CustomPeriodPreset.LAST_3D -> max(trackerInstallTimeMillis, currentNow - 3L * 24L * 3600_000L) to currentNow
            CustomPeriodPreset.LAST_7D -> max(trackerInstallTimeMillis, currentNow - 7L * 24L * 3600_000L) to currentNow
            CustomPeriodPreset.SINCE_EACH_APP_INSTALL -> (currentNow - 365L * 24L * 3600_000L) to currentNow
            CustomPeriodPreset.CUSTOM_SLIDER -> {
                val safeStartFrac = startFraction.coerceIn(0f, 0.95f)
                val safeEndFrac = endFraction.coerceIn(safeStartFrac + 0.05f, 1f)
                val span = max(60_000L, currentNow - trackerInstallTimeMillis)
                val s = trackerInstallTimeMillis + (span * safeStartFrac).toLong()
                val e = trackerInstallTimeMillis + (span * safeEndFrac).toLong()
                s to e
            }
        }
    }

    var customUsageList by remember { mutableStateOf<List<AppUsageModel>>(emptyList()) }

    LaunchedEffect(
        viewModel.hasPermission,
        customStartMillis,
        customEndMillis,
        selectedPreset,
        ui.hideSystemApps,
        manualRefreshTrigger
    ) {
        if (viewModel.hasPermission) {
            customUsageList = viewModel.loadCustomRangeUsage(
                startTime = customStartMillis,
                endTime = customEndMillis,
                perAppInstall = selectedPreset == CustomPeriodPreset.SINCE_EACH_APP_INSTALL
            )
        }
    }

    val filteredApps = remember(customUsageList, searchQuery, sortOption) {
        customUsageList
            .filter { app ->
                searchQuery.isBlank() || app.appName.contains(searchQuery, ignoreCase = true)
            }
            .let { list ->
                when (sortOption) {
                    SortOption.TIME -> list.sortedByDescending { it.timeInMillis }
                    SortOption.NAME -> list.sortedBy { it.appName.lowercase(Locale.getDefault()) }
                }
            }
    }

    val totalCustomUsageMillis = remember(customUsageList) {
        customUsageList.sumOf { it.timeInMillis }
    }
    val selectedSpanDuration = max(1L, customEndMillis - customStartMillis)
    val screenTimeSharePercent = ((totalCustomUsageMillis.toDouble() / selectedSpanDuration.toDouble()) * 100.0)
        .roundToInt()
        .coerceIn(0, 100)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ui.listDensity.spacingDp.dp)
    ) {
        item(key = "install_info_card") {
            ThemedCard(ui = ui, cornerRadius = cornerRadius) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "С момента скачивания",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Установлено: ${dateFormat.format(Date(trackerInstallTimeMillis))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "С момента установки приложения прошло: ${formatDuration(totalInstallSpan, TimeFormatStyle.STANDARD)}. Выберите нужный интервал времени:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AdaptiveChoiceGrid(
                        items = CustomPeriodPreset.values().toList(),
                        selectedItem = selectedPreset,
                        labelProvider = { it.label },
                        columns = 2,
                        onSelect = { preset ->
                            onHaptic()
                            selectedPreset = preset
                        }
                    )

                    AnimatedVisibility(visible = selectedPreset == CustomPeriodPreset.CUSTOM_SLIDER) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SettingSliderItem(
                                title = "Начало периода (От)",
                                valueText = dateFormat.format(Date(customStartMillis)),
                                value = startFraction,
                                valueRange = 0f..0.95f,
                                onValueChange = { newStart ->
                                    startFraction = newStart
                                    if (endFraction <= newStart + 0.05f) {
                                        endFraction = (newStart + 0.05f).coerceAtMost(1f)
                                    }
                                }
                            )

                            SettingSliderItem(
                                title = "Конец периода (До)",
                                valueText = dateFormat.format(Date(customEndMillis)),
                                value = endFraction,
                                valueRange = 0.05f..1f,
                                onValueChange = { newEnd ->
                                    endFraction = newEnd.coerceAtLeast(startFraction + 0.05f)
                                }
                            )
                        }
                    }
                }
            }
        }

        item(key = "custom_period_summary") {
            ThemedCard(ui = ui, cornerRadius = cornerRadius) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Проведено в приложениях",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$screenTimeSharePercent% периода",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = formatDuration(totalCustomUsageMillis, ui.timeFormatStyle),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Интервал: ${dateFormat.format(Date(customStartMillis))} — ${dateFormat.format(Date(customEndMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = { (screenTimeSharePercent / 100f).coerceIn(0.03f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round
                    )

                    Text(
                        text = "Активных приложений за выбранный период: ${filteredApps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "custom_period_search_sort") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Поиск приложения в выбранном периоде...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.Search,
                            contentDescription = "Поиск",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = AppIcons.Close,
                                    contentDescription = "Очистить",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                AdaptiveChoiceGrid(
                    items = SortOption.values().toList(),
                    selectedItem = sortOption,
                    labelProvider = { it.label },
                    columns = 2,
                    onSelect = { option ->
                        onHaptic()
                        sortOption = option
                    }
                )
            }
        }

        if (filteredApps.isEmpty()) {
            item(key = "custom_empty") {
                ThemedCard(ui = ui, cornerRadius = cornerRadius) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = AppIcons.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (viewModel.hasPermission) "За выбранный период нет данных" else "Выдайте доступ к статистике на главном экране",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val maxAppTime = (filteredApps.maxOfOrNull { it.timeInMillis } ?: 1L).coerceAtLeast(1L)
            itemsIndexed(
                items = filteredApps,
                key = { _, item -> item.packageName }
            ) { index, appModel ->
                val appLimitMins = ui.appLimitsMap[appModel.packageName] ?: 0
                AppUsageItemCard(
                    rankIndex = index,
                    app = appModel,
                    limitMinutes = appLimitMins,
                    totalPeriodMillis = totalCustomUsageMillis.coerceAtLeast(1L),
                    maxTimeMillis = maxAppTime,
                    ui = ui,
                    cornerRadius = cornerRadius,
                    showInstallDate = true,
                    showRankMedal = ui.showRankBadges && sortOption == SortOption.TIME,
                    onClick = {
                        onHaptic()
                        selectedAppForDialog = appModel
                    }
                )
            }
        }
    }

    selectedAppForDialog?.let { app ->
        AppDetailsAndLimitDialog(
            app = app,
            periodLabel = "выбранный период",
            totalTimeMillis = totalCustomUsageMillis,
            timeFormatStyle = ui.timeFormatStyle,
            existingLimitMinutes = ui.appLimitsMap[app.packageName] ?: 0,
            onDismiss = { selectedAppForDialog = null },
            onSaveLimit = { mins ->
                onHaptic()
                viewModel.setAppLimit(app.packageName, mins)
                selectedAppForDialog = null
                Toast.makeText(context, "Параметры приложения сохранены", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun LeaderboardScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    cornerRadius: Float,
    onHaptic: () -> Unit
) {
    val ui = viewModel.ui
    val maxLeaderMinutes = remember(viewModel.leaderboard) {
        (viewModel.leaderboard.maxOfOrNull { it.minutes } ?: 1L).coerceAtLeast(1L)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ui.listDensity.spacingDp.dp)
    ) {
        item(key = "submit_card") {
            PlayerSubmitCard(
                playerName = viewModel.playerName,
                todayMinutes = viewModel.todayMinutes,
                isLoading = viewModel.isLoading,
                statusMessage = viewModel.statusMessage,
                ui = ui,
                cornerRadius = cornerRadius,
                onNameChange = viewModel::updatePlayerName,
                onSubmit = {
                    onHaptic()
                    viewModel.submitCurrentResult()
                },
                onRefresh = {
                    onHaptic()
                    viewModel.refreshLocalUsage()
                    viewModel.loadLeaderboard()
                }
            )
        }

        item(key = "leaderboard_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Таблица лидеров (Топ-50)",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${viewModel.leaderboard.size} уч.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (viewModel.leaderboard.isEmpty() && !viewModel.isLoading) {
            item(key = "empty_leaderboard") {
                SettingCard(
                    title = "Список пока пуст",
                    ui = ui,
                    cornerRadius = cornerRadius
                ) {
                    Text(
                        text = "Нажмите «Отправить результат», чтобы занять первое место в таблице лидеров Firebase!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(
                items = viewModel.leaderboard,
                key = { index, item -> item.uid.ifEmpty { "${item.name}_$index" } }
            ) { index, entry ->
                LeaderboardItemCard(
                    rank = index + 1,
                    entry = entry,
                    maxMinutesInTop = maxLeaderMinutes,
                    isCurrentUser = entry.uid.isNotEmpty() && entry.uid == viewModel.currentUid,
                    ui = ui,
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    achievements: List<AchievementItem>,
    contentPadding: PaddingValues,
    cornerRadius: Float,
    onNavigateToThemes: () -> Unit,
    onHaptic: () -> Unit
) {
    val context = LocalContext.current
    val ui = viewModel.ui
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedAchievement by remember { mutableStateOf<AchievementItem?>(null) }

    val avatarBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = ui.avatarPath) {
        value = if (ui.avatarPath.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                decodeSampledBitmapFromFile(ui.avatarPath, reqSize = 320)?.asImageBitmap()
            }
        } else null
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("profile_avatar_")) file.delete()
            }
            val savedPath = copyUriToInternalStorage(
                context,
                uri,
                "profile_avatar_${System.currentTimeMillis()}.jpg"
            )
            if (savedPath != null) {
                ui.avatarPath = savedPath
            }
        }
    }

    val unlockedCount = achievements.count { it.isUnlocked }
    val claimedCount = achievements.count { it.isClaimed }
    val readyToClaimList = achievements.filter { it.isUnlocked && !it.isClaimed }

    val claimedXp = achievements.filter { it.isClaimed }.sumOf { it.xpReward }
    val unlockedBonusXp = achievements.filter { it.isUnlocked && !it.isClaimed }.sumOf { it.xpReward / 4 }
    val totalXp = claimedXp + unlockedBonusXp + (ui.streakDays * 25)
    val userLevel = (totalXp / 250) + 1
    val currentLevelXp = totalXp % 250
    val levelProgress by animateFloatAsState(
        targetValue = (currentLevelXp / 250f).coerceIn(0.05f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "LevelProgressAnim"
    )

    val rankTitle = when {
        userLevel >= 7 -> "Магистр Фокуса"
        userLevel >= 5 -> "Хранитель Времени"
        userLevel >= 3 -> "Практик Баланса"
        else -> "Искатель Фокуса"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AvatarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "profile_header_card") {
            ThemedCard(ui = ui, cornerRadius = cornerRadius) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .size(116.dp)
                            .clickable {
                                onHaptic()
                                avatarPickerLauncher.launch("image/*")
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(106.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarBitmap != null) {
                                    Image(
                                        bitmap = avatarBitmap!!,
                                        contentDescription = "Аватар",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(text = "🚀", fontSize = 40.sp)
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Ур. $userLevel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = ui.nickname.ifBlank { "Загрузка..." },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val cleanUsername = ui.username.removePrefix("@")
                    Text(
                        text = "${if (cleanUsername.isNotBlank()) "@$cleanUsername" else ""} • $rankTitle",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ui.userBio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Опыт: $totalXp XP",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(levelProgress * 100).roundToInt()}% до Ур. ${userLevel + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { levelProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            strokeCap = StrokeCap.Round
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                onHaptic()
                                showEditDialog = true
                            },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Изменить", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = {
                                onHaptic()
                                avatarPickerLauncher.launch("image/*")
                            },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Фото", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        item(key = "profile_stats_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val stats = listOf(
                    Triple("🔥", "${ui.streakDays} дн.", "Дней подряд") to null,
                    Triple("🏆", "$unlockedCount / ${achievements.size}", "Открыто") to null,
                    Triple("✨", "$totalXp", "Опыт XP") to null
                )
                stats.forEach { (info, action) ->
                    ThemedCard(
                        ui = ui,
                        cornerRadius = cornerRadius,
                        modifier = Modifier.weight(1f),
                        onClick = action
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = info.first, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = info.second,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = info.third,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item(key = "daily_goal_card") {
            ThemedCard(ui = ui, cornerRadius = cornerRadius) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingSliderItem(
                        title = "Дневная цель экранного времени",
                        valueText = "${ui.dailyGoalHours.roundToInt()} ч / день",
                        value = ui.dailyGoalHours,
                        valueRange = 1f..12f,
                        steps = 10,
                        onValueChange = { ui.dailyGoalHours = it }
                    )

                    Text(
                        text = "Влияет на процент нормы в трекере и получение достижения «Цифровой баланс».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = onNavigateToThemes,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = AppIcons.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Темы Material You и Кастомизация",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item(key = "achievements_card") {
            ThemedCard(ui = ui, cornerRadius = cornerRadius) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Достижения",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Собрано: $claimedCount / ${achievements.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (readyToClaimList.isNotEmpty()) {
                        Button(
                            onClick = {
                                onHaptic()
                                val allUnlockedIds = achievements.filter { it.isUnlocked }.map { it.id }.toSet()
                                ui.claimedAchievementIds = ui.claimedAchievementIds + allUnlockedIds
                                Toast.makeText(context, "Все доступные награды собраны! 🎉", Toast.LENGTH_SHORT).show()
                            },
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🎁 Забрать все награды (${readyToClaimList.size})",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    achievements.forEach { item ->
                        val animAchProgress by animateFloatAsState(
                            targetValue = item.progress,
                            animationSpec = tween(450, easing = FastOutSlowInEasing),
                            label = "AchProg_${item.id}"
                        )
                        val isReadyToClaim = item.isUnlocked && !item.isClaimed

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isReadyToClaim) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
                                    }
                                )
                                .clickable {
                                    onHaptic()
                                    selectedAchievement = item
                                }
                                .padding(14.dp)
                                .alpha(if (item.isUnlocked) 1f else 0.75f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item.isUnlocked) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = item.emoji, fontSize = 22.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.progressText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (item.isClaimed) {
                                        Color(0xFF00C896).copy(alpha = 0.2f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                ) {
                                    Text(
                                        text = if (item.isClaimed) "✅ +${item.xpReward}" else "+${item.xpReward} XP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isClaimed) Color(0xFF00C896) else MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { animAchProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (item.isClaimed) Color(0xFF00C896) else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                strokeCap = StrokeCap.Round
                            )

                            if (isReadyToClaim) {
                                Button(
                                    onClick = {
                                        onHaptic()
                                        ui.claimedAchievementIds = ui.claimedAchievementIds + item.id
                                        Toast.makeText(context, "Награда +${item.xpReward} XP получена! ✨", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Забрать награду +${item.xpReward} XP",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var tempNickname by remember { mutableStateOf(ui.nickname) }
        var tempUsername by remember { mutableStateOf(ui.username) }
        var tempBio by remember { mutableStateOf(ui.userBio) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать профиль") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempNickname,
                        onValueChange = { tempNickname = it },
                        label = { Text("Никнейм (также для Топ-50)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { tempUsername = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                        label = { Text("Юзернейм (без @)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Статус / О себе") },
                        maxLines = 2,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onHaptic()
                        ui.nickname = tempNickname.ifBlank { "Пользователь" }
                        
                        val cleanTempUser = tempUsername.trim().removePrefix("@")
                        ui.username = if (cleanTempUser.isNotBlank()) "@$cleanTempUser" else ""
                        
                        ui.userBio = tempBio.ifBlank { "Контролирую экранное время" }
                        showEditDialog = false
                    },
                    shape = CircleShape
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = { showEditDialog = false },
                    shape = CircleShape
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    selectedAchievement?.let { ach ->
        val isReady = ach.isUnlocked && !ach.isClaimed
        AlertDialog(
            onDismissRequest = { selectedAchievement = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = ach.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = ach.title)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = ach.description, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Прогресс: ${ach.progressText}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = when {
                            ach.isClaimed -> "Статус: Награда +${ach.xpReward} XP уже получена ✅"
                            ach.isUnlocked -> "Статус: Достижение выполнено! Заберите +${ach.xpReward} XP 🎁"
                            else -> "Статус: В процессе выполнения 🔒 (+${ach.xpReward} XP)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isReady) {
                            onHaptic()
                            ui.claimedAchievementIds = ui.claimedAchievementIds + ach.id
                        }
                        selectedAchievement = null
                    },
                    shape = CircleShape
                ) {
                    Text(if (isReady) "Забрать +${ach.xpReward} XP" else "Закрыть")
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    cornerRadius: Float,
    onHaptic: () -> Unit
) {
    val context = LocalContext.current
    val ui = viewModel.ui
    var activeSubTab by remember { mutableStateOf(SettingsSubTab.THEMES) }
    var tapCount by remember { mutableStateOf(0) } // СЧЕТЧИК КЛИКОВ ДЛЯ АДМИНКИ

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = copyUriToInternalStorage(context, uri, "custom_user_font.ttf")
            if (savedPath != null) {
                ui.customFontPath = savedPath
                ui.fontChoice = FontChoice.CUSTOM
                ui.recordStyleChange()
                Toast.makeText(context, "Шрифт .ttf успешно установлен!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Ошибка чтения файла шрифта", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            sendTestPushNotification(context)
        } else {
            Toast.makeText(context, "Разрешение на уведомления отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "settings_subtabs") {
            val subTabs = SettingsSubTab.values()
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp)
            ) {
                val tabWidth = maxWidth / subTabs.size
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * activeSubTab.index,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "SettingsSubTabOffset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    subTabs.forEach { tab ->
                        val selected = activeSubTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable {
                                    onHaptic()
                                    activeSubTab = tab
                                }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }

        item(key = "settings_animated_body") {
            AnimatedContent(
                targetState = activeSubTab,
                transitionSpec = {
                    val direction = if (targetState.index >= initialState.index) 1 else -1
                    (slideInHorizontally(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        initialOffsetX = { w -> (direction * w * 0.25f).roundToInt() }
                    ) + fadeIn(tween(240))).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            targetOffsetX = { w -> (-direction * w * 0.2f).roundToInt() }
                        ) + fadeOut(tween(160))
                    )
                },
                label = "SettingsSubTabContent"
            ) { currentTab ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (currentTab) {
                        SettingsSubTab.THEMES -> {
                            SettingCard(
                                title = "Дизайн-система Material",
                                ui = ui,
                                cornerRadius = cornerRadius
                            ) {
                                Text(
                                    text = "Выберите визуальное поколение интерфейса:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                DesignStylePreset.values().forEach { preset ->
                                    val isSelected = ui.designStyle == preset
                                    val cardBg by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        },
                                        animationSpec = tween(250),
                                        label = "PresetBg_${preset.name}"
                                    )
                                    val textColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(preset.defaultRadius.dp))
                                            .background(cardBg)
                                            .clickable {
                                                onHaptic()
                                                ui.designStyle = preset
                                                ui.cornerRadius = preset.defaultRadius
                                                if (preset == DesignStylePreset.MATERIAL_YOU && ui.supportsDynamic) {
                                                    ui.dynamicColorEnabled = true
                                                } else if (preset == DesignStylePreset.MATERIAL_YOU_3) {
                                                    ui.dynamicColorEnabled = false
                                                }
                                                ui.recordStyleChange()
                                            }
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = preset.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = textColor,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp)
                                            )
                                            if (isSelected) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary
                                                ) {
                                                    Icon(
                                                        imageVector = AppIcons.Check,
                                                        contentDescription = "Активно",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier
                                                            .padding(4.dp)
                                                            .size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = preset.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor.copy(alpha = 0.82f)
                                        )
                                    }
                                }
                            }

                            SettingCard(
                                title = "Палитра и Темный режим",
                                ui = ui,
                                cornerRadius = cornerRadius
                            ) {
                                if (ui.supportsDynamic) {
                                    SettingSwitchRow(
                                        title = "Динамические цвета (Android 12+)",
                                        subtitle = "Генерация палитры из обоев рабочего стола",
                                        checked = ui.dynamicColorEnabled,
                                        onCheckedChange = {
                                            onHaptic()
                                            ui.dynamicColorEnabled = it
                                            ui.recordStyleChange()
                                        }
                                    )
                                }

                                Text(
                                    text = "Фирменные акценты (8 цветов)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        AccentPresets.take(4).forEachIndexed { idx, color ->
                                            AccentColorCircle(
                                                color = color,
                                                isSelected = !ui.dynamicColorEnabled && ui.accentIndex == idx,
                                                onClick = {
                                                    onHaptic()
                                                    ui.accentIndex = idx
                                                    ui.dynamicColorEnabled = false
                                                    ui.recordStyleChange()
                                                }
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        AccentPresets.drop(4).forEachIndexed { offsetIdx, color ->
                                            val idx = offsetIdx + 4
                                            AccentColorCircle(
                                                color = color,
                                                isSelected = !ui.dynamicColorEnabled && ui.accentIndex == idx,
                                                onClick = {
                                                    onHaptic()
                                                    ui.accentIndex = idx
                                                    ui.dynamicColorEnabled = false
                                                    ui.recordStyleChange()
                                                }
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Режим темы",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AdaptiveChoiceGrid(
                                    items = ThemeMode.values().toList(),
                                    selectedItem = ui.themeMode,
                                    labelProvider = { it.label },
                                    columns = 2,
                                    onSelect = {
                                        onHaptic()
                                        ui.themeMode = it
                                        ui.recordStyleChange()
                                    }
                                )
                            }
                        }

                        SettingsSubTab.CUSTOMIZATION -> {
                            SettingCard(
                                title = "Стиль и UI",
                                ui = ui,
                                cornerRadius = cornerRadius
                            ) {
                                SettingSliderItem(
                                    title = "Скругление углов карточек",
                                    valueText = "${ui.cornerRadius.roundToInt()} dp",
                                    value = ui.cornerRadius,
                                    valueRange = 8f..40f,
                                    onValueChange = { ui.cornerRadius = it }
                                )

                                SettingSliderItem(
                                    title = "Размер иконок приложений",
                                    valueText = "${ui.appIconSizeDp.roundToInt()} dp",
                                    value = ui.appIconSizeDp,
                                    valueRange = 38f..62f,
                                    onValueChange = { ui.appIconSizeDp = it }
                                )

                                SettingSwitchRow(
                                    title = "Медали топа приложений",
                                    checked = ui.showRankBadges,
                                    onCheckedChange = {
                                        onHaptic()
                                        ui.showRankBadges = it
                                    }
                                )

                                SettingSwitchRow(
                                    title = "Скрывать системные пакеты",
                                    checked = ui.hideSystemApps,
                                    onCheckedChange = {
                                        onHaptic()
                                        ui.hideSystemApps = it
                                        viewModel.refreshLocalUsage()
                                    }
                                )
                            }
                        }

                        SettingsSubTab.SYSTEM -> {
                            SettingCard(
                                title = "Система и Отклик",
                                ui = ui,
                                cornerRadius = cornerRadius
                            ) {
                                SettingSwitchRow(
                                    title = "Тактильный отклик (Haptic)",
                                    subtitle = "Мягкая вибрация при нажатиях",
                                    checked = ui.hapticEnabled,
                                    onCheckedChange = {
                                        ui.hapticEnabled = it
                                        if (it) onHaptic()
                                    }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onHaptic()
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                                if (granted) sendTestPushNotification(context) else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                sendTestPushNotification(context)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = CircleShape
                                    ) {
                                        Text("Тест Push")
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            onHaptic()
                                            viewModel.clearAllCaches()
                                            Toast.makeText(context, "Кэш очищен", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = CircleShape
                                    ) {
                                        Text("Сброс кэша")
                                    }
                                }

                                // СЕКРЕТНЫЙ ВХОД В АДМИНКУ (5 кликов)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            tapCount++
                                            if (tapCount >= 5) {
                                                onHaptic()
                                                viewModel.requestAdminAccess()
                                                tapCount = 0
                                            }
                                        }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "AppTime v1.0",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun decodeSampledBitmapFromFile(path: String, reqSize: Int): android.graphics.Bitmap? {
    return try {
        val file = File(path)
        if (!file.exists()) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        var inSampleSize = 1
        val (height, width) = options.outHeight to options.outWidth
        if (height > reqSize || width > reqSize) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqSize && (halfWidth / inSampleSize) >= reqSize) {
                inSampleSize *= 2
            }
        }
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        })
    } catch (e: Exception) { null }
}

private fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val destFile = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        }
        destFile.absolutePath
    } catch (e: Exception) { null }
}

private fun sendTestPushNotification(context: Context) {
    val channelId = "timetracker_test_channel"
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(NotificationChannel(channelId, "TimeTracker Уведомления", NotificationManager.IMPORTANCE_HIGH))
    }
    manager.notify(1001, NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("TimeTracker")
        .setContentText("Уведомление доставлено!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build())
}