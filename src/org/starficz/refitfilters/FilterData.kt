package org.starficz.refitfilters

import org.starficz.starui.mutableStateOf
import org.starficz.starui.UIState

/** The filters' state, shared by every picker of one kind. Each state is declared once with its default; [reset] restores them all. */
abstract class FilterData(
    defaultUpperRange: () -> Float, defaultSortKey: () -> SortKey, defaultSortDescending: () -> Boolean,
    defaultSliderStat: () -> SliderStat
) {
    private val resets = mutableListOf<() -> Unit>()

    /** A state that [reset] sets back to [default] (read again at each reset, so changed settings apply). */
    protected fun <T> state(default: () -> T): UIState<T> =
        mutableStateOf(default()).also { state -> resets += { state.value = default() } }

    protected fun <T> state(default: T): UIState<T> = state { default }

    val kineticDamage = state(true)
    val heDamage = state(true)
    val energyDamage = state(true)
    val fragDamage = state(true)
    val lowerRange = state(0f)   // range starts at 0, like the slider's other stats
    val upperRange = state(defaultUpperRange)
    val currentSearch = state("")

    /** The sort row's keys and the stats the slider can show, in order; the first stat is range. */
    abstract val sortKeys: List<SortKey>
    abstract val sliderStats: List<SliderStat>

    // How the list is ordered and which stat the slider shows: the settings' defaults, which RESET FILTERS puts back.
    val sortKey = state(defaultSortKey)
    val sortDescending = state(defaultSortDescending)
    val sliderStat = state(defaultSliderStat)

    fun reset() = resets.forEach { it() }
}

class WeaponFilterData : FilterData(
    { RFSettings.weaponMaxRange.toFloat() }, { RFSettings.weaponDefaultSort }, { RFSettings.weaponDefaultSortDescending },
    { RFSettings.weaponDefaultSlider }
) {
    val projectileWeapons = state(true)
    val beamWeapons = state(true)
    val pdWeapons = state(true)
    val nonpdWeapons = state(true)
    val ammoWeapons = state(true)
    val nonAmmoWeapons = state(true)

    // The stat filters the slider can show besides range; each keeps its own limits, and all of them filter at once.
    val dpsLower = state(StatScale.DPS.min)
    val dpsUpper = state { StatScale.DPS.max }
    val dmgLower = state(StatScale.DMG.min)
    val dmgUpper = state { StatScale.DMG.max }
    val effLower = state(StatScale.EFF.min)
    val effUpper = state { StatScale.EFF.max }

    override val sortKeys = listOf(SortKey.NAME, SortKey.OP, SortKey.RANGE, SortKey.DMG, SortKey.DPS, SortKey.EFF)
    override val sliderStats = listOf(SliderStat.RANGE, SliderStat.DPS, SliderStat.DMG, SliderStat.EFF)
}

class FighterFilterData : FilterData(
    { RFSettings.fighterMaxRange.toFloat() }, { RFSettings.fighterDefaultSort }, { RFSettings.fighterDefaultSortDescending },
    { RFSettings.fighterDefaultSlider }
) {
    val supportWing = state(true)
    val fighterWing = state(true)
    val bomberWing = state(true)
    val interceptorWing = state(true)

    // The stat filters the slider can show besides range, as for weapons.
    val refitLower = state(StatScale.REFIT.min)
    val refitUpper = state { StatScale.REFIT.max }
    val sizeLower = state(StatScale.SIZE.min)
    val sizeUpper = state { StatScale.SIZE.max }

    override val sortKeys = listOf(SortKey.NAME, SortKey.OP, SortKey.RANGE, SortKey.REFIT, SortKey.SIZE)
    override val sliderStats = listOf(SliderStat.RANGE, SliderStat.REFIT, SliderStat.SIZE)
}

/** What the picker lists can be sorted by (each picker shows its own [FilterData.sortKeys]). OP, highest first, is vanilla's own order. */
enum class SortKey(val label: String, val descendingFirst: Boolean, val tooltip: String) {
    NAME("NAME", false, "Sort by name."),
    OP("OP", true, "Sort by ordnance point cost."),
    RANGE("RANGE", true, "Sort by base range."),
    DMG("DMG", true, "Sort by damage per shot."),
    DPS("DPS", true, "Sort by sustained damage per second."),
    EFF("EFF", true, "Sort by flux efficiency."),
    REFIT("REFIT", false, "Sort by fighter replacement time."),
    SIZE("SIZE", true, "Sort by wing size: fighters per wing."),
}

/** The stats the slider can filter by (weapons: range, DPS, DMG, EFF; fighters: range, REFIT, SIZE). */
enum class SliderStat(val label: String) { RANGE("RANGE"), DPS("DPS"), DMG("DMG"), EFF("EFF"), REFIT("REFIT"), SIZE("SIZE") }

/**
 * The slider scales of the derived stats: lower end and tick step here, the top a setting (RFSettings, read each time:
 * a changed setting applies to the next build of the slider and, for the filters, the next reset). The top of each
 * scale means no upper limit, so the few weapons beyond it stay reachable: on 0.98.5a, damage per shot runs to 6000
 * (torpedoes) with 95% at or under 4000, sustained DPS to 1500, and damage per flux to 25 with 90% at or under 3.
 * Wings: refit time runs from 3 to 25 seconds, wing size from 1 to 6 fighters.
 */
enum class StatScale(val min: Float, val step: Float, private val configuredMax: () -> Float) {
    DPS(0f, 100f, { RFSettings.weaponMaxDps.toFloat() }),
    DMG(0f, 100f, { RFSettings.weaponMaxDamage.toFloat() }),
    EFF(0f, 0.1f, { RFSettings.weaponMaxEfficiency.toFloat() }),
    REFIT(0f, 2f, { RFSettings.fighterMaxRefit.toFloat() }),
    SIZE(1f, 1f, { RFSettings.fighterMaxWingSize.toFloat() });

    val max: Float get() = configuredMax()
}
