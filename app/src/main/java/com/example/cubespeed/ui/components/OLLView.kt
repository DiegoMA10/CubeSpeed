package com.example.cubespeed.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cubespeed.utils.AlgUtils

/**
 * OLL preview inspirado en Twisty Timer:
 * - Central 3×3
 * - Franjas auxiliares rectangulares en cada lado
 * - Diseño compacto con margen extra en top y left
 *
 * @param state Cadena de 21 caracteres del cubo:
 *   - 3 stickers frontales [0-2]
 *   - 3 stickers izquierdos [3-5]
 *   - 9 stickers superiores [6-14]
 *   - 3 stickers derechos [15-17]
 *   - 3 stickers traseros [18-20]
 * @param size Tamaño total del componente
 * @param gap Espacio entre stickers
 * @param cornerRadius Radio de esquinas
 */
@Composable
fun OLLView(
    state: String,
    size: Dp = 120.dp,
    gap: Dp = 2.dp,
    cornerRadius: Dp = 2.dp
) {
    Canvas(modifier = Modifier.size(size).background(Color.DarkGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp))) {
        val px = size.toPx()
        val g = gap.toPx()
        // proporción de auxiliares respecto a sticker
        val auxRatio = 0.37f
        // margen extra como proporción de auxiliar
        val marginRatio = 0f
        // calculamos cell y aux: 2*aux + 3*cell + 4*g = px
        val cell = (px - 4 * g) / (3 + 2 * auxRatio)
        val aux = auxRatio * cell
        // margen en px
        val margin = marginRatio * aux
        // inicio rejilla central con margen aplicado en top y left
        val start = aux + g + margin

        fun drawCell(x: Float, y: Float, w: Float, h: Float, idx: Int) {
            val colorInt = AlgUtils.getColorFromStateIndex(state, idx)
            drawRoundRect(
                color = Color(colorInt),
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }

        // Top auxiliaries (frontales) [0-2]
        for (i in 0..2) {
            val x = start + i * (cell + g)
            drawCell(x, g/2+margin, cell, aux, i)
        }
        // Bottom auxiliaries (traseros) [18-20]
        for (i in 0..2) {
            val x = start + i * (cell + g)
            val y = start + 3 * (cell + g) -g/2+ margin
            drawCell(x, y, cell, aux, 18 + i)
        }

        // Left auxiliaries (izquierdos) [3-5]
        for (i in 0..2) {
            val y = start + i * (cell + g)
            drawCell(g / 2+ margin, y, aux, cell, 3 + i)
        }
        // Right auxiliaries (derechos) [15-17]
        for (i in 0..2) {
            val y = start + i * (cell + g)
            val x = start + 3 * (cell + g)  -g/2+ margin
            drawCell(x, y, aux, cell, 15 + i)
        }

        // Rejilla central 3×3 (superiores) [6-14]
        for (r in 0..2) {
            for (c in 0..2) {
                val idx = 6 + r * 3 + c
                val x = start + c * (cell + g)
                val y = start + r * (cell + g)
                drawCell(x, y, cell, cell, idx)
            }
        }
    }
}

@Preview
@Composable
fun PreviewTwistyOLL() {
    // Using the getCaseState method to get a 21-character string
    val state = AlgUtils.getCaseState(LocalContext.current, "OLL", "OLL 01")
    OLLView(state = state, size = 200.dp, gap = 6.dp, cornerRadius = 7.dp)
}
