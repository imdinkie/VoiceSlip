package com.example.voiceslip.service

import kotlin.math.abs
import kotlin.math.roundToInt

internal enum class BubbleEdge {
    START,
    END
}

internal data class BubbleBounds(
    val width: Int,
    val height: Int,
    val bubbleWidth: Int,
    val bubbleHeight: Int,
    val edgePadding: Int
) {
    val minX: Int = edgePadding
    val maxX: Int = maxOf(edgePadding, width - bubbleWidth - edgePadding)
    val minY: Int = edgePadding
    val maxY: Int = maxOf(edgePadding, height - bubbleHeight - edgePadding)
}

internal data class BubblePosition(
    val x: Int,
    val y: Int
)

internal data class BubblePlacement(
    val edge: BubbleEdge,
    val verticalFraction: Float
) {
    fun toPosition(bounds: BubbleBounds): BubblePosition {
        val x = when (edge) {
            BubbleEdge.START -> bounds.minX
            BubbleEdge.END -> bounds.maxX
        }
        val y = (bounds.minY + (bounds.maxY - bounds.minY) * verticalFraction)
            .roundToInt()
            .coerceIn(bounds.minY, bounds.maxY)
        return BubblePosition(x, y)
    }

    companion object {
        fun fromPosition(x: Int, y: Int, bounds: BubbleBounds): BubblePlacement {
            val clampedX = x.coerceIn(bounds.minX, bounds.maxX)
            val edge = if (abs(clampedX - bounds.minX) <= abs(clampedX - bounds.maxX)) {
                BubbleEdge.START
            } else {
                BubbleEdge.END
            }
            val yRange = bounds.maxY - bounds.minY
            val fraction = if (yRange <= 0) 0f else (y.coerceIn(bounds.minY, bounds.maxY) - bounds.minY).toFloat() / yRange
            return BubblePlacement(edge, fraction.coerceIn(0f, 1f))
        }
    }
}
