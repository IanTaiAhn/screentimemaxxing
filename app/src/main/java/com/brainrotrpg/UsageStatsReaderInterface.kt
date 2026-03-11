package com.brainrotrpg

import android.content.Context

interface IUsageStatsReader {
    fun getUsageSince(context: Context, sinceMillis: Long): Map<String, Long>
    fun getCategorizedUsage(context: Context, sinceMillis: Long): Map<Category, Long>
}
