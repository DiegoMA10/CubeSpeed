package com.example.cubespeed.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cubespeed.ui.screens.algorithms.utils.AlgUtils

/**
 * PLL preview inspirado en CubeSpeed:
 * - Central 3×3
 * - Franjas auxiliares rectangulares en cada lado
 * - Diseño compacto con margen extra en top y left
 * - Muestra una imagen de PLL superpuesta sobre el canvas
 *
 * @param state Cadena de 21 caracteres del cubo:
 *   - 3 stickers frontales [0-2]
 *   - 3 stickers izquierdos [3-5]
 *   - 9 stickers superiores [6-14]
 *   - 3 stickers derechos [15-17]
 *   - 3 stickers traseros [18-20]
 * @param pllCase Nombre del caso PLL a mostrar (ej: "aa_perm")
 * @param size Tamaño total del componente
 * @param gap Espacio entre stickers
 * @param cornerRadius Radio de esquinas
 */
@Composable
fun PLLView(
    state: String,
    pllCase: String,
    size: Dp = 120.dp,
    gap: Dp = 2.dp,
    cornerRadius: Dp = 2.dp
) {
    // Get PLL image resource
    val context = LocalContext.current

    // Convert new PLL case name format to old resource name format
    val resourceName = when {
        pllCase.length == 1 -> "pll_${pllCase.lowercase()}_perm" // Single letter cases like "E" -> "pll_e_perm"
        else -> "pll_${pllCase.lowercase()}_perm" // Cases like "Aa" -> "pll_aa_perm"
    }

    val resourceId = context.resources.getIdentifier(
        resourceName,
        "drawable",
        context.packageName
    )

    // Box to overlay PLL image on top of canvas
    Box(
        contentAlignment = Alignment.Center
    ) {
        // Canvas for cube visualization
        Canvas(
            modifier = Modifier.size(size)
                .background(Color.DarkGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp))
        ) {
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
                drawCell(x, g / 2 + margin, cell, aux, i)
            }
            // Bottom auxiliaries (traseros) [18-20]
            for (i in 0..2) {
                val x = start + i * (cell + g)
                val y = start + 3 * (cell + g) - g / 2 + margin
                drawCell(x, y, cell, aux, 18 + i)
            }

            // Left auxiliaries (izquierdos) [3-5]
            for (i in 0..2) {
                val y = start + i * (cell + g)
                drawCell(g / 2 + margin, y, aux, cell, 3 + i)
            }
            // Right auxiliaries (derechos) [15-17]
            for (i in 0..2) {
                val y = start + i * (cell + g)
                val x = start + 3 * (cell + g) - g / 2 + margin
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

        // Display PLL image on top of the canvas if resource exists
        if (resourceId != 0) {
            Image(
                painter = painterResource(id = resourceId),
                contentDescription = "PLL $pllCase",
                modifier = Modifier.size(size)
            )
        }
    }
}

@Preview
@Composable
fun PreviewPLLView() {
    // Using the getCaseState method to get a 21-character string
    val state = AlgUtils.getCaseState(LocalContext.current, "OLL", "OLL 01")
    PLLView(
        state = state,
        pllCase = "T",
        size = 200.dp,
        gap = 6.dp,
        cornerRadius = 7.dp
    )
}

@Preview
@Composable
fun PreviewPLLViewWithDifferentCase() {
    // Using the getCaseState method to get a 21-character string
    val state = AlgUtils.getCaseState(LocalContext.current, "OLL", "OLL 01")
    PLLView(
        state = state,
        pllCase = "Aa",
        size = 200.dp,
        gap = 6.dp,
        cornerRadius = 7.dp
    )
}
