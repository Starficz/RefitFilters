package org.starficz.refitfilters

import org.starficz.starui.mutableStateOf
import org.starficz.starui.UIState

/** The filters' state, shared by every picker of one kind. Each state is declared once with its default; [reset] restores them all. */
abstract class FilterData(defaultLowerRange: () -> Float, defaultUpperRange: () -> Float) {
    private val resets = mutableListOf<() -> Unit>()

    /** A state that [reset] sets back to [default] (read again at each reset, so changed settings apply). */
    protected fun <T> state(default: () -> T): UIState<T> =
        mutableStateOf(default()).also { state -> resets += { state.value = default() } }

    protected fun <T> state(default: T): UIState<T> = state { default }

    val kineticDamage = state(true)
    val heDamage = state(true)
    val energyDamage = state(true)
    val fragDamage = state(true)
    val lowerRange = state(defaultLowerRange)
    val upperRange = state(defaultUpperRange)
    val currentSearch = state("")

    fun reset() = resets.forEach { it() }
}

class WeaponFilterData : FilterData({ RFSettings.weaponMinRange.toFloat() }, { RFSettings.weaponMaxRange.toFloat() }) {
    val projectileWeapons = state(true)
    val beamWeapons = state(true)
    val pdWeapons = state(true)
    val nonpdWeapons = state(true)
    val ammoWeapons = state(true)
    val nonAmmoWeapons = state(true)

    // The stat filters the slider can show besides range; each keeps its own limits, and all of them filter at once.
    val dpsLower = state(StatScale.DPS.min)
    val dpsUpper = state(StatScale.DPS.max)
    val dmgLower = state(StatScale.DMG.min)
    val dmgUpper = state(StatScale.DMG.max)
    val effLower = state(StatScale.EFF.min)
    val effUpper = state(StatScale.EFF.max)

    // How the list is ordered and which stat the slider shows; RESET FILTERS puts these back too (vanilla's order, range).
    val sortKey = state(SortKey.OP)
    val sortDescending = state(true)
    val sliderStat = state(SliderStat.RANGE)
}

class FighterFilterData : FilterData({ RFSettings.fighterMinRange.toFloat() }, { RFSettings.fighterMaxRange.toFloat() }) {
    val supportWing = state(true)
    val fighterWing = state(true)
    val bomberWing = state(true)
    val interceptorWing = state(true)
}

/** What the weapon list can be sorted by. OP, highest first, is vanilla's own order. */
enum class SortKey(val label: String, val descendingFirst: Boolean, val tooltip: String) {
    NAME("NAME", false, "Sort by name."),
    OP("OP", true, "Sort by ordnance point cost (vanilla's order)."),
    RANGE("RANGE", true, "Sort by base range."),
    DMG("DMG", true, "Sort by damage per shot."),
    DPS("DPS", true, "Sort by sustained damage per second."),
    EFF("EFF", true, "Sort by flux efficiency: damage per point of flux."),
}

/** The stats the weapon slider can filter by. */
enum class SliderStat(val label: String) { RANGE("RANGE"), DPS("DPS"), DMG("DMG"), EFF("EFF") }

/**
 * The fixed slider scales of the derived stats (range's comes from the settings). The top of each scale means no
 * upper limit, so the few weapons beyond it stay reachable: on 0.98.5a, damage per shot runs to 6000 (torpedoes) with
 * 95% at or under 4000, sustained DPS to 1500, and damage per flux to 25 with 90% at or under 3.
 */
enum class StatScale(val min: Float, val max: Float, val step: Float) {
    DPS(0f, 1500f, 100f),
    DMG(0f, 2000f, 100f),
    EFF(0f, 2f, 0.1f),
}
