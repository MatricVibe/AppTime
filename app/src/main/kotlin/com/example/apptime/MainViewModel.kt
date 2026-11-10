package com.example.apptime

import android.content.SharedPreferences
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainViewModel(private val usageHelper: UsageStatsHelper, private val repository: FirebaseRepository, private val prefs: SharedPreferences) : ViewModel() {
    val ui = UiSettings(prefs, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val trackerInstallTimeMillis = usageHelper.getTrackerInstallTime()
    
    var hasPermission by mutableStateOf(usageHelper.hasUsagePermission()); private set
    var selectedPeriod by mutableStateOf(StatsPeriod.DAY); private set
    var usageList by mutableStateOf<List<AppUsageModel>>(emptyList()); private set
    var todayUsageSnapshot by mutableStateOf<List<AppUsageModel>>(emptyList()); private set
    var previousTotalMillis by mutableLongStateOf(0L); private set
    var weeklyBreakdown by mutableStateOf(List(7) { 0L }); private set
    var todayMinutes by mutableLongStateOf(0L); private set
    var leaderboard by mutableStateOf<List<LeaderboardEntry>>(emptyList()); private set
    var currentUid by mutableStateOf(repository.currentUid); private set
    var isLoading by mutableStateOf(false); private set
    var statusMessage by mutableStateOf<String?>(null); private set
    var needsNickname by mutableStateOf(false); private set
    var isCheckingUsername by mutableStateOf(false); private set
    var usernameError by mutableStateOf<String?>(null); private set

    // ПЕРЕМЕННЫЕ АДМИНКИ
    var isAdminMode by mutableStateOf(false); private set
    var showAdminPasswordDialog by mutableStateOf(false); private set
    var adminPasswordError by mutableStateOf<String?>(null); private set
    var adminUsers by mutableStateOf<List<LeaderboardEntry>>(emptyList()); private set

    val playerName: String get() = ui.nickname
    fun updatePlayerName(newName: String) { ui.nickname = newName }

    init {
        checkDailyLoginStreak()
        refreshLocalUsage()
        loadLeaderboard()
        if (repository.getCurrentNickname().isBlank()) needsNickname = true
    }

    private fun checkDailyLoginStreak() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = format.format(Date())
        val yesterdayStr = format.format(Date(System.currentTimeMillis() - 86_400_000L))
        val lastLoginStr = prefs.getString("last_login_date", "").orEmpty()
        
        if (lastLoginStr.isEmpty()) { ui.streakDays = 1; prefs.edit().putString("last_login_date", todayStr).apply() }
        else if (lastLoginStr != todayStr) {
            ui.streakDays = if (lastLoginStr == yesterdayStr) ui.streakDays + 1 else 1
            prefs.edit().putString("last_login_date", todayStr).apply()
        }
    }

    fun registerUsername(newUsername: String) {
        val cleanInput = newUsername.trim()
        if (cleanInput.length < 3) { usernameError = "Минимум 3 символа"; return }
        if (!cleanInput.matches(Regex("^[a-zA-Z0-9_]+$"))) { usernameError = "Только буквы, цифры и _"; return }
        viewModelScope.launch {
            isCheckingUsername = true; usernameError = null
            try {
                if (repository.isUsernameAvailable(cleanInput)) {
                    val latestMinutes = withContext(Dispatchers.IO) { usageHelper.getTodayUsageMinutes() }
                    todayMinutes = latestMinutes
                    ui.nickname = cleanInput; ui.username = cleanInput
                    repository.registerUsernameAndSubmit(ui.nickname, ui.username, latestMinutes)
                    needsNickname = false; loadLeaderboard(); statusMessage = "Ник @$cleanInput успешно занят!"
                } else usernameError = "Этот юзернейм уже занят"
            } catch (e: Exception) { usernameError = "Ошибка сети: ${e.localizedMessage}" } finally { isCheckingUsername = false }
        }
    }

    fun selectPeriod(period: StatsPeriod) { selectedPeriod = period; ui.periodsMask = ui.periodsMask or period.maskBit; refreshLocalUsage() }
    fun setAppLimit(packageName: String, limitMinutes: Int) { val updated = ui.appLimitsMap.toMutableMap(); if (limitMinutes <= 0) updated.remove(packageName) else updated[packageName] = limitMinutes; ui.appLimitsMap = updated }

    fun refreshLocalUsage() {
        hasPermission = usageHelper.hasUsagePermission()
        if (!hasPermission) { usageList = emptyList(); todayUsageSnapshot = emptyList(); todayMinutes = 0L; return }
        viewModelScope.launch {
            val (periodData, prevTotal, weekBars, dayData) = withContext(Dispatchers.IO) {
                val cur = usageHelper.getUsageForPeriod(selectedPeriod, ui.hideSystemApps)
                val prev = usageHelper.getPreviousPeriodTotalTime(selectedPeriod)
                val bars = if (selectedPeriod == StatsPeriod.WEEK && ui.showWeeklyChart) usageHelper.getWeeklyDailyBreakdown() else emptyList()
                val day = if (selectedPeriod == StatsPeriod.DAY) cur else usageHelper.getUsageForPeriod(StatsPeriod.DAY, ui.hideSystemApps)
                Quadruple(cur, prev, bars, day)
            }
            usageList = periodData; previousTotalMillis = prevTotal; if (weekBars.isNotEmpty()) weeklyBreakdown = weekBars
            todayUsageSnapshot = dayData; todayMinutes = dayData.sumOf { it.timeInMillis } / 60_000L
        }
    }

    suspend fun loadCustomRangeUsage(startTime: Long, endTime: Long, perAppInstall: Boolean): List<AppUsageModel> = withContext(Dispatchers.IO) {
        if (!usageHelper.hasUsagePermission()) return@withContext emptyList()
        usageHelper.getUsageForCustomRange(startTime, endTime, ui.hideSystemApps, perAppInstall)
    }

    fun clearAllCaches() { usageHelper.clearCache(); refreshLocalUsage() }

    fun loadLeaderboard() { viewModelScope.launch { isLoading = true; statusMessage = null; try { leaderboard = repository.getTop50Leaderboard(); currentUid = repository.currentUid } catch (e: Exception) { statusMessage = "Ошибка загрузки: ${e.localizedMessage}" } finally { isLoading = false } } }

    fun submitCurrentResult() {
        viewModelScope.launch {
            isLoading = true; statusMessage = null
            try {
                val latestMinutes = withContext(Dispatchers.IO) { usageHelper.getTodayUsageMinutes() }
                todayMinutes = latestMinutes
                repository.submitScore(ui.nickname, ui.username.removePrefix("@"), latestMinutes)
                currentUid = repository.currentUid; leaderboard = repository.getTop50Leaderboard()
                statusMessage = "Результат ($latestMinutes мин) отправлен в Топ-50!"
            } catch (e: Exception) { statusMessage = "Ошибка отправки: ${e.localizedMessage}" } finally { isLoading = false }
        }
    }

    fun buildAchievements(): List<AchievementItem> {
        val todayTotalHours = todayUsageSnapshot.sumOf { it.timeInMillis } / 3600_000f
        val isUnderGoal = hasPermission && todayUsageSnapshot.isNotEmpty() && todayTotalHours <= ui.dailyGoalHours
        val customCount = ui.customizationChangesCount.coerceAtMost(5)
        val periodsUnlockedCount = (if ((ui.periodsMask and 1) != 0) 1 else 0) + (if ((ui.periodsMask and 2) != 0) 1 else 0) + (if ((ui.periodsMask and 4) != 0) 1 else 0)
        val limitsCount = ui.appLimitsMap.size
        val explorerActions = (ui.appInspectCount + ui.searchUsedCount).coerceAtMost(3)
        val inLeaderboard = leaderboard.any { it.uid.isNotEmpty() && it.uid == currentUid }

        return listOf(
            AchievementItem("first_step", "🏆", "Первый шаг", "Выдайте разрешение Usage Access.", 100, if (hasPermission) 1f else 0.25f, if (hasPermission) "1 / 1" else "0 / 1", hasPermission, "first_step" in ui.claimedAchievementIds),
            AchievementItem("cloud_champion", "☁️", "Покоритель онлайна", "Отправьте результат в Топ-50.", 250, if (inLeaderboard) 1f else 0.2f, if (inLeaderboard) "В топе!" else "Ждем результат", inLeaderboard, "cloud_champion" in ui.claimedAchievementIds),
            AchievementItem("period_navigator", "🧭", "Хроно-навигатор", "Просмотрите все 3 периода.", 150, (periodsUnlockedCount / 3f).coerceIn(0f, 1f), "$periodsUnlockedCount / 3", periodsUnlockedCount >= 3, "period_navigator" in ui.claimedAchievementIds),
            AchievementItem("app_inspector", "🔍", "Искатель деталей", "Поиск/карточка 3 раза.", 200, (explorerActions / 3f).coerceIn(0f, 1f), "$explorerActions / 3", explorerActions >= 3, "app_inspector" in ui.claimedAchievementIds),
            AchievementItem("limit_guardian", "⏳", "Контролер лимитов", "Установите дневной лимит.", 250, if (limitsCount >= 1) 1f else 0f, if (limitsCount >= 1) "Активно: $limitsCount" else "0 / 1", limitsCount >= 1, "limit_guardian" in ui.claimedAchievementIds),
            AchievementItem("style_architect", "🎨", "Архитектор стиля", "Измените стиль 5 раз.", 200, (customCount / 5f).coerceIn(0f, 1f), "$customCount / 5", customCount >= 5, "style_architect" in ui.claimedAchievementIds),
            AchievementItem("identity_pro", "⚡", "Личный бренд", "Загрузите аватар.", 150, if (ui.avatarPath.isNotEmpty()) 1f else 0.4f, if (ui.avatarPath.isNotEmpty()) "1 / 1" else "0 / 1", ui.avatarPath.isNotEmpty(), "identity_pro" in ui.claimedAchievementIds),
            AchievementItem("digital_detox", "💎", "Цифровой баланс", "Удерживайте норму (${ui.dailyGoalHours.roundToInt()} ч).", 300, if (isUnderGoal) 1f else (ui.dailyGoalHours / todayTotalHours.coerceAtLeast(1f)).coerceIn(0.15f, 0.9f), if (isUnderGoal) "В норме" else "Цель: ≤ ${ui.dailyGoalHours.roundToInt()} ч", isUnderGoal, "digital_detox" in ui.claimedAchievementIds),
            AchievementItem("streak_master", "🔥", "Неделя дисциплины", "Заходите 7 дней подряд.", 400, (ui.streakDays / 7f).coerceIn(0f, 1f), "${ui.streakDays.coerceAtMost(7)} / 7", ui.streakDays >= 7, "streak_master" in ui.claimedAchievementIds)
        )
    }

    // --- ФУНКЦИИ АДМИНКИ ---
    fun requestAdminAccess() { showAdminPasswordDialog = true; adminPasswordError = null }
    fun cancelAdminAccess() { showAdminPasswordDialog = false; adminPasswordError = null }
    fun exitAdminMode() { isAdminMode = false }

    fun verifyAdminPassword(password: String) {
        if (password == "Bmw_2009") { 
            isAdminMode = true
            showAdminPasswordDialog = false
            loadAllUsersForAdmin()
        } else {
            adminPasswordError = "Неверный пароль"
        }
    }

    fun loadAllUsersForAdmin() {
        viewModelScope.launch {
            isLoading = true
            try { adminUsers = repository.getAllUsers() } 
            catch (e: Exception) { statusMessage = "Ошибка загрузки: ${e.localizedMessage}" } 
            finally { isLoading = false }
        }
    }

    fun deleteUserAdmin(uid: String, username: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                repository.deleteUser(uid, username)
                loadAllUsersForAdmin()
                statusMessage = "Пользователь удален!"
            } catch (e: Exception) { statusMessage = "Ошибка удаления: ${e.localizedMessage}" } 
            finally { isLoading = false }
        }
    }

    class Factory(private val usageHelper: UsageStatsHelper, private val repository: FirebaseRepository, private val prefs: SharedPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(usageHelper, repository, prefs) as T
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)