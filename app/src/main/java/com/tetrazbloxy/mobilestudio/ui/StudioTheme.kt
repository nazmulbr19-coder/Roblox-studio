package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetrazbloxy.mobilestudio.core.Theme
import com.tetrazbloxy.mobilestudio.studio.StudioSession

/** Compose palette derived from the persisted core [Theme]. */
data class StudioPalette(
    val accent: Color,
    val bg: Color,
    val border: Color,
    val card: Color,
    val dim: Color,
    val editorBg: Color,
    val editorGutter: Color,
    val editorText: Color,
    val header: Color,
    val hover: Color,
    val input: Color,
    val light: Color,
    val panel: Color,
    val ribbon: Color,
    val rowAlt: Color,
    val select: Color,
    val text: Color,
    val topbar: Color,
    val error: Color,
    val info: Color,
    val warn: Color,
    val play: Color,
    val stop: Color
) {
    val isLight: Boolean get() = bg.luminance() > 0.55f

    companion object {
        fun from(theme: Theme): StudioPalette = StudioPalette(
            accent = Color(theme.accent),
            bg = Color(theme.bg),
            border = Color(theme.border),
            card = Color(theme.card),
            dim = Color(theme.dim),
            editorBg = Color(theme.editorBg),
            editorGutter = Color(theme.editorGutter),
            editorText = Color(theme.editorText),
            header = Color(theme.header),
            hover = Color(theme.hover),
            input = Color(theme.input),
            light = Color(theme.light),
            panel = Color(theme.panel),
            ribbon = Color(theme.ribbon),
            rowAlt = Color(theme.rowAlt),
            select = Color(theme.select),
            text = Color(theme.text),
            topbar = Color(theme.topbar),
            error = Color(theme.error),
            info = Color(theme.info),
            warn = Color(theme.warn),
            play = Color(theme.play),
            stop = Color(theme.stop)
        )
    }
}

fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

val LocalStudioPalette = staticCompositionLocalOf { StudioPalette.from(Theme.DARK) }

/** The single app session, provided by [StudioRoot] to nested composables. */
val LocalStudioSessionOwner = staticCompositionLocalOf<StudioSession?> { null }

val StudioShapes = RoundedCornerShape(14.dp)

private fun studioTypography(): Typography {
    val base = Typography()
    return base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
        bodyMedium = base.bodyMedium.copy(lineHeight = 20.sp),
        bodySmall = base.bodySmall.copy(lineHeight = 16.sp)
    )
}

@Composable
fun StudioTheme(theme: Theme = Theme.DARK, content: @Composable () -> Unit) {
    val palette = StudioPalette.from(theme)
    val scheme = if (palette.isLight) {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.panel,
            onSurface = palette.text,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.dim,
            outline = palette.border,
            error = palette.error
        )
    } else {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.panel,
            onSurface = palette.text,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.dim,
            outline = palette.border,
            error = palette.error
        )
    }

    CompositionLocalProvider(LocalStudioPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = studioTypography(),
            content = content
        )
    }
}

/** Code/editor text style. */
fun editorTextStyle(sizeSp: Float, color: Color): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * 1.45f).sp,
    color = color
)
