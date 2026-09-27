package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import kotlin.math.abs

/**
 * High quality deterministic 2D barcode / QR matrix visualizer.
 * Generates standard finder patterns (3 corner squares) and data pattern
 * based on input text checksum for preview and sharing.
 */
@Composable
fun QrCodeVisualizer(
    content: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    codeColor: Color = Color(0xFF0F172A),
    gridSize: Int = 29
) {
    val matrix = remember(content) {
        generateQrMatrix(content, gridSize)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            val cellSize = size.width / gridSize
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = codeColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun generateQrMatrix(content: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }

    fun drawFinder(top: Int, left: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isOuter = r == 0 || r == 6 || c == 0 || c == 6
                val isInner = r in 2..4 && c in 2..4
                matrix[top + r][left + c] = isOuter || isInner
            }
        }
    }

    // Three standard corner finder patterns
    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    // Timing patterns
    for (i in 7 until size - 7) {
        if (i % 2 == 0) {
            matrix[6][i] = true
            matrix[i][6] = true
        }
    }

    // Seeded data bits from SHA-256 of content
    val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
    val contentBytes = content.toByteArray()

    var byteIdx = 0
    for (r in 0 until size) {
        for (c in 0 until size) {
            val inFinder = (r < 8 && c < 8) || (r < 8 && c >= size - 8) || (r >= size - 8 && c < 8)
            val inTiming = r == 6 || c == 6
            if (!inFinder && !inTiming) {
                val seedByte = if (byteIdx < contentBytes.size) {
                    contentBytes[byteIdx].toInt()
                } else {
                    digest[(byteIdx - contentBytes.size) % digest.size].toInt()
                }
                val bit = ((seedByte shr (abs(r * size + c) % 8)) and 1) == 1
                matrix[r][c] = bit
                byteIdx++
            }
        }
    }

    return matrix
}
