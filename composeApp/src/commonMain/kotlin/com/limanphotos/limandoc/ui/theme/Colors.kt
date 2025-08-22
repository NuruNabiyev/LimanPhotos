package theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces

fun Color.luminance(factor: Float): Color {
    return Color(
        minOf(red * factor, ColorSpaces.Srgb.getMaxValue(0)),
        minOf(green * factor, ColorSpaces.Srgb.getMaxValue(1)),
        minOf(blue * factor, ColorSpaces.Srgb.getMaxValue(2)),
        alpha
    )
}

object LightTheme {
    //Gray
    val gray_1 = Color(0xFF000000)

    /** Dark background */
    val gray_3 = Color(0xFF383A42)
    val gray_6 = Color(0xFF6C707E)

    /** Disabled text & placeholders, Icons & shortcuts on dark bg Diff removed: Stripe mark */
    val gray_9 = Color(0xFFC9CCD6)
    val gray_10 = Color(0xFFD3D5DB)

    /** Diff removed: Word, Diff removed: Stroke */
    val gray_11 = Color(0xFFDFE1E5)

    /** Inactive selection */
    val gray_12 = Color(0xFFDFE1E5)

    /** Lines & separators, General icons: Fill */
    val gray_13 = Color(0xFFEBECF0)

    /** Secondary background (tool windows), Diff removed: Fill */
    val gray_14 = Color(0xFFF7F8FA)

    /** white */

    //Blue
    val blue_1 = Color(0xFF2E55A3)
    val blue_2 = Color(0xFF315FBD)

    /** Link Button: Pressed */
    val blue_3 = Color(0xFF3369D6)

    /** Button: Default, General icons: Fill, Outline icons */
    val blue_5 = Color(0xFF4682FA)

    /** Banner stroke, Diff changed: Stroke, Diff changed: Word */
    val blue_11 = Color(0xFFD4E2FF)

    /** Selection active */
    val blue_12 = Color(0xFFEDF3FF)

    /** Editor: Current line, Banner fill */
    //Green
    val green_1 = Color(0xFF1E6B33)

    /** Button: Hovered */
    val green_4 = Color(0xFF208A3C)

    /** Test root in com.limanphotos.limandoc tree */
    val green_11 = Color(0xFFF2FCF3)

    /** General icons: Fill, Banner: Fill */
    //Red
    val red_1 = Color(0xFFAD2B38)

    /** Button: Hovered, Validation (error) text */
    val red_4 = Color(0xFFDB3B4B)

    /** Banner stroke, Diff conflict: Stroke, Diff conflict: Word */
    val red_10 = Color(0xFFFFF2F3)
}

