package com.nibbli.nibbligo.feature.pet.ui.pixel

/** Authentic P1-style 128×64 mono LCD (ArduinoGotchi / SSD1306 layout). */
object P1DisplaySpec {
    const val LCD_WIDTH_PX = 128
    const val LCD_HEIGHT_PX = 64
    const val LCD_SCALE = 2f

    /** Pet draw zone — upper portion of LCD. */
    const val PET_ZONE_TOP_PX = 4
    const val PET_ZONE_HEIGHT_PX = 36

    /** Bottom strip for stat icons. */
    const val ICON_STRIP_TOP_PX = 48
    const val ICON_SIZE_PX = 16
    const val ICON_SCALE = 1f

    /** Menu label band above icons. */
    const val MENU_BAND_TOP_PX = 40
    const val MENU_BAND_HEIGHT_PX = 8
}

enum class P1StatIcon {
    HUNGER,
    HAPPY,
    ENERGY,
    SICK,
    MESS,
}

/** Original mono icons — cute kawaii-style stat glyphs. */
object P1StatIconBitmaps {
    private val hunger = arrayOf(
        "................",
        ".....####.......",
        "....######......",
        "...########.....",
        "..##########....",
        "...########.....",
        "....######......",
        ".....####.......",
        "......##........",
        "......##........",
        "......##........",
        "......##........",
        "......##........",
        "......##........",
        "......##........",
        "................",
    )

    private val happy = arrayOf(
        "................",
        "....######......",
        "...########.....",
        "..##########....",
        ".####....####...",
        "####......####..",
        "###..####..###..",
        "###........###..",
        "####......####..",
        ".####....####...",
        "..##########....",
        "...########.....",
        "....######......",
        "................",
        "................",
        "................",
    )

    private val energy = arrayOf(
        ".......##.......",
        "......####......",
        ".....######.....",
        "....########....",
        "...##########...",
        "......####......",
        ".....######.....",
        "....########....",
        "...##########...",
        "......####......",
        "......####......",
        ".......##.......",
        ".......##.......",
        ".......##.......",
        ".......##.......",
        "................",
    )

    private val sick = arrayOf(
        "................",
        ".....####.......",
        "....######......",
        "...########.....",
        "...########.....",
        "....######......",
        ".....####.......",
        "......##........",
        "......##........",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    )

    private val mess = arrayOf(
        "................",
        "......###.......",
        ".....#####......",
        "....#######.....",
        ".....#####......",
        "......###.......",
        ".......#........",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    )

    fun bitmap(icon: P1StatIcon): Array<String> = when (icon) {
        P1StatIcon.HUNGER -> hunger
        P1StatIcon.HAPPY -> happy
        P1StatIcon.ENERGY -> energy
        P1StatIcon.SICK -> sick
        P1StatIcon.MESS -> mess
    }
}
