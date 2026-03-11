package com.brainrotrpg

object UsageStatsReaderProvider {
    val instance: IUsageStatsReader
        get() = if (BuildConfig.USE_MOCK_DATA) MockUsageStatsReader else UsageStatsReader
}
