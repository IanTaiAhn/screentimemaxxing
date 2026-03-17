# BrainRot RPG — Room Objects & XP Multiplier System

A self-contained feature branch. Complete these tasks in order — each step is independently testable before moving to the next. Do not skip tasks; later steps depend on earlier ones.

---

## Overview

**What this feature adds:**

- Spendable hour currency accumulated from real app usage
- Placeable room objects (weights, bookshelf, pizza tower, etc.) bought with category-specific hours
- Each object grants XP multipliers when **active**
- Objects are **dormant by default** — the player must walk up and tap them to activate a 4-hour boost window
- Once the window expires the object goes dormant again, incentivizing the player to re-open the app

**Data flow:**
```
Real usage tracked → spendable hours accumulate
       ↓
Player spends hours in shop → RoomObject placed in world
       ↓
Player taps object in room → object activates (4hr window)
       ↓
UsageTrackingWorker checks active objects → applies XP multipliers
       ↓
Multiplied XP written to PlayerStats
```

---

## Phase 8: Data Layer

### Task 8.1 — Add `RoomObjectType` Enum

Create `app/src/main/java/com/brainrotrpg/RoomObjectType.kt`.

Each entry encodes its own cost and multiplier effect — no external lookup table needed.

```kotlin
package com.brainrotrpg

data class XpMultipliers(
    val brainrot: Float = 1f,
    val mid: Float = 1f,
    val enrichment: Float = 1f
)

enum class RoomObjectType(
    val displayName: String,
    val emoji: String,
    val costCategory: Category,
    val costHours: Float,
    val multipliers: XpMultipliers,
    val description: String
) {
    WEIGHTS(
        displayName = "Weights",
        emoji = "🏋️",
        costCategory = Category.ENRICHMENT,
        costHours = 2f,
        multipliers = XpMultipliers(brainrot = 1f, mid = 1f, enrichment = 1.15f),
        description = "+15% all XP while active"
    ),
    BOOKSHELF(
        displayName = "Bookshelf",
        emoji = "📚",
        costCategory = Category.ENRICHMENT,
        costHours = 3f,
        multipliers = XpMultipliers(brainrot = 1f, mid = 1f, enrichment = 1.25f),
        description = "+25% Enrichment XP while active"
    ),
    SECOND_MONITOR(
        displayName = "Second Monitor",
        emoji = "🖥️",
        costCategory = Category.MID,
        costHours = 2f,
        multipliers = XpMultipliers(brainrot = 1f, mid = 1.20f, enrichment = 1f),
        description = "+20% Mid XP while active"
    ),
    PIZZA_TOWER(
        displayName = "Pizza Tower",
        emoji = "🍕",
        costCategory = Category.BRAINROT,
        costHours = 2f,
        multipliers = XpMultipliers(brainrot = 1.20f, mid = 1f, enrichment = 1f),
        description = "+20% Brainrot XP while active"
    ),
    ENERGY_FRIDGE(
        displayName = "Energy Fridge",
        emoji = "⚡",
        costCategory = Category.BRAINROT,
        costHours = 3f,
        multipliers = XpMultipliers(brainrot = 1.15f, mid = 1.15f, enrichment = 0.90f),
        description = "+15% Brainrot & Mid XP, -10% Enrichment while active"
    ),
    HEADPHONE_STAND(
        displayName = "Headphone Stand",
        emoji = "🎧",
        costCategory = Category.ENRICHMENT,
        costHours = 2f,
        multipliers = XpMultipliers(brainrot = 1f, mid = 1f, enrichment = 1.20f),
        description = "+20% Enrichment XP while active"
    )
}
```

**Verification:** File compiles with no errors. No other files change yet.

---

### Task 8.2 — Add `RoomObject` Room Entity and DAO

**Create `app/src/main/java/com/brainrotrpg/RoomObject.kt`:**

```kotlin
package com.brainrotrpg

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "room_objects")
data class RoomObject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,               // RoomObjectType.name — stored as string for Room compatibility
    val worldX: Float,              // normalised world position (0f..1f)
    val worldY: Float,
    val isActive: Boolean = false,
    val activatedAt: Long = 0L,     // timestamp of last tap activation
    val activeDurationMs: Long = 4 * 60 * 60 * 1000L  // 4 hours default
)

fun RoomObject.objectType(): RoomObjectType = RoomObjectType.valueOf(type)

fun RoomObject.isCurrentlyActive(now: Long = System.currentTimeMillis()): Boolean {
    return isActive && (activatedAt + activeDurationMs) > now
}

fun RoomObject.timeRemainingMs(now: Long = System.currentTimeMillis()): Long {
    return if (isCurrentlyActive(now)) (activatedAt + activeDurationMs) - now else 0L
}
```

**Create `app/src/main/java/com/brainrotrpg/RoomObjectDao.kt`:**

```kotlin
package com.brainrotrpg

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomObjectDao {

    @Insert
    suspend fun insert(obj: RoomObject): Long

    @Update
    suspend fun update(obj: RoomObject)

    @Delete
    suspend fun delete(obj: RoomObject)

    @Query("SELECT * FROM room_objects")
    fun observeAll(): Flow<List<RoomObject>>

    @Query("SELECT * FROM room_objects")
    suspend fun getAll(): List<RoomObject>

    @Query("SELECT * FROM room_objects WHERE id = :id")
    suspend fun getById(id: Long): RoomObject?

    // Returns all objects where the active window has not yet expired
    @Query("SELECT * FROM room_objects WHERE isActive = 1 AND (activatedAt + activeDurationMs) > :now")
    suspend fun getActiveObjects(now: Long = System.currentTimeMillis()): List<RoomObject>
}
```

**Verification:** Files compile. DAO and entity reference `RoomObjectType` correctly via the helper extension.

---

### Task 8.3 — Migrate the Database

Update `AppDatabase.kt` to include the new entity and bump the version:

```kotlin
@Database(
    entities = [UsageRecord::class, PlayerStats::class, RoomObject::class],
    version = 2,           // bumped from 1
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun roomObjectDao(): RoomObjectDao
}
```

Update `DatabaseProvider.kt` to add a migration (or use destructive migration during development):

```kotlin
Room.databaseBuilder(
    context.applicationContext,
    AppDatabase::class.java,
    DATABASE_NAME
)
.addMigrations(MIGRATION_1_2)   // use this for production
// .fallbackToDestructiveMigration()  // use this during development only
.build()

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS room_objects (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                worldX REAL NOT NULL,
                worldY REAL NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 0,
                activatedAt INTEGER NOT NULL DEFAULT 0,
                activeDurationMs INTEGER NOT NULL DEFAULT 14400000
            )
        """)
    }
}
```

**Verification:** App installs and launches without a Room schema mismatch crash.

---

### Task 8.4 — Add Spendable Hours to `PlayerStats`

The existing `brainrotHours`, `midHours`, `enrichmentHours` fields are cumulative totals used for avatar class resolution and should not be touched. Add three new **spendable** fields alongside them.

Update `PlayerStats.kt`:

```kotlin
@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey
    val id: Int = 1,
    val totalXp: Long,
    val level: Int,
    val brainrotHours: Float,       // cumulative — used for avatar class
    val midHours: Float,
    val enrichmentHours: Float,
    val spendableBrainrotHours: Float = 0f,   // NEW — spendable currency
    val spendableMidHours: Float = 0f,         // NEW
    val spendableEnrichmentHours: Float = 0f,  // NEW
    val lastCheckedTimestamp: Long
)
```

Add a migration column addition to `MIGRATION_1_2` (or create `MIGRATION_2_3` if 8.3 is already deployed):

```sql
ALTER TABLE player_stats ADD COLUMN spendableBrainrotHours REAL NOT NULL DEFAULT 0;
ALTER TABLE player_stats ADD COLUMN spendableMidHours REAL NOT NULL DEFAULT 0;
ALTER TABLE player_stats ADD COLUMN spendableEnrichmentHours REAL NOT NULL DEFAULT 0;
```

**Verification:** App launches. Existing player data is preserved. New columns default to 0.

---

### Task 8.5 — Update `UsageTrackingWorker` to Accumulate Spendable Hours and Apply Multipliers

This is the core logic change. The worker now does two new things on each run:

1. Adds delta hours to *both* cumulative and spendable hour pools
2. Queries active room objects and applies their multipliers to XP calculation

Update the relevant section of `UsageTrackingWorker.kt`:

```kotlin
// After calculating delta hours (existing code)...

val newBrainrotHours = (currentStats?.brainrotHours ?: 0f) + brainrotDeltaHours
val newMidHours = (currentStats?.midHours ?: 0f) + midDeltaHours
val newEnrichmentHours = (currentStats?.enrichmentHours ?: 0f) + enrichmentDeltaHours

// NEW: accumulate spendable hours
val newSpendableBrainrot = (currentStats?.spendableBrainrotHours ?: 0f) + brainrotDeltaHours
val newSpendableMid = (currentStats?.spendableMidHours ?: 0f) + midDeltaHours
val newSpendableEnrichment = (currentStats?.spendableEnrichmentHours ?: 0f) + enrichmentDeltaHours

// NEW: fetch active room objects and aggregate multipliers
val roomObjectDao = db.roomObjectDao()
val now = System.currentTimeMillis()
val activeObjects = roomObjectDao.getActiveObjects(now)

// Expire any objects whose window has closed since last check
val allObjects = roomObjectDao.getAll()
for (obj in allObjects) {
    if (obj.isActive && !obj.isCurrentlyActive(now)) {
        roomObjectDao.update(obj.copy(isActive = false))
    }
}

// Aggregate multipliers (multiplicative stacking)
var brainrotMultiplier = 1f
var midMultiplier = 1f
var enrichmentMultiplier = 1f
for (obj in activeObjects) {
    val m = obj.objectType().multipliers
    brainrotMultiplier *= m.brainrot
    midMultiplier *= m.mid
    enrichmentMultiplier *= m.enrichment
}

// Apply multipliers to XP calculation
val newTotalXp = XpEngine.calculateXpWithMultipliers(
    brainrotHours = newBrainrotHours,
    midHours = newMidHours,
    enrichmentHours = newEnrichmentHours,
    brainrotMultiplier = brainrotMultiplier,
    midMultiplier = midMultiplier,
    enrichmentMultiplier = enrichmentMultiplier
)
```

Update `XpEngine.kt` to add the multiplier-aware function:

```kotlin
fun calculateXpWithMultipliers(
    brainrotHours: Float,
    midHours: Float,
    enrichmentHours: Float,
    brainrotMultiplier: Float = 1f,
    midMultiplier: Float = 1f,
    enrichmentMultiplier: Float = 1f
): Long {
    val xp = (brainrotHours * XP_PER_HOUR * brainrotMultiplier) +
              (midHours * XP_PER_HOUR * midMultiplier) +
              (enrichmentHours * XP_PER_HOUR * enrichmentMultiplier)
    return xp.toLong()
}
```

**Verification:** Run with mock data. Log the multiplier values. Confirm XP increases when a manually-inserted active object exists in the DB.

---

## Phase 9: Room Scene Interaction

### Task 9.1 — Add Proximity Hit Detection to `RoomScene`

The tap handler currently resolves a tap to either a floor walk target or nothing. Add a third case: tapping near a placed object triggers interaction.

Add a helper function to `RoomScene.kt`:

```kotlin
/**
 * Returns the closest RoomObject to the tap within [thresholdWorld] world units,
 * or null if none are close enough.
 */
private fun findTappedObject(
    tapWx: Float,
    tapWy: Float,
    placedObjects: List<RoomObject>,
    thresholdWorld: Float = 0.08f
): RoomObject? {
    return placedObjects
        .map { obj ->
            val dx = obj.worldX - tapWx
            val dy = obj.worldY - tapWy
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            obj to dist
        }
        .filter { (_, dist) -> dist < thresholdWorld }
        .minByOrNull { (_, dist) -> dist }
        ?.first
}
```

Update `RoomScene`'s signature to accept placed objects and an interaction callback:

```kotlin
@Composable
fun RoomScene(
    brainrotHours: Float,
    midHours: Float,
    enrichmentHours: Float,
    placedObjects: List<RoomObject> = emptyList(),          // NEW
    onObjectTapped: (RoomObject) -> Unit = {},              // NEW
    modifier: Modifier = Modifier,
    characterState: CharacterState = remember { CharacterState() }
)
```

Update the tap handler inside `pointerInput`:

```kotlin
.pointerInput(placedObjects) {   // key on placedObjects so handler refreshes when list changes
    detectTapGestures { tapOffset ->
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        val world = screenToWorld(tapOffset.x, tapOffset.y, w, h) ?: return@detectTapGestures

        val tappedObject = findTappedObject(world.first, world.second, placedObjects)
        if (tappedObject != null) {
            onObjectTapped(tappedObject)   // interaction — don't walk
        } else {
            characterState.setTarget(world.first, world.second)   // walk
        }
    }
}
```

**Verification:** With a hardcoded test object in `placedObjects`, tapping near it fires the callback. Tapping elsewhere still walks.

---

### Task 9.2 — Draw Placed Objects in the Room Scene

Add a draw function for placed objects. These render on the floor at their world position, with a visual indicator showing active vs. dormant state.

Add to `RoomScene.kt`:

```kotlin
private fun DrawScope.drawPlacedObjects(
    w: Float,
    h: Float,
    objects: List<RoomObject>,
    now: Long = System.currentTimeMillis()
) {
    for (obj in objects) {
        val screenPos = worldToScreen(obj.worldX, obj.worldY, w, h)
        val isActive = obj.isCurrentlyActive(now)
        val size = w * 0.045f

        // Base: isometric box in object's category color
        val baseColor = when (obj.objectType().costCategory) {
            Category.BRAINROT -> Color(0xFFCC2200)
            Category.ENRICHMENT -> Color(0xFF2266CC)
            Category.MID -> Color(0xFF885500)
            Category.UNTRACKED -> Color(0xFF666666)
        }

        drawIsometricBox(
            centerX = screenPos.x,
            centerY = screenPos.y,
            boxW = size,
            boxH = size,
            depth = size * 0.7f,
            topColor = if (isActive) baseColor else baseColor.copy(alpha = 0.45f),
            sideColor = if (isActive) baseColor.copy(alpha = 0.7f) else baseColor.copy(alpha = 0.25f)
        )

        // Active glow ring
        if (isActive) {
            drawCircle(
                color = baseColor.copy(alpha = 0.30f),
                radius = size * 1.4f,
                center = screenPos
            )
        }

        // Dormant indicator: small grey dot on top
        if (!isActive) {
            drawCircle(
                color = Color(0x88AAAAAA),
                radius = size * 0.25f,
                center = Offset(screenPos.x, screenPos.y - size * 0.6f)
            )
        }
    }
}
```

Call it inside the `Canvas` draw block, after `drawCategoryItems` and before drawing the character (painter's order matters for isometric depth):

```kotlin
drawPlacedObjects(w, h, placedObjects)
```

**Verification:** Place a test object via the DB directly. Confirm it renders at the correct world position with correct active/dormant visual.

---

### Task 9.3 — Activation Logic in `RoomObjectViewModel`

Create `app/src/main/java/com/brainrotrpg/RoomObjectViewModel.kt`:

```kotlin
package com.brainrotrpg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomObjectViewModel(
    private val roomObjectDao: RoomObjectDao,
    private val playerStatsDao: PlayerStatsDao
) : ViewModel() {

    val placedObjects: StateFlow<List<RoomObject>> = roomObjectDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Called when the player taps a placed object in the room. */
    fun activateObject(obj: RoomObject) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (!obj.isCurrentlyActive(now)) {
                roomObjectDao.update(
                    obj.copy(
                        isActive = true,
                        activatedAt = now
                    )
                )
            }
            // If already active — tapping again could show info toast, or reset the timer.
            // For MVP: do nothing if already active (tap is a no-op).
        }
    }

    /** Purchase and place an object. Deducts hours from PlayerStats. */
    fun purchaseAndPlace(type: RoomObjectType, worldX: Float, worldY: Float) {
        viewModelScope.launch {
            val stats = playerStatsDao.getStats() ?: return@launch

            // Check the player can afford it
            val canAfford = when (type.costCategory) {
                Category.BRAINROT -> stats.spendableBrainrotHours >= type.costHours
                Category.MID -> stats.spendableMidHours >= type.costHours
                Category.ENRICHMENT -> stats.spendableEnrichmentHours >= type.costHours
                Category.UNTRACKED -> false
            }
            if (!canAfford) return@launch

            // Deduct hours
            val updatedStats = when (type.costCategory) {
                Category.BRAINROT -> stats.copy(
                    spendableBrainrotHours = stats.spendableBrainrotHours - type.costHours
                )
                Category.MID -> stats.copy(
                    spendableMidHours = stats.spendableMidHours - type.costHours
                )
                Category.ENRICHMENT -> stats.copy(
                    spendableEnrichmentHours = stats.spendableEnrichmentHours - type.costHours
                )
                Category.UNTRACKED -> stats
            }
            playerStatsDao.upsert(updatedStats)

            // Place the object
            roomObjectDao.insert(
                RoomObject(
                    type = type.name,
                    worldX = worldX,
                    worldY = worldY
                )
            )
        }
    }

    /** Remove a placed object and refund half its cost. */
    fun removeObject(obj: RoomObject) {
        viewModelScope.launch {
            val stats = playerStatsDao.getStats() ?: return@launch
            val type = obj.objectType()
            val refund = type.costHours * 0.5f

            val updatedStats = when (type.costCategory) {
                Category.BRAINROT -> stats.copy(
                    spendableBrainrotHours = stats.spendableBrainrotHours + refund
                )
                Category.MID -> stats.copy(
                    spendableMidHours = stats.spendableMidHours + refund
                )
                Category.ENRICHMENT -> stats.copy(
                    spendableEnrichmentHours = stats.spendableEnrichmentHours + refund
                )
                Category.UNTRACKED -> stats
            }
            playerStatsDao.upsert(updatedStats)
            roomObjectDao.delete(obj)
        }
    }

    companion object {
        fun factory(
            roomObjectDao: RoomObjectDao,
            playerStatsDao: PlayerStatsDao
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RoomObjectViewModel(roomObjectDao, playerStatsDao)
            }
        }
    }
}
```

**Verification:** Unit test `activateObject` — confirm `isActive` and `activatedAt` are set correctly. Confirm `purchaseAndPlace` correctly deducts hours and returns early if unaffordable.

---

## Phase 10: Shop UI

### Task 10.1 — Shop Bottom Sheet

Create `app/src/main/java/com/brainrotrpg/ShopSheet.kt`:

A `ModalBottomSheet` listing all `RoomObjectType` entries. Each row shows the object name, cost, multiplier description, and an affordability-aware "Place" button.

```kotlin
package com.brainrotrpg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSheet(
    spendableBrainrotHours: Float,
    spendableMidHours: Float,
    spendableEnrichmentHours: Float,
    onDismiss: () -> Unit,
    onSelectObject: (RoomObjectType) -> Unit   // enters placement mode for this type
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Shop", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "🧟 ${spendableBrainrotHours.format(1)} hrs  " +
                       "😐 ${spendableMidHours.format(1)} hrs  " +
                       "🎧 ${spendableEnrichmentHours.format(1)} hrs",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(RoomObjectType.entries) { type ->
                    ShopRow(
                        type = type,
                        canAfford = when (type.costCategory) {
                            Category.BRAINROT -> spendableBrainrotHours >= type.costHours
                            Category.MID -> spendableMidHours >= type.costHours
                            Category.ENRICHMENT -> spendableEnrichmentHours >= type.costHours
                            Category.UNTRACKED -> false
                        },
                        onPlace = {
                            onSelectObject(type)
                            onDismiss()
                        }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ShopRow(
    type: RoomObjectType,
    canAfford: Boolean,
    onPlace: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${type.emoji} ${type.displayName}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = type.description,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Cost: ${type.costHours}h ${type.costCategory.name.lowercase()} time",
                style = MaterialTheme.typography.bodySmall,
                color = if (canAfford)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = onPlace,
            enabled = canAfford
        ) {
            Text("Place")
        }
    }
}

// Extension to format Float to N decimal places
private fun Float.format(decimals: Int) = "%.${decimals}f".format(this)
```

**Verification:** Open the sheet with hardcoded spendable hour values. Confirm affordability coloring works correctly. Confirm "Place" button is disabled when unaffordable.

---

### Task 10.2 — Placement Mode

When the player selects an object from the shop, the room enters **placement mode** — the next floor tap places the object at that position rather than walking.

Add state to `AvatarScreen` (or a shared VM):

```kotlin
// In AvatarScreen or a shared screen-level composable
var placementTarget: RoomObjectType? by remember { mutableStateOf(null) }
var showShop by remember { mutableStateOf(false) }
```

Pass a modified `onObjectTapped`/floor tap handler to `RoomScene` that checks placement mode:

```kotlin
RoomScene(
    brainrotHours = uiState.brainrotHours,
    midHours = uiState.midHours,
    enrichmentHours = uiState.enrichmentHours,
    placedObjects = placedObjects,
    onFloorTapped = { wx, wy ->
        val target = placementTarget
        if (target != null) {
            // Place the object here
            roomObjectViewModel.purchaseAndPlace(target, wx, wy)
            placementTarget = null   // exit placement mode
        } else {
            characterState.setTarget(wx, wy)   // normal walk
        }
    },
    onObjectTapped = { obj ->
        roomObjectViewModel.activateObject(obj)
    }
)
```

Add a placement mode banner above the room so the player knows they're placing:

```kotlin
if (placementTarget != null) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Tap the floor to place ${placementTarget!!.emoji} ${placementTarget!!.displayName}",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```

**Verification:** Select an object from the shop → banner appears → tap the floor → object appears in room at tap position → banner disappears.

---

### Task 10.3 — Shop Entry Point & Object Info Tooltip

Add a shop button to `AvatarScreen` (a FAB or a simple icon button in the top bar):

```kotlin
FloatingActionButton(
    onClick = { showShop = true },
    modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
) {
    Text("🛒")
}
```

Add a simple tooltip/dialog when an **already-active** object is tapped — showing the object name, effect, and time remaining:

```kotlin
var inspectedObject: RoomObject? by remember { mutableStateOf(null) }

// In onObjectTapped:
onObjectTapped = { obj ->
    if (obj.isCurrentlyActive()) {
        inspectedObject = obj   // show info
    } else {
        roomObjectViewModel.activateObject(obj)
    }
}

// Dialog:
inspectedObject?.let { obj ->
    AlertDialog(
        onDismissRequest = { inspectedObject = null },
        title = { Text("${obj.objectType().emoji} ${obj.objectType().displayName}") },
        text = {
            val remaining = obj.timeRemainingMs()
            val hours = remaining / 3_600_000
            val minutes = (remaining % 3_600_000) / 60_000
            Text("${obj.objectType().description}\n\nActive for ${hours}h ${minutes}m remaining")
        },
        confirmButton = {
            TextButton(onClick = { inspectedObject = null }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = {
                roomObjectViewModel.removeObject(obj)
                inspectedObject = null
            }) { Text("Remove (50% refund)") }
        }
    )
}
```

**Verification:** Tap an active object → dialog shows correct time remaining. Tap "Remove" → object disappears and hours are refunded. Tap a dormant object → it activates (glow appears), no dialog.

---

## Phase 11: Polish & Testing

### Task 11.1 — Add strings to `strings.xml`

Move all hardcoded UI strings from the shop and tooltip into `res/values/strings.xml`. Minimum additions:

```xml
<string name="shop_title">Shop</string>
<string name="shop_balance">%1$.1f hrs  %2$.1f hrs  %3$.1f hrs</string>
<string name="shop_place">Place</string>
<string name="shop_cost">Cost: %1$.0fh %2$s time</string>
<string name="placement_banner">Tap the floor to place %1$s %2$s</string>
<string name="object_remove">Remove (50% refund)</string>
<string name="object_active_remaining">%1$s\n\nActive for %2$dh %3$dm remaining</string>
<string name="spendable_hours_label">Spendable Hours</string>
```

---

### Task 11.2 — Unit Tests for New Logic

Add tests for:

- `RoomObject.isCurrentlyActive()` — active within window, expired, never activated
- `RoomObject.timeRemainingMs()` — correct ms returned, zero when expired
- `XpEngine.calculateXpWithMultipliers()` — multipliers correctly scale XP per category
- `RoomObjectViewModel.purchaseAndPlace()` — deducts correct category hours, returns early when unaffordable
- `RoomObjectViewModel.removeObject()` — refunds 50%, deletes from DB

---

### Task 11.3 — End-to-End Test

With mock data enabled:

1. Launch app — confirm spendable hours accumulate correctly from mock usage
2. Open shop — confirm affordability is calculated correctly
3. Place an object — confirm it appears in the room at the correct position
4. Tap the object — confirm it activates (glow), `isActive` = true in DB
5. Wait / fast-forward time — confirm object expires and glow disappears
6. Tap expired object — confirm it reactivates
7. Run the worker manually — confirm active multipliers are applied to XP calculation
8. Confirm XP is higher than baseline when an enrichment object is active during enrichment usage

---

## Feature Complete Checklist

Before merging this feature, confirm:

- [ ] `RoomObjectType` enum compiles with all six object types
- [ ] `RoomObject` entity persists correctly to Room DB
- [ ] Database migration runs without crashing on existing installs
- [ ] Spendable hours accumulate correctly alongside cumulative hours
- [ ] Active objects are detected by `UsageTrackingWorker` and apply multipliers to XP
- [ ] Expired objects are marked dormant on the next worker run
- [ ] Placed objects render correctly at their world positions
- [ ] Active objects show a glow; dormant objects show a grey dot
- [ ] Tapping a dormant object activates it
- [ ] Tapping an active object shows the info dialog with correct time remaining
- [ ] Shop sheet opens, shows correct balance, disables unaffordable items
- [ ] Placement mode works: select → banner → tap floor → object placed → banner clears
- [ ] Remove with 50% refund works correctly
- [ ] All new strings are in `strings.xml`
- [ ] All unit tests pass
