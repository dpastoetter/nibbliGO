package com.nibbli.nibbligo.feature.pet.ui.pixel

/** Authentic P1-style 128×64 mono LCD (ArduinoGotchi / SSD1306 layout). */
object P1DisplaySpec {
    const val LCD_WIDTH_PX = 128
    const val LCD_HEIGHT_PX = 64
    /** On-screen LCD is slightly taller than the authentic 2:1 ratio for readability. */
    const val LCD_DISPLAY_HEIGHT_SCALE = 1.125f
    const val LCD_SCALE = 2f

    val lcdAspectRatio: Float
        get() = LCD_WIDTH_PX / (LCD_HEIGHT_PX * LCD_DISPLAY_HEIGHT_SCALE)

    /** Pet draw zone — upper portion of LCD. */
    const val PET_ZONE_TOP_PX = 4
    const val PET_ZONE_HEIGHT_PX = 36

    /** Bottom strip for stat icons. */
    const val ICON_STRIP_TOP_PX = 48
    const val ICON_SIZE_PX = 16
    const val ICON_SCALE = 1f

    /** Menu label band above icons (care menu when idle). */
    const val MENU_BAND_TOP_PX = 40
    const val MENU_BAND_HEIGHT_PX = 8

    /** Scrolling LLM dialogue band — above stat icons, below pet. */
    const val DIALOGUE_ZONE_TOP_PX = 30
    const val DIALOGUE_ZONE_HEIGHT_PX = 18

    /** LLM talk mode: reply area above bottom strip. */
    const val BOTTOM_STRIP_TOP_PX = ICON_STRIP_TOP_PX
    const val BOTTOM_STRIP_SLOT_PX = ICON_SIZE_PX
    const val BOTTOM_STRIP_GAP_PX = 4
    const val TALK_PET_LEFT_PX = 2
    const val TALK_STAT_ICONS_START_PX =
        TALK_PET_LEFT_PX + BOTTOM_STRIP_SLOT_PX + BOTTOM_STRIP_GAP_PX

    const val TALK_REPLY_TOP_PX = 4
    const val TALK_REPLY_BOTTOM_PX = ICON_STRIP_TOP_PX - 2
    const val TALK_REPLY_LEFT_PX = TALK_STAT_ICONS_START_PX
    const val TALK_REPLY_RIGHT_PX = 4
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
