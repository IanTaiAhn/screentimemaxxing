# Isometric Character Movement — Implementation Guide

## Overview

This guide adds tap-to-move character navigation to `RoomScene.kt`. The system is lightweight — no external physics library is needed. The character glides smoothly toward wherever the player taps on the floor using a frame-based lerp loop built into Jetpack Compose.

**What gets added:**
- A new `CharacterState.kt` class managing position, target, and movement ticking
- Coordinate conversion functions mapping screen taps ↔ isometric world space
- A `pointerInput` tap handler on the `Canvas`
- A `LaunchedEffect` + `withFrameMillis` physics loop inside `RoomScene`
- A tap target indicator (subtle circle) while the character is walking

**What stays the same:** Room drawing, item accumulation, avatar states, XP system, WorkManager — nothing else is touched.

---

## Prerequisites

These imports are needed in `RoomScene.kt`. Most are already present; the new ones are marked.

```kotlin
// Already present
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale

// New imports
import androidx.compose.foundation.gestures.detectTapGestures   // NEW
import androidx.compose.ui.input.pointer.pointerInput            // NEW
import kotlinx.coroutines.withFrameMillis                        // NEW
```

No new Gradle dependencies are required.

---

## Step 1 — Create `CharacterState.kt`

Create a new file at `app/src/main/java/com/brainrotrpg/CharacterState.kt`.

This class holds all movement state as Compose `mutableStateOf` fields so the Canvas recomposes automatically when position changes.

```kotlin
package com.brainrotrpg

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.sqrt

class CharacterState {

    // Current rendered position in normalised world space (0f..1f)
    var worldX by mutableStateOf(0.5f)
    var worldY by mutableStateOf(0.5f)

    // Where the character is walking toward
    var targetX by mutableStateOf(0.5f)
    var targetY by mutableStateOf(0.5f)

    var isMoving by mutableStateOf(false)

    /**
     * Called when the player taps the floor.
     * [x] and [y] are normalised world coordinates (0f..1f).
     */
    fun setTarget(x: Float, y: Float) {
        targetX = x.coerceIn(0.05f, 0.95f)
        targetY = y.coerceIn(0.05f, 0.95f)
        isMoving = true
    }

    /**
     * Advance the character position toward the target.
     * Called once per frame; [deltaMs] is elapsed milliseconds since last frame.
     *
     * Speed is expressed in world-units per second. 1.2f feels natural for the
     * default room size — increase for a snappier feel, decrease for a slower walk.
     */
    fun tick(deltaMs: Float) {
        val speed = (deltaMs / 1000f) * 1.2f
        val dx = targetX - worldX
        val dy = targetY - worldY
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist < 0.008f) {
            worldX = targetX
            worldY = targetY
            isMoving = false
        } else {
            val move = minOf(speed, dist)
            worldX += (dx / dist) * move
            worldY += (dy / dist) * move
        }
    }
}
```

**Design notes:**
- `isMoving` being `false` when the character is idle means the `LaunchedEffect` physics loop (Step 3) is completely inactive at rest — zero overhead.
- The `coerceIn` bounds keep the character away from the very edges of the floor diamond. Adjust the margins (`0.05f` / `0.95f`) if the character clips walls.
- `0.008f` is the snap threshold. Below this distance the character teleports to the target rather than jittering around it.

---

## Step 2 — Add Coordinate Conversion Functions to `RoomScene.kt`

Add these two private functions anywhere in `RoomScene.kt`, outside any composable.

The floor diamond's corners (from `drawRoom()`) are the ground truth for this math:

| Corner | Canvas x | Canvas y |
|--------|----------|----------|
| Top    | `w * 0.50` | `h * 0.40` |
| Right  | `w * 0.95` | `h * 0.62` |
| Bottom | `w * 0.50` | `h * 0.84` |
| Left   | `w * 0.05` | `h * 0.62` |

```kotlin
/**
 * Convert normalised world coordinates (0f..1f each) to a canvas pixel position.
 * The world space maps linearly onto the isometric floor diamond.
 *
 * wx=0 is the left corner, wx=1 is the right corner.
 * wy=0 is the top corner, wy=1 is the bottom corner.
 */
private fun worldToScreen(wx: Float, wy: Float, w: Float, h: Float): Offset {
    // Bilinear blend across the four floor corners
    val topX    = w * 0.50f;  val topY    = h * 0.40f
    val rightX  = w * 0.95f;  val rightY  = h * 0.62f
    val bottomX = w * 0.50f;  val bottomY = h * 0.84f
    val leftX   = w * 0.05f;  val leftY   = h * 0.62f

    // Interpolate: wx moves left→right, wy moves top→bottom
    val topEdgeX    = leftX   + wx * (topX    - leftX)
    val topEdgeY    = leftY   + wx * (topY    - leftY)
    val bottomEdgeX = leftX   + wx * (rightX  - leftX)
    val bottomEdgeY = leftY   + wx * (rightY  - leftY)

    val screenX = topEdgeX + wy * (bottomEdgeX - topEdgeX)
    val screenY = topEdgeY + wy * (bottomEdgeY - topEdgeY)
    return Offset(screenX, screenY)
}

/**
 * Convert a screen-space tap position to normalised world coordinates.
 * Returns null if the tap falls outside the isometric floor diamond.
 */
private fun screenToWorld(sx: Float, sy: Float, w: Float, h: Float): Pair<Float, Float>? {
    // Centre of the floor diamond in canvas space
    val cx = w * 0.50f
    val cy = h * 0.62f

    // Half-extents of the diamond axes
    val halfWidth  = w * 0.45f   // left→right axis
    val halfHeight = h * 0.22f   // top→bottom axis

    val relX = (sx - cx) / halfWidth   // -1..1
    val relY = (sy - cy) / halfHeight  // -1..1

    // Standard diamond containment: |u| + |v| < 1
    if (Math.abs(relX) + Math.abs(relY) > 0.96f) return null

    // Map -1..1 → 0..1 world space
    val wx = ((relX + 1f) / 2f).coerceIn(0.05f, 0.95f)
    val wy = ((relY + 1f) / 2f).coerceIn(0.05f, 0.95f)
    return Pair(wx, wy)
}
```

**Debugging tip:** During development, temporarily draw a small circle at every `screenToWorld` result to visually confirm the conversion is landing where you tapped. Remove before release.

---

## Step 3 — Update `RoomScene` Composable

Replace the existing `RoomScene` signature and body with the version below. The changes are:

1. Add `characterState` parameter (with a `remember`ed default so existing call sites don't break)
2. Add `LaunchedEffect` physics loop
3. Add `.pointerInput` tap handler to the `Canvas` modifier
4. Replace the hardcoded character centre with the dynamic `worldToScreen` position
5. Draw a tap target indicator dot

```kotlin
@Composable
fun RoomScene(
    brainrotHours: Float,
    midHours: Float,
    enrichmentHours: Float,
    modifier: Modifier = Modifier,
    characterState: CharacterState = remember { CharacterState() }   // NEW
) {
    // --- Physics loop (NEW) ---
    // Only active while the character is moving; zero overhead when idle.
    LaunchedEffect(characterState.isMoving) {
        if (!characterState.isMoving) return@LaunchedEffect
        var lastFrameMs = 0L
        while (characterState.isMoving) {
            withFrameMillis { frameMs ->
                val delta = if (lastFrameMs == 0L) 16f else (frameMs - lastFrameMs).toFloat()
                lastFrameMs = frameMs
                characterState.tick(delta)
            }
        }
    }

    // --- Breathing animation (unchanged) ---
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            // --- Tap handler (NEW) ---
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val world = screenToWorld(tapOffset.x, tapOffset.y, w, h)
                    if (world != null) {
                        characterState.setTarget(world.first, world.second)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // Room and items — unchanged
        drawRoom(w, h)
        drawCategoryItems(w, h, BRAINROT_POSITIONS,    itemCount(brainrotHours),    COLOR_BRAINROT_PRIMARY,    COLOR_BRAINROT_SECONDARY)
        drawCategoryItems(w, h, ENRICHMENT_POSITIONS,  itemCount(enrichmentHours),  COLOR_ENRICHMENT_PRIMARY,  COLOR_ENRICHMENT_SECONDARY)
        drawCategoryItems(w, h, MID_POSITIONS,         itemCount(midHours),         COLOR_MID_PRIMARY,         COLOR_MID_SECONDARY)

        // --- Tap target indicator (NEW) ---
        // Shows a faint circle at the destination while the character is walking.
        if (characterState.isMoving) {
            val target = worldToScreen(characterState.targetX, characterState.targetY, w, h)
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = w * 0.018f,
                center = target
            )
            // Crosshair lines
            val r = w * 0.025f
            drawLine(Color.White.copy(alpha = 0.25f), target - Offset(r, 0f), target + Offset(r, 0f), strokeWidth = 1.5f)
            drawLine(Color.White.copy(alpha = 0.25f), target - Offset(0f, r), target + Offset(0f, r), strokeWidth = 1.5f)
        }

        // --- Character (updated position) ---
        val charPos = worldToScreen(characterState.worldX, characterState.worldY, w, h)   // CHANGED
        scale(scale = breatheScale, pivot = charPos) {
            drawCharacter(charPos.x, charPos.y, w)
        }
    }
}
```

---

## Step 4 — Pass `CharacterState` from `AvatarScreen` (Optional but Recommended)

Currently the default `remember { CharacterState() }` inside `RoomScene` is fine for basic use. If you want to persist movement state across recompositions or drive it from the ViewModel later, hoist it to `AvatarScreen`.

In `AvatarViewModel.kt`, or simply in the composable:

```kotlin
// In AvatarScreen.kt
@Composable
fun AvatarScreen(
    avatarViewModel: AvatarViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by avatarViewModel.uiState.collectAsStateWithLifecycle()
    val characterState = remember { CharacterState() }   // hoisted here

    AvatarScreenContent(
        uiState = uiState,
        characterState = characterState,
        modifier = modifier
    )
}

// Pass it down to AvatarScreenContent → RoomScene
```

This is optional for MVP. The in-place `remember` default works fine.

---

## Step 5 — Optional: Walking Bob Animation

When the character is moving, swap the breathing scale for a vertical walking bob. This gives the flat sprite a sense of footsteps without any additional assets.

Inside the `Canvas` draw block, replace the character draw section with:

```kotlin
val charPos = worldToScreen(characterState.worldX, characterState.worldY, w, h)

// Walking bob: offset the character vertically using a sine wave driven by world position
val bobOffset = if (characterState.isMoving) {
    val dist = Math.sqrt(
        ((characterState.worldX - characterState.targetX).toDouble().let { it * it } +
         (characterState.worldY - characterState.targetY).toDouble().let { it * it })
    ).toFloat()
    val phase = (characterState.worldX + characterState.worldY) * 40f  // position-driven phase
    Math.sin(phase.toDouble()).toFloat() * w * 0.008f
} else 0f

val animScale = if (characterState.isMoving) 1.0f else breatheScale
val animPivot = charPos

scale(scale = animScale, pivot = animPivot) {
    drawCharacter(charPos.x, charPos.y + bobOffset, w)
}
```

---

## Tuning Reference

| Parameter | Location | Effect |
|-----------|----------|--------|
| Movement speed | `CharacterState.tick()` — the `1.2f` multiplier | Higher = faster walk |
| Snap distance | `CharacterState.tick()` — the `0.008f` threshold | Lower = more precise stop |
| Floor bounds margin | `CharacterState.setTarget()` — `coerceIn(0.05f, 0.95f)` | Keeps character away from walls |
| Diamond edge tolerance | `screenToWorld()` — the `0.96f` cutoff | Higher = larger tappable area |
| Tap indicator size | `drawCircle` radius — `w * 0.018f` | Visual feedback scale |
| Bob amplitude | Optional bob section — `w * 0.008f` | Height of walking bounce |

---

## Testing Checklist

- [ ] Tap inside the floor diamond → character walks to that point
- [ ] Tap outside the floor (walls, ceiling area) → no movement, no crash
- [ ] Tap a new destination mid-walk → character smoothly redirects
- [ ] Character reaches destination and stops → `isMoving` becomes `false`
- [ ] App survives rotation / recomposition mid-walk (state is in `remember`)
- [ ] Mock data mode still shows correct avatar state while character moves
- [ ] No visible jitter when character snaps to final position
- [ ] Preview in Android Studio still renders (default `remember` handles the preview case)

---

## What This Does Not Include (Post-MVP)

- **Collision with furniture** — the couch and items are purely visual; the character walks through them. Adding collision would require bounding boxes per item and a path-finding step around them.
- **Facing direction** — the character sprite doesn't flip or rotate to face the walk direction. Post-MVP this could use a simple `dx/dy` sign check to pick a directional sprite.
- **Multiple characters or NPCs** — `CharacterState` is a single instance. A list of states + IDs would extend this to multiple movers.
- **Persistence across sessions** — movement state is in-memory only. The character always starts at world centre `(0.5, 0.5)` on app launch.
