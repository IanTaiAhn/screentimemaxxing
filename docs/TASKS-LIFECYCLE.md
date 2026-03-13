# BrainRot RPG — Lifecycle & Archive Feature

A self-contained feature branch. Complete tasks in order — each step is independently testable before moving to the next.

---

## Overview

**What this feature adds:**

- A lifecycle system where each run ends at 160 hours (16,000 XP / Level 10)
- Three possible outcomes determined by the avatar's final usage stats: **Graduate**, **Die**, or **Ascend**
- A lifecycle end screen shown automatically when the threshold is hit
- A persistent archive of all past lives with full stats per lifecycle
- The archive is accessible as a new tab/screen from anywhere in the app
- On lifecycle end, everything resets — XP, hours, room objects, spendable hours — and the player manually starts a new life after viewing the end screen

**Outcome logic:**

| Outcome | Condition | Flavour |
|---------|-----------|---------|
| 🎓 Graduate | Dominant category is ENRICHMENT (>60%) | You consumed enough culture to escape |
| 💀 Die | Dominant category is BRAINROT (>60%) | The feed consumed you |
| ✨ Ascend | Dominant category is MID, or Hybrid (no dominant category) | You became one with the internet |

**Data flow:**
```
UsageTrackingWorker writes XP update
       ↓
LifecycleEngine checks if totalXp >= 16,000
       ↓
If threshold hit → resolve outcome → write LifecycleRecord → reset PlayerStats
       ↓
UI detects pending end-of-life state → shows LifecycleEndScreen
       ↓
Player taps "Begin New Life" → clears pending state → navigates to avatar screen
       ↓
Archive screen reads all LifecycleRecords from Room
```

---

## Phase 12: Data Layer

### Task 12.1 — Add `LifecycleOutcome` Enum

Create `app/src/main/java/com/brainrotrpg/LifecycleOutcome.kt`:

```kotlin
package com.brainrotrpg

enum class LifecycleOutcome(
    val displayName: String,
    val emoji: String,
    val description: String
) {
    GRADUATE(
        displayName = "Graduate",
        emoji = "🎓",
        description = "You consumed enough culture to escape the feed."
    ),
    DIE(
        displayName = "Die",
        emoji = "💀",
        description = "The brainrot consumed you. There was nothing left."
    ),
    ASCEND(
        displayName = "Ascend",
        emoji = "✨",
        description = "Extremely online. Deeply chronically. You became the internet."
    )
}

fun resolveLifecycleOutcome(
    brainrotHours: Float,
    midHours: Float,
    enrichmentHours: Float
): LifecycleOutcome {
    val total = brainrotHours + midHours + enrichmentHours
    if (total == 0f) return LifecycleOutcome.ASCEND

    val brainrotPct = brainrotHours / total
    val enrichmentPct = enrichmentHours / total

    return when {
        enrichmentPct > 0.6f -> LifecycleOutcome.GRADUATE
        brainrotPct > 0.6f -> LifecycleOutcome.DIE
        else -> LifecycleOutcome.ASCEND
    }
}
```

**Verification:** File compiles. `resolveLifecycleOutcome` returns the correct outcome for known inputs.

---

### Task 12.2 — Add `LifecycleRecord` Room Entity and DAO

**Create `app/src/main/java/com/brainrotrpg/LifecycleRecord.kt`:**

```kotlin
package com.brainrotrpg

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lifecycle_records")
data class LifecycleRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lifecycleNumber: Int,           // 1-indexed, increments with each new life
    val outcome: String,                // LifecycleOutcome.name
    val finalAvatarClass: String,       // AvatarState class name at time of end
    val totalXp: Long,
    val finalLevel: Int,
    val brainrotHours: Float,
    val midHours: Float,
    val enrichmentHours: Float,
    val totalHours: Float,              // brainrot + mid + enrichment
    val roomObjectsPlaced: Int,         // total objects placed during this life
    val startedAt: Long,                // timestamp when this life began
    val endedAt: Long                   // timestamp when this life ended
)

fun LifecycleRecord.outcome(): LifecycleOutcome = LifecycleOutcome.valueOf(outcome)
fun LifecycleRecord.avatarState(): AvatarState = when (finalAvatarClass) {
    "SigmaZombie" -> AvatarState.SigmaZombie
    "ExtremelyOnline" -> AvatarState.ExtremelyOnline
    "FakeIntellectual" -> AvatarState.FakeIntellectual
    else -> AvatarState.Hybrid
}

// Derived stat: dominant category as a readable label
fun LifecycleRecord.dominantCategory(): String {
    val total = totalHours
    if (total == 0f) return "None"
    return when {
        brainrotHours / total > 0.6f -> "Brainrot"
        midHours / total > 0.6f -> "Mid"
        enrichmentHours / total > 0.6f -> "Enrichment"
        else -> "Hybrid"
    }
}
```

**Create `app/src/main/java/com/brainrotrpg/LifecycleRecordDao.kt`:**

```kotlin
package com.brainrotrpg

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LifecycleRecordDao {

    @Insert
    suspend fun insert(record: LifecycleRecord): Long

    @Query("SELECT * FROM lifecycle_records ORDER BY lifecycleNumber DESC")
    fun observeAll(): Flow<List<LifecycleRecord>>

    @Query("SELECT * FROM lifecycle_records ORDER BY lifecycleNumber DESC")
    suspend fun getAll(): List<LifecycleRecord>

    @Query("SELECT COUNT(*) FROM lifecycle_records")
    suspend fun getCount(): Int

    @Query("SELECT * FROM lifecycle_records WHERE id = :id")
    suspend fun getById(id: Long): LifecycleRecord?
}
```

**Verification:** Files compile. DAO references the correct entity and returns the correct types.

---

### Task 12.3 — Migrate the Database

Update `AppDatabase.kt` to include the new entity and bump the version:

```kotlin
@Database(
    entities = [UsageRecord::class, PlayerStats::class, RoomObject::class, LifecycleRecord::class],
    version = 3,           // bumped from 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun roomObjectDao(): RoomObjectDao
    abstract fun lifecycleRecordDao(): LifecycleRecordDao
}
```

Add `MIGRATION_2_3` to `DatabaseProvider.kt`:

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS lifecycle_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                lifecycleNumber INTEGER NOT NULL,
                outcome TEXT NOT NULL,
                finalAvatarClass TEXT NOT NULL,
                totalXp INTEGER NOT NULL,
                finalLevel INTEGER NOT NULL,
                brainrotHours REAL NOT NULL,
                midHours REAL NOT NULL,
                enrichmentHours REAL NOT NULL,
                totalHours REAL NOT NULL,
                roomObjectsPlaced INTEGER NOT NULL,
                startedAt INTEGER NOT NULL,
                endedAt INTEGER NOT NULL
            )
        """)
        // Track when the current life started (default to 0 for existing installs)
        database.execSQL(
            "ALTER TABLE player_stats ADD COLUMN lifecycleNumber INTEGER NOT NULL DEFAULT 1"
        )
        database.execSQL(
            "ALTER TABLE player_stats ADD COLUMN lifecycleStartedAt INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE player_stats ADD COLUMN pendingLifecycleEnd INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

Register the migration in `DatabaseProvider`:

```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3)
```

**Verification:** App installs and launches without a Room schema mismatch crash. Existing player data is preserved.

---

### Task 12.4 — Update `PlayerStats` for Lifecycle Tracking

Add three new fields to `PlayerStats.kt`:

```kotlin
@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey
    val id: Int = 1,
    val totalXp: Long,
    val level: Int,
    val brainrotHours: Float,
    val midHours: Float,
    val enrichmentHours: Float,
    val spendableBrainrotHours: Float = 0f,
    val spendableMidHours: Float = 0f,
    val spendableEnrichmentHours: Float = 0f,
    val lastCheckedTimestamp: Long,
    val lifecycleNumber: Int = 1,           // NEW — which life this is
    val lifecycleStartedAt: Long = 0L,      // NEW — timestamp when this life began
    val pendingLifecycleEnd: Boolean = false // NEW — true when lifecycle has just ended, awaiting player acknowledgment
)
```

**Verification:** App compiles. Existing stats row is preserved with defaults.

---

### Task 12.5 — Create `LifecycleEngine`

Create `app/src/main/java/com/brainrotrpg/LifecycleEngine.kt`:

```kotlin
package com.brainrotrpg

object LifecycleEngine {

    private const val LIFECYCLE_XP_THRESHOLD = 16_000L

    fun isLifecycleComplete(totalXp: Long): Boolean = totalXp >= LIFECYCLE_XP_THRESHOLD

    /**
     * Builds a LifecycleRecord snapshot from the current PlayerStats.
     * [roomObjectsPlaced] must be passed in from a separate DB query.
     */
    fun buildRecord(
        stats: PlayerStats,
        roomObjectsPlaced: Int,
        endedAt: Long = System.currentTimeMillis()
    ): LifecycleRecord {
        val outcome = resolveLifecycleOutcome(
            stats.brainrotHours,
            stats.midHours,
            stats.enrichmentHours
        )
        val avatarState = resolveAvatarState(
            stats.brainrotHours,
            stats.midHours,
            stats.enrichmentHours
        )
        val avatarClassName = when (avatarState) {
            is AvatarState.SigmaZombie -> "SigmaZombie"
            is AvatarState.ExtremelyOnline -> "ExtremelyOnline"
            is AvatarState.FakeIntellectual -> "FakeIntellectual"
            is AvatarState.Hybrid -> "Hybrid"
        }
        return LifecycleRecord(
            lifecycleNumber = stats.lifecycleNumber,
            outcome = outcome.name,
            finalAvatarClass = avatarClassName,
            totalXp = stats.totalXp,
            finalLevel = stats.level,
            brainrotHours = stats.brainrotHours,
            midHours = stats.midHours,
            enrichmentHours = stats.enrichmentHours,
            totalHours = stats.brainrotHours + stats.midHours + stats.enrichmentHours,
            roomObjectsPlaced = roomObjectsPlaced,
            startedAt = stats.lifecycleStartedAt,
            endedAt = endedAt
        )
    }

    /**
     * Produces a fresh PlayerStats row for the next life.
     * Carries over only the lifecycle number (incremented) and a new start timestamp.
     */
    fun resetStats(currentStats: PlayerStats, now: Long = System.currentTimeMillis()): PlayerStats {
        return PlayerStats(
            id = 1,
            totalXp = 0L,
            level = 1,
            brainrotHours = 0f,
            midHours = 0f,
            enrichmentHours = 0f,
            spendableBrainrotHours = 0f,
            spendableMidHours = 0f,
            spendableEnrichmentHours = 0f,
            lastCheckedTimestamp = now,
            lifecycleNumber = currentStats.lifecycleNumber + 1,
            lifecycleStartedAt = now,
            pendingLifecycleEnd = false
        )
    }
}
```

**Verification:** Unit test `buildRecord` and `resetStats` with known inputs. Confirm lifecycle number increments correctly.

---

## Phase 13: Worker Integration

### Task 13.1 — Hook Lifecycle Detection into `UsageTrackingWorker`

After the XP update is calculated and before writing `updatedStats` to the DB, add a lifecycle check:

```kotlin
// After calculating newTotalXp and newLevel...

val now = System.currentTimeMillis()

if (LifecycleEngine.isLifecycleComplete(newTotalXp) && !(currentStats?.pendingLifecycleEnd ?: false)) {
    // Snapshot the completed lifecycle
    val roomObjectsPlaced = roomObjectDao.getAll().size
    val statsForRecord = PlayerStats(
        id = 1,
        totalXp = newTotalXp,
        level = newLevel,
        brainrotHours = newBrainrotHours,
        midHours = newMidHours,
        enrichmentHours = newEnrichmentHours,
        spendableBrainrotHours = newSpendableBrainrot,
        spendableMidHours = newSpendableMid,
        spendableEnrichmentHours = newSpendableEnrichment,
        lastCheckedTimestamp = now,
        lifecycleNumber = currentStats?.lifecycleNumber ?: 1,
        lifecycleStartedAt = currentStats?.lifecycleStartedAt ?: now,
        pendingLifecycleEnd = false
    )
    val record = LifecycleEngine.buildRecord(statsForRecord, roomObjectsPlaced, endedAt = now)
    db.lifecycleRecordDao().insert(record)

    // Mark pendingLifecycleEnd so the UI can show the end screen
    // Do NOT reset yet — reset happens when the player taps "Begin New Life"
    playerStatsDao.upsert(statsForRecord.copy(pendingLifecycleEnd = true))

    Log.d(TAG, "Lifecycle ${record.lifecycleNumber} complete. Outcome: ${record.outcome}")
    return Result.success()
}

// Normal write path (lifecycle not yet complete)
playerStatsDao.upsert(updatedStats)
```

**Verification:** With mock data, manually set XP to just below 16,000 and confirm the lifecycle record is written and `pendingLifecycleEnd` is set to true on the next worker run.

---

## Phase 14: Lifecycle End Screen

### Task 14.1 — Create `LifecycleEndScreen`

Create `app/src/main/java/com/brainrotrpg/LifecycleEndScreen.kt`:

```kotlin
package com.brainrotrpg

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LifecycleEndScreen(
    record: LifecycleRecord,
    onBeginNewLife: () -> Unit,
    onViewArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outcome = record.outcome()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = outcome.emoji,
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Life ${record.lifecycleNumber} — ${outcome.displayName}",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = outcome.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        // Stats summary card
        LifecycleStatsSummary(record = record)

        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onBeginNewLife,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Begin New Life")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onViewArchive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Archive")
        }
    }
}

@Composable
private fun LifecycleStatsSummary(
    record: LifecycleRecord,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Final Stats", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            StatLine("Avatar Class", record.avatarState().displayName())
            StatLine("Total Hours", "%.1f hrs".format(record.totalHours))
            StatLine("🧟 Brainrot", "%.1f hrs".format(record.brainrotHours))
            StatLine("😐 Mid", "%.1f hrs".format(record.midHours))
            StatLine("🎧 Enrichment", "%.1f hrs".format(record.enrichmentHours))
            StatLine("Objects Placed", "${record.roomObjectsPlaced}")
            StatLine("Final Level", "${record.finalLevel}")
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

// Helper to get a display name from AvatarState
private fun AvatarState.displayName(): String = when (this) {
    is AvatarState.SigmaZombie -> "Sigma Zombie"
    is AvatarState.ExtremelyOnline -> "Extremely Online"
    is AvatarState.FakeIntellectual -> "Fake Intellectual"
    is AvatarState.Hybrid -> "Hybrid"
}
```

**Verification:** Preview with a hardcoded `LifecycleRecord`. Confirm all three outcomes render correctly with the correct emoji and description.

---

### Task 14.2 — Wire Lifecycle End Screen into Navigation

The lifecycle end screen should intercept the normal flow when `pendingLifecycleEnd = true`.

Update `BrainRotNavHost` in `MainActivity.kt` to add two new destinations:

```kotlin
composable("lifecycle_end") {
    val db = DatabaseProvider.getDatabase(context)
    val lifecycleRecordDao = db.lifecycleRecordDao()
    val playerStatsDao = db.playerStatsDao()
    val viewModel: LifecycleViewModel = viewModel(
        factory = LifecycleViewModel.factory(lifecycleRecordDao, playerStatsDao)
    )
    val mostRecentRecord by viewModel.mostRecentRecord.collectAsStateWithLifecycle()
    mostRecentRecord?.let { record ->
        LifecycleEndScreen(
            record = record,
            onBeginNewLife = {
                viewModel.beginNewLife()
                navController.navigate("avatar") {
                    popUpTo("lifecycle_end") { inclusive = true }
                }
            },
            onViewArchive = {
                navController.navigate("archive")
            }
        )
    }
}

composable("archive") {
    val db = DatabaseProvider.getDatabase(context)
    val lifecycleRecordDao = db.lifecycleRecordDao()
    val viewModel: LifecycleViewModel = viewModel(
        factory = LifecycleViewModel.factory(lifecycleRecordDao, db.playerStatsDao())
    )
    ArchiveScreen(viewModel = viewModel)
}
```

Update `startDestination` logic to check for `pendingLifecycleEnd`:

```kotlin
val startDestination = when {
    !UsagePermissionHelper.hasUsagePermission(context) -> "permission"
    // Check pendingLifecycleEnd — read synchronously at startup via runBlocking or pass via ViewModel
    else -> "avatar"   // LifecycleViewModel handles redirect if pendingLifecycleEnd is true
}
```

The cleaner approach is to have `AvatarViewModel` (or a shared `AppViewModel`) observe `pendingLifecycleEnd` from the `PlayerStats` flow and emit a navigation event when it becomes true. The avatar screen then responds to this event and navigates to `lifecycle_end`.

**Verification:** With `pendingLifecycleEnd = true` injected directly into the DB, confirm the app navigates to the end screen on launch rather than the avatar screen.

---

## Phase 15: Archive Screen

### Task 15.1 — Create `LifecycleViewModel`

Create `app/src/main/java/com/brainrotrpg/LifecycleViewModel.kt`:

```kotlin
package com.brainrotrpg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LifecycleViewModel(
    private val lifecycleRecordDao: LifecycleRecordDao,
    private val playerStatsDao: PlayerStatsDao
) : ViewModel() {

    val allRecords: StateFlow<List<LifecycleRecord>> = lifecycleRecordDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val mostRecentRecord: StateFlow<LifecycleRecord?> = lifecycleRecordDao.observeAll()
        .map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Called when the player taps "Begin New Life". Resets PlayerStats and clears all room objects. */
    fun beginNewLife() {
        viewModelScope.launch {
            val current = playerStatsDao.getStats() ?: return@launch
            val resetStats = LifecycleEngine.resetStats(current)
            playerStatsDao.upsert(resetStats)
            // Room objects are cleared — they belong to the old life
            // roomObjectDao.deleteAll() — add this query to RoomObjectDao
        }
    }

    companion object {
        fun factory(
            lifecycleRecordDao: LifecycleRecordDao,
            playerStatsDao: PlayerStatsDao
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LifecycleViewModel(lifecycleRecordDao, playerStatsDao)
            }
        }
    }
}
```

Add a `deleteAll` query to `RoomObjectDao`:

```kotlin
@Query("DELETE FROM room_objects")
suspend fun deleteAll()
```

---

### Task 15.2 — Create `ArchiveScreen`

Create `app/src/main/java/com/brainrotrpg/ArchiveScreen.kt`:

```kotlin
package com.brainrotrpg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ArchiveScreen(
    viewModel: LifecycleViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.allRecords.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Archive",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No lives completed yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records) { record ->
                    ArchiveCard(record = record)
                }
            }
        }
    }
}

@Composable
private fun ArchiveCard(
    record: LifecycleRecord,
    modifier: Modifier = Modifier
) {
    val outcome = record.outcome()
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${outcome.emoji} Life ${record.lifecycleNumber}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = outcome.displayName,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = record.dominantCategory() + " · " +
                       "%.0f hrs total".format(record.totalHours) + " · " +
                       "Level ${record.finalLevel}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CategoryPill("🧟 %.0fh".format(record.brainrotHours))
                CategoryPill("😐 %.0fh".format(record.midHours))
                CategoryPill("🎧 %.0fh".format(record.enrichmentHours))
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
```

**Verification:** With two or three hardcoded `LifecycleRecord` objects, confirm the archive renders correctly. Confirm empty state shows when the list is empty.

---

### Task 15.3 — Add Archive Entry Point to Avatar Screen

Add an archive icon button to the `AvatarScreen` top bar or alongside the shop FAB so players can reach it at any time:

```kotlin
// Add a second FAB or an icon in the top area
FloatingActionButton(
    onClick = onArchiveClick,
    modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(16.dp)
) {
    Text("📜")
}
```

Pass `onArchiveClick` up from `BrainRotNavHost` and wire it to `navController.navigate("archive")`.

**Verification:** Tapping the archive button from the avatar screen navigates to the archive. Back navigation returns to the avatar screen.

---

## Phase 16: Polish & Testing

### Task 16.1 — Add Strings to `strings.xml`

```xml
<string name="lifecycle_end_title">Life %d — %s</string>
<string name="lifecycle_begin_new">Begin New Life</string>
<string name="lifecycle_view_archive">View Archive</string>
<string name="archive_title">Archive</string>
<string name="archive_empty">No lives completed yet.</string>
<string name="outcome_graduate_name">Graduate</string>
<string name="outcome_die_name">Die</string>
<string name="outcome_ascend_name">Ascend</string>
<string name="outcome_graduate_desc">You consumed enough culture to escape the feed.</string>
<string name="outcome_die_desc">The brainrot consumed you. There was nothing left.</string>
<string name="outcome_ascend_desc">Extremely online. Deeply chronically. You became the internet.</string>
<string name="lifecycle_stat_total_hours">Total Hours</string>
<string name="lifecycle_stat_avatar_class">Avatar Class</string>
<string name="lifecycle_stat_objects_placed">Objects Placed</string>
```

---

### Task 16.2 — Unit Tests for New Logic

Add tests for:

- `resolveLifecycleOutcome()` — all three outcome paths, edge cases (zero hours, exactly 60%)
- `LifecycleEngine.isLifecycleComplete()` — boundary values (15,999 XP, 16,000 XP, 16,001 XP)
- `LifecycleEngine.buildRecord()` — confirm all fields are populated correctly from `PlayerStats`
- `LifecycleEngine.resetStats()` — confirm XP/hours/objects are zeroed, lifecycle number increments, `pendingLifecycleEnd` is false
- `LifecycleRecord.dominantCategory()` — all four category combinations

---

### Task 16.3 — End-to-End Test

With mock data enabled:

1. Set mock data to generate enough hours to trigger lifecycle completion (>160h equivalent)
2. Run the worker — confirm `LifecycleRecord` is written to DB with correct outcome
3. Confirm `pendingLifecycleEnd = true` in `PlayerStats`
4. Relaunch the app — confirm the lifecycle end screen is shown (not the avatar screen)
5. Confirm stats summary on the end screen matches the written `LifecycleRecord`
6. Tap "Begin New Life" — confirm `PlayerStats` is reset, room objects are cleared
7. Confirm the new life starts at Level 1 with 0 hours and lifecycle number incremented
8. Navigate to the archive — confirm the completed life appears as a card
9. Complete a second lifecycle — confirm both lives appear in the archive in reverse order

---

## Feature Complete Checklist

Before merging this feature, confirm:

- [ ] `LifecycleOutcome` enum compiles with all three outcomes
- [ ] `resolveLifecycleOutcome` returns the correct outcome for all usage distributions
- [ ] `LifecycleRecord` entity persists correctly to Room DB
- [ ] Database migration runs without crashing on existing installs
- [ ] `UsageTrackingWorker` detects lifecycle completion and writes the record exactly once
- [ ] `pendingLifecycleEnd` is set to true after the record is written
- [ ] Worker does not re-trigger on subsequent runs when lifecycle is already pending
- [ ] Lifecycle end screen shows with the correct outcome, emoji, and stats
- [ ] "Begin New Life" resets all stats and room objects, increments lifecycle number
- [ ] Archive screen shows all past lives in reverse chronological order
- [ ] Archive is accessible from the avatar screen at any time
- [ ] Empty archive state renders correctly
- [ ] All new strings are in `strings.xml`
- [ ] All unit tests pass
