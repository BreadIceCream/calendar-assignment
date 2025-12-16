package com.example.calendar.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors - 现代蓝色调
val Blue500 = Color(0xFF2196F3)
val Blue600 = Color(0xFF1E88E5)
val Blue700 = Color(0xFF1976D2)

// Secondary Colors - 温暖的橙色
val Orange500 = Color(0xFFFF9800)
val Orange600 = Color(0xFFFB8C00)

// Surface Colors
val SurfaceLight = Color(0xFFFFFBFE)
val SurfaceDark = Color(0xFF1C1B1F)

// Background Colors
val BackgroundLight = Color(0xFFF8F9FA)
val BackgroundDark = Color(0xFF121212)

// Text Colors
val OnPrimaryLight = Color.White
val OnPrimaryDark = Color.White
val OnSurfaceLight = Color(0xFF1C1B1F)
val OnSurfaceDark = Color(0xFFE6E1E5)

// Calendar specific colors
val TodayBackground = Color(0xFF2196F3)
val TodayText = Color.White
val SelectedBackground = Color(0xFFE3F2FD)
val SelectedBorder = Color(0xFF2196F3)
val WeekendText = Color(0xFFE57373)
val LunarText = Color(0xFF9E9E9E)
val FestivalText = Color(0xFFF44336)
val SolarTermText = Color(0xFF4CAF50)

// Event Colors (用户可选)
val EventColors = listOf(
    Color(0xFFF44336), // Red
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF3F51B5), // Indigo
    Color(0xFF2196F3), // Blue
    Color(0xFF03A9F4), // Light Blue
    Color(0xFF00BCD4), // Cyan
    Color(0xFF009688), // Teal
    Color(0xFF4CAF50), // Green
    Color(0xFF8BC34A), // Light Green
    Color(0xFFCDDC39), // Lime
    Color(0xFFFFEB3B), // Yellow
    Color(0xFFFFC107), // Amber
    Color(0xFFFF9800), // Orange
    Color(0xFFFF5722), // Deep Orange
)

// Event color hex strings
val EventColorHexList = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
    "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
)

fun hexToColor(hex: String?): Color {
    if (hex == null) return Blue500
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Blue500
    }
}