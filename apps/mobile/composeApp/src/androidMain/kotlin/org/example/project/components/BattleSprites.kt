package org.example.project.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** プレイヤー表示モード（歩き / 待機 / 攻撃準備 / 攻撃 / 休憩） */
enum class PlayerSpriteMode {
    /** `sprite_player_walk_1` と `sprite_player_walk_2` を交互 */
    Walking,
    /** `sprite_player_idle_1` と `sprite_player_idle_2` を交互 */
    Idle,
    /** `sprite_player_prep_1` と `sprite_player_prep_2` を順再生 */
    Prep,
    /** `sprite_player_attack_1` 〜 `sprite_player_attack_5` を順再生 */
    Attack,
    Attack,
    /** `sprite_player_rest_1`（無ければ idle / prep / walk） */
    Rest
}

private fun drawableId(context: android.content.Context, baseName: String): Int =
    context.resources.getIdentifier(baseName, "drawable", context.packageName)

@Composable
fun BattleSprite(
    spriteKey: String,
    spriteType: String,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier,
    /** false のとき先頭フレームのみ（戦闘接近シーンの敵1枚表示用） */
    animateFrames: Boolean = true
) {
    val context = LocalContext.current
    val frames = remember(spriteKey, spriteType) {
        val prefix = "sprite_${spriteType}_${spriteKey}_"
        (1..8).mapNotNull { i ->
            val resId = drawableId(context, "${prefix}$i")
            if (resId != 0) resId else null
        }.ifEmpty {
            val single = drawableId(context, "sprite_${spriteType}_${spriteKey}_1")
            if (single != 0) listOf(single) else emptyList()
        }
    }

    if (frames.isEmpty()) return

    var currentFrame by remember { mutableIntStateOf(0) }

    if (frames.size > 1 && animateFrames) {
        LaunchedEffect(frames) {
            while (true) {
                delay(280L)
                currentFrame = (currentFrame + 1) % frames.size
            }
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(frames[currentFrame]),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun PlayerSprite(
    mode: PlayerSpriteMode,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    when (mode) {
        PlayerSpriteMode.Walking -> {
            val id1 = drawableId(context, "sprite_player_walk_1")
            val id2 = drawableId(context, "sprite_player_walk_2")
            val frames = listOfNotNull(
                id1.takeIf { it != 0 },
                id2.takeIf { it != 0 }
            ).distinct()
            if (frames.isEmpty()) return

            var currentFrame by remember { mutableIntStateOf(0) }
            if (frames.size > 1) {
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(320L)
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

        PlayerSpriteMode.Attack -> {
            val frames = collectFrameIds(context, "sprite_player_attack")
            if (frames.isEmpty()) return
            var currentFrame by remember { mutableIntStateOf(0) }
            LaunchedEffect(mode) {
                currentFrame = 0
                for (i in 1 until frames.size) {
                    delay(80)
                    currentFrame = i
                }
            }
            Image(painter = painterResource(frames[currentFrame]), contentDescription = null,
                modifier = modifier.size(size), contentScale = ContentScale.Fit)
        }

        PlayerSpriteMode.Prep -> {
            val frames = collectFrameIds(context, "sprite_player_prep")
            if (frames.isEmpty()) return
            var currentFrame by remember { mutableIntStateOf(0) }
            LaunchedEffect(mode) {
                currentFrame = 0
                for (i in 1 until frames.size) {
                    delay(100)
                    currentFrame = i
                }
            }
            Image(painter = painterResource(frames[currentFrame]), contentDescription = null,
                modifier = modifier.size(size), contentScale = ContentScale.Fit)
        }

        PlayerSpriteMode.Idle -> {
            val frames = collectFrameIds(context, "sprite_player_idle")
            if (frames.isEmpty()) return
            var currentFrame by remember { mutableIntStateOf(0) }
            LaunchedEffect(mode) {
                while (true) {
                    delay(600)
                    currentFrame = (currentFrame + 1) % frames.size
                }
            }
            Image(painter = painterResource(frames[currentFrame]), contentDescription = null,
                modifier = modifier.size(size), contentScale = ContentScale.Fit)
        }

        PlayerSpriteMode.Rest -> {
            val restId = drawableId(context, "sprite_player_rest_1").takeIf { it != 0 }
                ?: drawableId(context, "sprite_player_idle_1").takeIf { it != 0 }
                ?: drawableId(context, "sprite_player_prep_1").takeIf { it != 0 }
                ?: drawableId(context, "sprite_player_walk_1").takeIf { it != 0 }
                ?: drawableId(context, "sprite_player_walk_2")
            if (restId == 0) return
            Image(
                painter = painterResource(restId),
                contentDescription = null,
                modifier = modifier.size(size),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun DungeonBackground(
    dungeonName: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 0.75f,
    isTrainingGround: Boolean = false
) {
    val context = LocalContext.current
    val bgType = remember(dungeonName, isTrainingGround) {
        when {
            isTrainingGround -> "training"
            dungeonName == null -> "default"
            dungeonName.contains("森") || dungeonName.contains("forest", true) -> "forest"
            dungeonName.contains("洞窟") || dungeonName.contains("水晶") || dungeonName.contains("cave", true) -> "cave"
            dungeonName.contains("塔") || dungeonName.contains("炎") || dungeonName.contains("tower", true) -> "tower"
            else -> "default"
        }
    }

    val resId = remember(bgType) {
        val id = drawableId(context, "bg_dungeon_$bgType")
        if (id != 0) id else drawableId(context, "bg_dungeon_default")
    }

    if (resId != 0) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            alpha = alpha
        )
    }
}

fun hasSpriteResource(context: android.content.Context, spriteType: String, key: String): Boolean =
    drawableId(context, "sprite_${spriteType}_${key}_1") != 0

fun hasPlayerWalkSprite(context: android.content.Context): Boolean =
    drawableId(context, "sprite_player_walk_1") != 0

/** 編成画面などでプレイヤーキャラの静止画を表示できるか（idle / prep / walk のいずれか） */
fun hasPartyPlayerSprite(context: android.content.Context): Boolean =
    drawableId(context, "sprite_player_idle_1") != 0 ||
        drawableId(context, "sprite_player_prep_1") != 0 ||
        drawableId(context, "sprite_player_walk_1") != 0

private fun collectFrameIds(context: android.content.Context, prefix: String): List<Int> {
    return (1..8).mapNotNull { frame ->
        drawableId(context, "${prefix}_$frame").takeIf { it != 0 }
    }
}

@Composable
private fun FrameAnimator(
    frames: List<Int>,
    intervalMs: Long,
    loop: Boolean = true,
    content: @Composable (Int) -> Unit
) {
    if (frames.size < 2) {
        content(frames.first())
        return
    }
    var currentFrame by remember { mutableIntStateOf(0) }
    LaunchedEffect(frames, loop) {
        while (loop || currentFrame < frames.size - 1) {
            delay(intervalMs)
            currentFrame = if (loop) (currentFrame + 1) % frames.size
            else (currentFrame + 1).coerceAtMost(frames.size - 1)
        }
    }
    content(frames[currentFrame])
}

/** Attack/Prep 用: 毎回再生成される key で LaunchedEffect を走らせ、一連の再生を保証 */
@Composable
private fun OneShotAnimator(
    frames: List<Int>,
    intervalMs: Long,
    content: @Composable (Int) -> Unit
) {
    if (frames.isEmpty()) return
    if (frames.size == 1) {
        content(frames.first())
        return
    }
    val triggerKey = remember { mutableIntStateOf(0) }
    var currentFrame by remember { mutableIntStateOf(0) }
    LaunchedEffect(triggerKey.intValue) {
        currentFrame = 0
        for (i in 1 until frames.size) {
            delay(intervalMs)
            currentFrame = i
        }
    }
    // triggerKey を毎回進めて新しい LaunchedEffect を起こす
    LaunchedEffect(Unit) { triggerKey.intValue++ }
    content(frames[currentFrame])
}

fun hasBackgroundResource(
    context: android.content.Context,
    dungeonName: String?,
    isTrainingGround: Boolean = false
): Boolean {
    val bgType = when {
        isTrainingGround -> "training"
        dungeonName == null -> "default"
        dungeonName.contains("森") || dungeonName.contains("forest", true) -> "forest"
        dungeonName.contains("洞窟") || dungeonName.contains("水晶") || dungeonName.contains("cave", true) -> "cave"
        dungeonName.contains("塔") || dungeonName.contains("炎") || dungeonName.contains("tower", true) -> "tower"
        else -> "default"
    }
    val id = drawableId(context, "bg_dungeon_$bgType")
    return if (id != 0) true else drawableId(context, "bg_dungeon_default") != 0
}

/** 訓練場の樽スプライト（`prop_training_barrel.png`）。無ければ 0。 */
fun trainingBarrelDrawableId(context: android.content.Context): Int =
    drawableId(context, "prop_training_barrel")
