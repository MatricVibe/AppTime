package com.example.apptime

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.reflect.KProperty

@Immutable
data class AppUsageModel(
    val appName: String,
    val packageName: String,
    val timeInMillis: Long,
    val icon: Drawable?,
    val isSystemApp: Boolean = false,
    val installTimeMillis: Long = 0L
)

@Immutable
data class AchievementItem(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val progress: Float,
    val progressText: String,
    val isUnlocked: Boolean,
    val isClaimed: Boolean
)

enum class ScreenTab(val title: String, val headerTitle: String, val index: Int) {
    TRACKER("Трекер", "Экранное время", 0),
    CUSTOM_PERIOD("Период", "С момента установки", 1),
    LEADERBOARD("Топ-50", "Рейтинг лидеров", 2),
    PROFILE("Профиль", "Профиль", 3),
    SETTINGS("Опции", "Настройки", 4)
}

enum class StatsPeriod(val title: String, val genitiveTitle: String, val maskBit: Int) {
    DAY("День", "день", 1),
    WEEK("Неделя", "неделю", 2),
    MONTH("Месяц", "месяц", 4)
}

enum class CustomPeriodPreset(val label: String) {
    SINCE_TRACKER_INSTALL("С момента скачивания"),
    CUSTOM_SLIDER("Свой интервал (Шкала)"),
    LAST_24H("Последние 24 часа"),
    LAST_3D("Последние 3 дня"),
    LAST_7D("Последние 7 дней"),
    SINCE_EACH_APP_INSTALL("От скачивания прил.")
}

enum class DesignStylePreset(val title: String, val badge: String, val subtitle: String, val defaultRadius: Float) {
    MATERIAL_YOU("Material You", "You", "Мягкая адаптивная пастельная палитра обоев Android 12+, плавные углы 24 dp", 24f),
    MATERIAL_YOU_3("Material You 3", "M3", "Классический стандарт Material Design 3, четкая иерархия и геометрия 18 dp", 18f),
    MATERIAL_EXPRESSIVE("Material You Expressive", "Expressive", "Выразительный стиль: глубокие тона, гипер-скругления 32 dp, жирный шрифт Black", 32f)
}

enum class SettingsSubTab(val title: String, val index: Int) {
    THEMES("Темы M3", 0), CUSTOMIZATION("Стиль и UI", 1), SYSTEM("Система", 2)
}

enum class ThemeMode(val label: String) { LIGHT("Светлая"), DARK("Темная"), AMOLED("AMOLED"), SYSTEM("Авто (ОС)") }
enum class CardSurfaceStyle(val label: String) { TONAL("Тональный M3"), GLASS("Стекло (Glass)"), GRADIENT("Градиент"), FLAT("Минимализм") }
enum class TimeFormatStyle(val label: String) { STANDARD("2ч 15м"), PRECISE("2ч 15м 30с"), MINUTES("135 мин"), DIGITAL("02:15") }
enum class TopBarStyle(val label: String) { LARGE("Крупная шапка"), COMPACT("Компактная") }
enum class NavAnimationType(val label: String) { SLIDE("Сдвиг"), FADE("Плавное"), NONE("Мгновенно") }
enum class NavLabelVisibility(val label: String) { ALWAYS("Всегда"), SELECTED_ONLY("Активная"), HIDE("Только иконки") }
enum class ListDensity(val label: String, val spacingDp: Int, val cardPaddingDp: Int) { COMPACT("Компактно", 10, 14), STANDARD("Стандартно", 15, 18), SPACIOUS("Просторно", 22, 22) }
enum class FontChoice(val label: String) { SANS("Sans-Serif"), SERIF("Serif"), MONO("Monospace"), CUSTOM("Свой .ttf") }
enum class AppIconShape(val label: String) { CIRCLE("Круглые"), ROUNDED("Скругленные"), SQUARE("Квадратные") }
enum class SortOption(val label: String) { TIME("По времени"), NAME("По алфавиту") }

val AccentPresets = listOf(Color(0xFF6750A4), Color(0xFF006C4C), Color(0xFFB3261E), Color(0xFF7D5700), Color(0xFF00639B), Color(0xFF984061), Color(0xFF006874), Color(0xFF535A92))

class PrefState<T>(initialValue: T, private val saver: (T) -> Unit) {
    private var state by mutableStateOf(initialValue)
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = state
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { if (state != value) { state = value; saver(value) } }
}

class UiSettings(val prefs: SharedPreferences, val supportsDynamic: Boolean) {
    private fun strPref(key: String, def: String) = PrefState(prefs.getString(key, def) ?: def) { prefs.edit().putString(key, it).apply() }
    private fun boolPref(key: String, def: Boolean) = PrefState(prefs.getBoolean(key, def)) { prefs.edit().putBoolean(key, it).apply() }
    private fun intPref(key: String, def: Int) = PrefState(prefs.getInt(key, def)) { prefs.edit().putInt(key, it).apply() }
    private fun floatPref(key: String, def: Float) = PrefState(prefs.getFloat(key, def)) { prefs.edit().putFloat(key, it).apply() }
    private fun <E : Enum<E>> enumPref(key: String, def: E, values: Array<E>): PrefState<E> {
        val savedName = prefs.getString(key, def.name) ?: def.name
        return PrefState(values.firstOrNull { it.name == savedName } ?: def) { prefs.edit().putString(key, it.name).apply() }
    }
    private fun loadLimits(): Map<String, Int> {
        val raw = prefs.getString("app_limits_serialized", "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull { item ->
            val p = item.split("=")
            val mins = p.getOrNull(1)?.toIntOrNull()
            if (p.size == 2 && p[0].isNotBlank() && mins != null && mins > 0) p[0] to mins else null
        }.toMap()
    }

    var nickname by strPref("player_name", "")
    var username by strPref("username", "")
    var userBio by strPref("user_bio", "Контролирую экранное время и держу цифровой баланс")
    var avatarPath by strPref("avatar_path", "")
    var dailyGoalHours by floatPref("daily_goal_hours", 4f)
    var designStyle by enumPref("design_style", DesignStylePreset.MATERIAL_EXPRESSIVE, DesignStylePreset.values())
    var dynamicColorEnabled by boolPref("dynamic_color", supportsDynamic)
    var accentIndex by intPref("accent_index", 0)
    var cornerRadius by floatPref("corner_radius", 28f)
    var themeMode by enumPref("theme_mode", ThemeMode.SYSTEM, ThemeMode.values())
    var cardSurfaceStyle by enumPref("card_surface_style", CardSurfaceStyle.TONAL, CardSurfaceStyle.values())
    var timeFormatStyle by enumPref("time_format_style", TimeFormatStyle.STANDARD, TimeFormatStyle.values())
    var topBarStyle by enumPref("top_bar_style", TopBarStyle.LARGE, TopBarStyle.values())
    var navLabelVisibility by enumPref("nav_label_vis", NavLabelVisibility.SELECTED_ONLY, NavLabelVisibility.values())
    var navBarWidthFraction by floatPref("nav_bar_width", 0.94f)
    var navBarBottomPaddingDp by floatPref("nav_bar_bottom_pad", 18f)
    var navAnimation by enumPref("nav_anim", NavAnimationType.SLIDE, NavAnimationType.values())
    var glassEffectEnabled by boolPref("glass_enabled", true)
    var cardElevationDp by floatPref("card_elevation", 0f)
    var appIconSizeDp by floatPref("app_icon_size", 48f)
    var listDensity by enumPref("list_density", ListDensity.STANDARD, ListDensity.values())
    var showCardBorder by boolPref("show_card_border", false)
    var showRankBadges by boolPref("show_rank_badges", true)
    var showSharePercent by boolPref("show_share_percent", true)
    var showPackageNames by boolPref("show_package_names", true)
    var hideSystemApps by boolPref("hide_system_apps", false)
    var fontChoice by enumPref("font_choice", FontChoice.SANS, FontChoice.values())
    var customFontPath by strPref("custom_font_path", "")
    var fontScale by floatPref("font_scale", 1.0f)
    var iconShape by enumPref("icon_shape", AppIconShape.ROUNDED, AppIconShape.values())
    var showWeeklyChart by boolPref("show_weekly_chart", true)
    var gradientChartBars by boolPref("gradient_chart_bars", true)
    var showChartValues by boolPref("show_chart_values", true)
    var showChartTrendLine by boolPref("show_chart_trend", true)
    var showChartAverageLine by boolPref("show_chart_avg", true)
    var showProgressBar by boolPref("show_progress_bar", true)
    var progressThickness by floatPref("progress_thickness", 10f)
    var roundedProgressBar by boolPref("rounded_progress_bar", true)
    var hapticEnabled by boolPref("haptic_enabled", true)
    var streakDays by intPref("streak_days", 1)
    var customizationChangesCount by intPref("custom_changes_count", 1)
    var appInspectCount by intPref("app_inspect_count", 0)
    var searchUsedCount by intPref("search_used_count", 0)
    var periodsMask by intPref("periods_mask", StatsPeriod.DAY.maskBit)
    var claimedAchievementIds by PrefState(prefs.getStringSet("claimed_achievements", emptySet())?.toSet() ?: emptySet()) { prefs.edit().putStringSet("claimed_achievements", it).apply() }
    var appLimitsMap by PrefState(loadLimits()) { map -> prefs.edit().putString("app_limits_serialized", map.entries.joinToString(";") { "${it.key}=${it.value}" }).apply() }

    fun recordStyleChange() { customizationChangesCount += 1 }
}