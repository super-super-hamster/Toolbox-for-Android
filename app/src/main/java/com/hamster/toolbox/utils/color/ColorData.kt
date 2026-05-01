package com.hamster.toolbox.utils.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.roundToInt

data class RGB(val r: Int, val g: Int, val b: Int)

data class LAB(val l: Float, val a: Float, val b: Float)

fun RGB.toLAB(): LAB {
    val rgbColor = Color(red = this.r, green = this.g, blue = this.b)
    val labColor = rgbColor.convert(ColorSpaces.CieLab)
    return LAB(l = labColor.red, a = labColor.green, b = labColor.blue)
}

fun RGB.toInt(): Int {
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

fun LAB.toRGB(): RGB {
    val labColor = Color(
        red = this.l,
        green = this.a,
        blue = this.b,
        alpha = 1f,
        colorSpace = ColorSpaces.CieLab
    )
    val rgbColor = labColor.convert(ColorSpaces.Srgb)
    return RGB(
        r = (rgbColor.red * 255f).roundToInt().coerceIn(0, 255),
        g = (rgbColor.green * 255f).roundToInt().coerceIn(0, 255),
        b = (rgbColor.blue * 255f).roundToInt().coerceIn(0, 255)
    )
}

fun LAB.toInt(): Int {
    return this.toRGB().toInt()
}

fun Color.toRGB(): RGB {
    return RGB(
        r = (this.red * 255).toInt().coerceIn(0, 255),
        g = (this.green * 255).toInt().coerceIn(0, 255),
        b = (this.blue * 255).toInt().coerceIn(0, 255)
    )
}