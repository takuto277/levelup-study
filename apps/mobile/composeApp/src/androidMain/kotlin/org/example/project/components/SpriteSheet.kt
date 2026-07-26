package org.example.project.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext

/**
 * スプライトシート（6列×2行、各セル96×96px）からフレームを切り出す。
 *
 * レイアウト:
 *   行0: idle_1, idle_2, prep_1, prep_2, atk_1, atk_2
 *   行1: atk_3, atk_4, atk_5, walk_1, walk_2, rest_1
 */
object SpriteSheet {

    private const val COLS = 6
    private const val CELL = 96

    /** 各モードのフレームインデックス範囲 */
    val idleFrames = 0..1
    val prepFrames = 2..3
    val attackFrames = 4..8
    val walkFrames = 9..10
    val restFrame = 11

    /** スプライトシート Bitmap をロード */
    @Composable
    fun rememberSheet(): Bitmap? {
        val context = LocalContext.current
        val id = context.resources.getIdentifier(
            "sprite_player_sheet", "drawable", context.packageName
        )
        if (id == 0) return null
        return remember {
            BitmapFactory.decodeResource(context.resources, id)
        }
    }

    /** フレームインデックスから BitmapPainter を作る */
    fun framePainter(sheet: Bitmap, frameIndex: Int): BitmapPainter {
        val col = frameIndex % COLS
        val row = frameIndex / COLS
        val x = col * CELL
        val y = row * CELL
        val frame = Bitmap.createBitmap(sheet, x, y, CELL, CELL)
        return BitmapPainter(frame.asImageBitmap())
    }
}
