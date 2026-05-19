package com.zeekrbaha.fridgechef.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeekrbaha.fridgechef.R
import com.zeekrbaha.fridgechef.data.ThemePreference

val PaperLight = Color(0xFFF5F1E8)
val Paper2Light = Color(0xFFEFE9DC)
val InkLight = Color(0xFF2C2A26)
val InkSoftLight = Color(0xFF6B6862)
val RuleLight = Color(0xFFDDD7CA)
val SageLight = Color(0xFF87A878)
val TerracottaLight = Color(0xFFC97B5C)

val PaperDark = Color(0xFF1C1A16)
val Paper2Dark = Color(0xFF26231E)
val InkDark = Color(0xFFF0ECE4)
val InkSoftDark = Color(0xFFA8A39A)
val RuleDark = Color(0xFF3A3631)
val SageDark = Color(0xFFA3C594)
val TerracottaDark = Color(0xFFE09275)

val Fraunces = FontFamily(
    Font(R.font.fraunces_72pt_regular, FontWeight.Normal),
    Font(R.font.fraunces_72pt_bold, FontWeight.Bold),
)

val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
)

object Space {
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val s48 = 48.dp
}

data class FridgeChefColors(
    val paper: Color,
    val paper2: Color,
    val ink: Color,
    val inkSoft: Color,
    val rule: Color,
    val sage: Color,
    val terracotta: Color,
)

val ColorScheme.fridgeChef: FridgeChefColors
    @Composable get() = if (isSystemInDarkTheme()) darkFridgeColors else lightFridgeColors

private val lightFridgeColors = FridgeChefColors(PaperLight, Paper2Light, InkLight, InkSoftLight, RuleLight, SageLight, TerracottaLight)
private val darkFridgeColors = FridgeChefColors(PaperDark, Paper2Dark, InkDark, InkSoftDark, RuleDark, SageDark, TerracottaDark)

@Composable
fun FridgeChefTheme(themePreference: ThemePreference, content: @Composable () -> Unit) {
    val dark = when (themePreference) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    val colors = if (dark) darkFridgeColors else lightFridgeColors
    val scheme = if (dark) {
        darkColorScheme(
            background = colors.paper,
            surface = colors.paper,
            surfaceVariant = colors.paper2,
            primary = colors.sage,
            secondary = colors.terracotta,
            onBackground = colors.ink,
            onSurface = colors.ink,
        )
    } else {
        lightColorScheme(
            background = colors.paper,
            surface = colors.paper,
            surfaceVariant = colors.paper2,
            primary = colors.sage,
            secondary = colors.terracotta,
            onBackground = colors.ink,
            onSurface = colors.ink,
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(
            displayLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 34.sp),
            titleLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 22.sp),
            titleMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 20.sp),
            bodyLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 17.sp),
            bodyMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 15.sp),
            bodySmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
            labelMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 13.sp),
            labelSmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        ),
        content = content,
    )
}
