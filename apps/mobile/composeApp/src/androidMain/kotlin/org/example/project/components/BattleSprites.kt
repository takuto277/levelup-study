package org.example.project.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun BattleSprite(
    spriteKey: String,
    spriteType: String,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val frames = remember(spriteKey, spriteType) {
        val prefix = "sprite_${spriteType}_${spriteKey}_"
        (1..8).mapNotNull { i ->
            val resId = context.resources.getIdentifier("${prefix}$i", "drawable", context.packageName)
            if (resId != 0) resId else null
        }.ifEmpty {
            val single = context.resources.getIdentifier("sprite_${spriteType}_${spriteKey}_1", "drawable", context.packageName)
            if (single != 0) listOf(single) else emptyList()
        }
    }

    if (frames.isEmpty()) return

    var currentFrame by remember { mutableIntStateOf(0) }

    if (frames.size > 1) {
        LaunchedEffect(frames) {
            while (true) {
                delay(200L)
                currentFrame = (currentFrame + 1) % frames.size
            }
        }
    }

    Image(
        painter = painterResource(frames[currentFrame]),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun PlayerSprite(
    phase: String,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val frames = remember(phase) {
        val prefix = "sprite_player_${phase}_"
        (1..8).mapNotNull { i ->
            val resId = context.resources.getIdentifier("${prefix}$i", "drawable", context.packageName)
            if (resId != 0) resId else null
        }
    }

    if (frames.isEmpty()) return

    var currentFrame by remember { mutableIntStateOf(0) }

    if (frames.size > 1) {
        LaunchedEffect(frames) {
            while (true) {
                delay(if (phase == "attack") 150L else 200L)
                currentFrame = (currentFrame + 1) % frames.size
            }
        }
    }

    Image(
        painter = painterResource(frames[currentFrame]),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun DungeonBackground(
    dungeonName: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bgType = remember(dungeonName) {
        when {
            dungeonName == null -> "default"
            dungeonName.contains("森") || dungeonName.contains("forest", true) -> "forest"
            dungeonName.contains("洞窟") || dungeonName.contains("水晶") || dungeonName.contains("cave", true) -> "cave"
            dungeonName.contains("塔") || dungeonName.contains("炎") || dungeonName.contains("tower", true) -> "tower"
            else -> "default"
        }
    }

    val resId = remember(bgType) {
        context.resources.getIdentifier("bg_dungeon_$bgType", "drawable", context.packageName)
    }

    if (resId != 0) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alpha = 0.7f
        )
    }
}

fun hasSpriteResource(context: android.content.Context, spriteType: String, key: String): Boolean {
    return context.resources.getIdentifier(
        "sprite_${spriteType}_${key}_1", "drawable", context.packageName
    ) != 0
}

fun hasBackgroundResource(context: android.content.Context, dungeonName: String?): Boolean {
    val bgType = when {
        dungeonName == null -> "default"
        dungeonName.contains("森") || dungeonName.contains("forest", true) -> "forest"
        dungeonName.contains("洞窟") || dungeonName.contains("水晶") || dungeonName.contains("cave", true) -> "cave"
        dungeonName.contains("塔") || dungeonName.contains("炎") || dungeonName.contains("tower", true) -> "tower"
        else -> "default"
    }
    return context.resources.getIdentifier("bg_dungeon_$bgType", "drawable", context.packageName) != 0
}
