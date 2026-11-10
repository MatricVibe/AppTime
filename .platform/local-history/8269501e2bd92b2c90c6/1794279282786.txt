package com.example.apptime

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import java.util.Calendar

class UsageStatsHelper(private val context: Context) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    fun hasUsagePermission(): Boolean {
        val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTrackerInstallTime(): Long {
        return try { packageManager.getPackageInfo(context.packageName, 0).firstInstallTime } catch (e: Exception) { System.currentTimeMillis() }
    }

    fun clearCache() { }

    fun getTodayUsageMinutes(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        if (stats.isNullOrEmpty()) return 0L
        var totalTime = 0L
        val aggregatedStats = mutableMapOf<String, Long>()
        for (stat in stats) { if (stat.totalTimeInForeground > 0) aggregatedStats[stat.packageName] = aggregatedStats.getOrDefault(stat.packageName, 0L) + stat.totalTimeInForeground }
        for (time in aggregatedStats.values) totalTime += time
        return totalTime / 60_000L
    }

    fun getUsageForPeriod(period: StatsPeriod, hideSystem: Boolean): List<AppUsageModel> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        when (period) {
            StatsPeriod.DAY -> { calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0) }
            StatsPeriod.WEEK -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            StatsPeriod.MONTH -> calendar.add(Calendar.MONTH, -1)
        }
        return aggregateStats(calendar.timeInMillis, endTime, hideSystem)
    }

    fun getPreviousPeriodTotalTime(period: StatsPeriod): Long {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        when (period) {
            StatsPeriod.DAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, endTime)?.sumOf { it.totalTimeInForeground } ?: 0L
            }
            StatsPeriod.WEEK -> calendar.add(Calendar.DAY_OF_YEAR, -14)
            StatsPeriod.MONTH -> calendar.add(Calendar.MONTH, -2)
        }
        return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, calendar.timeInMillis, endTime)?.sumOf { it.totalTimeInForeground } ?: 0L
    }

    fun getWeeklyDailyBreakdown(): List<Long> {
        val result = mutableListOf<Long>()
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            dayCal.set(Calendar.HOUR_OF_DAY, 0); dayCal.set(Calendar.MINUTE, 0); dayCal.set(Calendar.SECOND, 0)
            val start = dayCal.timeInMillis
            dayCal.set(Calendar.HOUR_OF_DAY, 23); dayCal.set(Calendar.MINUTE, 59); dayCal.set(Calendar.SECOND, 59)
            val end = dayCal.timeInMillis
            result.add(usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)?.sumOf { it.totalTimeInForeground } ?: 0L)
        }
        return result
    }

    fun getUsageForCustomRange(startTime: Long, endTime: Long, hideSystemAndSelf: Boolean, perAppInstallTime: Boolean): List<AppUsageModel> {
        return aggregateStats(startTime, endTime, hideSystemAndSelf)
    }

    private fun aggregateStats(startTime: Long, endTime: Long, hideSystem: Boolean): List<AppUsageModel> {
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime) ?: return emptyList()
        val aggregated = mutableMapOf<String, Long>()
        for (stat in stats) { if (stat.totalTimeInForeground > 0) aggregated[stat.packageName] = aggregated.getOrDefault(stat.packageName, 0L) + stat.totalTimeInForeground }
        val result = mutableListOf<AppUsageModel>()
        for ((packageName, timeMillis) in aggregated) {
            if (packageName == context.packageName) continue
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                if (hideSystem && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
                result.add(AppUsageModel(packageManager.getApplicationLabel(appInfo).toString(), packageName, timeMillis, packageManager.getApplicationIcon(appInfo), (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0, packageManager.getPackageInfo(packageName, 0).firstInstallTime))
            } catch (e: Exception) { }
        }
        return result
    }
}