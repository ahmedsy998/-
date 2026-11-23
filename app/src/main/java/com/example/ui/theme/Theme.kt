package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

@Immutable
data class MasroofySemanticColors(
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val warning: Color
)

private val LightSemanticColors = MasroofySemanticColors(
    expense = ExpenseRedLight,
    expenseContainer = ExpenseContainerLight,
    onExpenseContainer = OnExpenseContainerLight,
    income = IncomeGreenLight,
    incomeContainer = IncomeContainerLight,
    onIncomeContainer = OnIncomeContainerLight,
    warning = WarningAmberLight
)

private val DarkSemanticColors = MasroofySemanticColors(
    expense = ExpenseRedDark,
    expenseContainer = ExpenseContainerDark,
    onExpenseContainer = OnExpenseContainerDark,
    income = IncomeGreenDark,
    incomeContainer = IncomeContainerDark,
    onIncomeContainer = OnIncomeContainerDark,
    warning = WarningAmberDark
)

val LocalMasroofyColors = staticCompositionLocalOf { LightSemanticColors }

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = OnEmeraldDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = OnSageDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = GoldTertiaryDark,
    onTertiary = OnGoldDark,
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = OnGoldContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLow = Color(0xFF141D1A),
    surfaceContainer = Color(0xFF192420),
    surfaceContainerHigh = Color(0xFF212E29),
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ExpenseRedDark,
    errorContainer = ExpenseContainerDark,
    onErrorContainer = OnExpenseContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = OnEmeraldLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = OnEmeraldContainerLight,
    secondary = SageSecondaryLight,
    onSecondary = OnSageLight,
    secondaryContainer = SageContainerLight,
    onSecondaryContainer = OnSageContainerLight,
    tertiary = GoldTertiaryLight,
    onTertiary = OnGoldLight,
    tertiaryContainer = GoldContainerLight,
    onTertiaryContainer = OnGoldContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLow = Color(0xFFF0F6F3),
    surfaceContainer = Color(0xFFEAF1EE),
    surfaceContainerHigh = Color(0xFFE3ECE8),
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ExpenseRedLight,
    errorContainer = ExpenseContainerLight,
    onErrorContainer = OnExpenseContainerLight
)

private val MasroofyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MasroofyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(
        LocalMasroofyColors provides semanticColors,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MasroofyTypography,
            shapes = MasroofyShapes,
            content = content
        )
    }
}
