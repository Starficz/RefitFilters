package org.starficz.refitfilters

import org.starficz.starui.UIState
import org.starficz.starui.mutableStateOf

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
}

class FighterFilterData : FilterData({ RFSettings.fighterMinRange.toFloat() }, { RFSettings.fighterMaxRange.toFloat() }) {
    val supportWing = state(true)
    val fighterWing = state(true)
    val bomberWing = state(true)
    val interceptorWing = state(true)
}
