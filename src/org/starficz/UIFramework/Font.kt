package org.starficz.UIFramework

// Custom UIPanelAPI extensions that reflect components made in a ToolTipMakerAPI onto a UIPanel
internal enum class Font {
    VICTOR_10,
    VICTOR_14,
    ORBITRON_20,
    ORBITRON_20_BOLD,
    ORBITRON_24,
    ORBITRON_24_BOLD
}

internal fun getFontPath(font: Font): String{
    return when(font){
        Font.VICTOR_10 -> "graphics/fonts/victor10.fnt"
        Font.VICTOR_14 -> "graphics/fonts/victor14.fnt"
        Font.ORBITRON_20 -> "graphics/fonts/orbitron20.fnt"
        Font.ORBITRON_20_BOLD -> "graphics/fonts/orbitron20bold.fnt"
        Font.ORBITRON_24 -> "graphics/fonts/orbitron24aa.fnt"
        Font.ORBITRON_24_BOLD -> "graphics/fonts/orbitron24aabold.fnt"
    }
}