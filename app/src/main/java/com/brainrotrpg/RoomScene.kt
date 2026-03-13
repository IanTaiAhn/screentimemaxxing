package com.brainrotrpg

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import com.brainrotrpg.ui.theme.BrainRotRPGTheme
import kotlinx.coroutines.withFrameMillis
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// Tier thresholds (hours) — tune these as needed
// ---------------------------------------------------------------------------
private const val TIER_SPARSE_HOURS = 2f
private const val TIER_MODERATE_HOURS = 5f
private const val TIER_PACKED_HOURS = 10f

// ---------------------------------------------------------------------------
// Colors — placeholder palette, replace with real sprites later
// ---------------------------------------------------------------------------
private val COLOR_FLOOR = Color(0xFFB8860B)
private val COLOR_FLOOR_TILE = Color(0xFFA07808)
private val COLOR_WALL_LEFT = Color(0xFFD4B896)
private val COLOR_WALL_RIGHT = Color(0xFFC4A882)
private val COLOR_WALL_BACK = Color(0xFFE8D4B4)
private val COLOR_COUCH = Color(0xFF4A7C59)
private val COLOR_COUCH_SHADOW = Color(0xFF2E5E3A)
private val COLOR_CHARACTER = Color(0xFFFFD700)
private val COLOR_CHARACTER_SHADOW = Color(0xFFC8A800)

// Category item colors
private val COLOR_BRAINROT_PRIMARY = Color(0xFFCC2200)    // soda cans / pizza boxes
private val COLOR_BRAINROT_SECONDARY = Color(0xFFFF6644)  // phone glow
private val COLOR_ENRICHMENT_PRIMARY = Color(0xFF2266CC)  // bookshelves
private val COLOR_ENRICHMENT_SECONDARY = Color(0xFF88BBFF) // music notes / headphones
private val COLOR_MID_PRIMARY = Color(0xFF885500)          // monitors / meme posters
private val COLOR_MID_SECONDARY = Color(0xFFFFAA00)        // energy drinks

// ---------------------------------------------------------------------------
// Item spawn positions
// Each position is a normalized (0..1) offset relative to canvas size,
// mapped to isometric floor space. Replace with sprite draw calls later.
// Positions are ordered: sparse slots first, packed slots last.
// ---------------------------------------------------------------------------
private val BRAINROT_POSITIONS = listOf(
    // Sparse tier (0-2): soda cans on floor near couch
    Offset(0.28f, 0.72f),
    Offset(0.32f, 0.75f),
    // Moderate tier (3-5): pizza boxes accumulate on coffee table area
    Offset(0.42f, 0.68f),
    Offset(0.38f, 0.72f),
    Offset(0.35f, 0.65f),
    // Packed tier (6-9): overflow onto floor, phone glow near walls
    Offset(0.22f, 0.78f),
    Offset(0.45f, 0.75f),
    Offset(0.30f, 0.80f),
    Offset(0.48f, 0.70f)
)

private val ENRICHMENT_POSITIONS = listOf(
    // Sparse: headphones on couch armrest area
    Offset(0.25f, 0.58f),
    Offset(0.20f, 0.62f),
    // Moderate: books stack on floor, music notes float near wall
    Offset(0.18f, 0.55f),
    Offset(0.15f, 0.60f),
    Offset(0.22f, 0.52f),
    // Packed: full bookshelf overflow, notes scattered
    Offset(0.12f, 0.57f),
    Offset(0.17f, 0.50f),
    Offset(0.10f, 0.63f),
    Offset(0.24f, 0.48f)
)

private val MID_POSITIONS = listOf(
    // Sparse: energy drink near couch
    Offset(0.55f, 0.62f),
    Offset(0.60f, 0.65f),
    // Moderate: meme posters on right wall, monitor glow
    Offset(0.65f, 0.55f),
    Offset(0.58f, 0.58f),
    Offset(0.70f, 0.60f),
    // Packed: more screens, energy drink empire
    Offset(0.75f, 0.52f),
    Offset(0.62f, 0.70f),
    Offset(0.68f, 0.48f),
    Offset(0.72f, 0.65f)
)

// ---------------------------------------------------------------------------
// Tier resolution — returns how many item slots to fill (0–9)
// ---------------------------------------------------------------------------
private fun itemCount(hours: Float): Int {
    return when {
        hours >= TIER_PACKED_HOURS -> 9
        hours >= TIER_MODERATE_HOURS -> {
            // Interpolate between 3 and 6 slots within moderate tier
            val progress = (hours - TIER_MODERATE_HOURS) / (TIER_PACKED_HOURS - TIER_MODERATE_HOURS)
            3 + (progress * 3).toInt()
        }
        hours >= TIER_SPARSE_HOURS -> {
            val progress = (hours - TIER_SPARSE_HOURS) / (TIER_MODERATE_HOURS - TIER_SPARSE_HOURS)
            1 + (progress * 2).toInt()
        }
        else -> 0
    }
}

// ---------------------------------------------------------------------------
// Coordinate conversion functions
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Proximity hit detection
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Main composable
// ---------------------------------------------------------------------------
@Composable
fun RoomScene(
    brainrotHours: Float,
    midHours: Float,
    enrichmentHours: Float,
    placedObjects: List<RoomObject> = emptyList(),
    onObjectTapped: (RoomObject) -> Unit = {},
    onFloorTapped: (Float, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    characterState: CharacterState = remember { CharacterState() }
) {
    // --- Physics loop ---
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

    // --- Breathing animation ---
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
            // --- Tap handler ---
            .pointerInput(placedObjects) {   // key on placedObjects so handler refreshes when list changes
                detectTapGestures { tapOffset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val world = screenToWorld(tapOffset.x, tapOffset.y, w, h) ?: return@detectTapGestures

                    val tappedObject = findTappedObject(world.first, world.second, placedObjects)
                    if (tappedObject != null) {
                        onObjectTapped(tappedObject)   // interaction — don't walk
                    } else {
                        onFloorTapped(world.first, world.second)   // delegate to caller
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

        // Placed room objects (after category items, before character — painter's order)
        drawPlacedObjects(w, h, placedObjects)

        // --- Tap target indicator ---
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

        // --- Character (updated position with walking bob) ---
        val charPos = worldToScreen(characterState.worldX, characterState.worldY, w, h)

        // Walking bob: offset the character vertically using a sine wave driven by world position
        val bobOffset = if (characterState.isMoving) {
            val phase = (characterState.worldX + characterState.worldY) * 40f  // position-driven phase
            Math.sin(phase.toDouble()).toFloat() * w * 0.008f
        } else 0f

        val animScale = if (characterState.isMoving) 1.0f else breatheScale
        val animPivot = charPos

        scale(scale = animScale, pivot = animPivot) {
            drawCharacter(charPos.x, charPos.y + bobOffset, w)
        }
    }
}

// ---------------------------------------------------------------------------
// Draw helpers
// ---------------------------------------------------------------------------

private fun DrawScope.drawRoom(w: Float, h: Float) {
    // --- Floor (isometric diamond shape) ---
    val floorPath = Path().apply {
        moveTo(w * 0.50f, h * 0.40f) // top point
        lineTo(w * 0.95f, h * 0.62f) // right point
        lineTo(w * 0.50f, h * 0.84f) // bottom point
        lineTo(w * 0.05f, h * 0.62f) // left point
        close()
    }
    drawPath(floorPath, COLOR_FLOOR)

    // Floor tile grid lines (isometric)
    val tileColor = COLOR_FLOOR_TILE.copy(alpha = 0.4f)
    val tileSteps = 4
    for (i in 1 until tileSteps) {
        val t = i.toFloat() / tileSteps
        // Left-to-right diagonal lines
        drawLine(
            color = tileColor,
            start = Offset(w * (0.05f + t * 0.45f), h * (0.62f - t * 0.22f)),
            end = Offset(w * (0.05f + t * 0.45f + 0.45f), h * (0.62f - t * 0.22f + 0.22f)),
            strokeWidth = 1.5f
        )
        // Right-to-left diagonal lines
        drawLine(
            color = tileColor,
            start = Offset(w * (0.50f + t * 0.45f), h * (0.40f + t * 0.22f)),
            end = Offset(w * (0.50f + t * 0.45f - 0.45f), h * (0.40f + t * 0.22f + 0.22f)),
            strokeWidth = 1.5f
        )
    }

    // --- Left wall ---
    val leftWallPath = Path().apply {
        moveTo(w * 0.05f, h * 0.62f) // floor left
        lineTo(w * 0.50f, h * 0.40f) // floor top
        lineTo(w * 0.50f, h * 0.08f) // wall top-center
        lineTo(w * 0.05f, h * 0.30f) // wall top-left
        close()
    }
    drawPath(leftWallPath, COLOR_WALL_LEFT)

    // --- Right wall ---
    val rightWallPath = Path().apply {
        moveTo(w * 0.50f, h * 0.40f) // floor top
        lineTo(w * 0.95f, h * 0.62f) // floor right
        lineTo(w * 0.95f, h * 0.30f) // wall top-right
        lineTo(w * 0.50f, h * 0.08f) // wall top-center
        close()
    }
    drawPath(rightWallPath, COLOR_WALL_RIGHT)

    // Wall edge line (ridge)
    drawLine(
        color = Color(0xFF8B6914),
        start = Offset(w * 0.50f, h * 0.08f),
        end = Offset(w * 0.50f, h * 0.40f),
        strokeWidth = 2f
    )

    // --- Couch (isometric box shape on left side of room) ---
    drawIsometricBox(
        centerX = w * 0.28f,
        centerY = h * 0.62f,
        boxW = w * 0.22f,
        boxH = h * 0.10f,
        depth = h * 0.06f,
        topColor = COLOR_COUCH,
        sideColor = COLOR_COUCH_SHADOW
    )

    // Couch back (taller box behind seat)
    drawIsometricBox(
        centerX = w * 0.24f,
        centerY = h * 0.56f,
        boxW = w * 0.22f,
        boxH = h * 0.12f,
        depth = h * 0.03f,
        topColor = COLOR_COUCH,
        sideColor = COLOR_COUCH_SHADOW
    )
}

private fun DrawScope.drawIsometricBox(
    centerX: Float,
    centerY: Float,
    boxW: Float,
    boxH: Float,
    depth: Float,
    topColor: Color,
    sideColor: Color
) {
    val halfW = boxW / 2f
    val halfH = boxH / 4f // isometric foreshortening

    // Top face
    val topPath = Path().apply {
        moveTo(centerX, centerY - halfH - depth)
        lineTo(centerX + halfW, centerY - depth)
        lineTo(centerX, centerY + halfH - depth)
        lineTo(centerX - halfW, centerY - depth)
        close()
    }
    drawPath(topPath, topColor)

    // Front-left face
    val leftFacePath = Path().apply {
        moveTo(centerX - halfW, centerY - depth)
        lineTo(centerX, centerY + halfH - depth)
        lineTo(centerX, centerY + halfH)
        lineTo(centerX - halfW, centerY)
        close()
    }
    drawPath(leftFacePath, sideColor)

    // Front-right face
    val rightFacePath = Path().apply {
        moveTo(centerX, centerY + halfH - depth)
        lineTo(centerX + halfW, centerY - depth)
        lineTo(centerX + halfW, centerY)
        lineTo(centerX, centerY + halfH)
        close()
    }
    drawPath(rightFacePath, sideColor.copy(alpha = 0.7f))
}

private fun DrawScope.drawCategoryItems(
    w: Float,
    h: Float,
    positions: List<Offset>,
    count: Int,
    primaryColor: Color,
    secondaryColor: Color
) {
    val clampedCount = count.coerceIn(0, positions.size)
    for (i in 0 until clampedCount) {
        val pos = positions[i]
        val x = pos.x * w
        val y = pos.y * h
        val itemSize = w * 0.030f

        // Alternate between primary and secondary color for visual variety
        val color = if (i % 2 == 0) primaryColor else secondaryColor

        // Draw as small isometric cube placeholder
        drawIsometricBox(
            centerX = x,
            centerY = y,
            boxW = itemSize,
            boxH = itemSize,
            depth = itemSize * 0.6f,
            topColor = color,
            sideColor = color.copy(alpha = 0.6f)
        )
    }
}

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

private fun DrawScope.drawCharacter(centerX: Float, centerY: Float, w: Float) {
    val bodyW = w * 0.06f
    val bodyH = w * 0.12f
    val headSize = w * 0.05f

    // Shadow
    drawOval(
        color = Color(0x44000000),
        topLeft = Offset(centerX - bodyW * 0.8f, centerY + bodyH * 0.45f),
        size = Size(bodyW * 1.6f, bodyH * 0.15f)
    )

    // Body
    drawRect(
        color = COLOR_CHARACTER_SHADOW,
        topLeft = Offset(centerX - bodyW / 2f + 2f, centerY - bodyH / 2f + 2f),
        size = Size(bodyW, bodyH)
    )
    drawRect(
        color = COLOR_CHARACTER,
        topLeft = Offset(centerX - bodyW / 2f, centerY - bodyH / 2f),
        size = Size(bodyW, bodyH)
    )

    // Head
    drawRect(
        color = COLOR_CHARACTER_SHADOW,
        topLeft = Offset(centerX - headSize / 2f + 2f, centerY - bodyH / 2f - headSize + 2f),
        size = Size(headSize, headSize)
    )
    drawRect(
        color = COLOR_CHARACTER,
        topLeft = Offset(centerX - headSize / 2f, centerY - bodyH / 2f - headSize),
        size = Size(headSize, headSize)
    )

    // Eyes (2 small dark pixels)
    val eyeSize = headSize * 0.18f
    drawRect(
        color = Color(0xFF1A1A1A),
        topLeft = Offset(centerX - headSize * 0.28f, centerY - bodyH / 2f - headSize * 0.55f),
        size = Size(eyeSize, eyeSize)
    )
    drawRect(
        color = Color(0xFF1A1A1A),
        topLeft = Offset(centerX + headSize * 0.10f, centerY - bodyH / 2f - headSize * 0.55f),
        size = Size(eyeSize, eyeSize)
    )
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------
@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun RoomScenePreview() {
    BrainRotRPGTheme {
        RoomScene(
            brainrotHours = 6f,
            midHours = 3f,
            enrichmentHours = 8f
        )
    }
}
