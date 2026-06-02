package org.starficz.refitfilters

import org.starficz.staruiframework.UIState

abstract class FilterData(
    val kineticDamage: UIState<Boolean> = UIState(true),
    val heDamage: UIState<Boolean> = UIState(true),
    val energyDamage: UIState<Boolean> = UIState(true),
    val fragDamage: UIState<Boolean> = UIState(true),
    val lowerRange: UIState<Float>,
    val upperRange: UIState<Float>,
    val currentSearch: UIState<String> = UIState("")
) {
    protected fun resetCommonFields(defaultLower: Float, defaultUpper: Float) {
        kineticDamage.value = true
        heDamage.value = true
        energyDamage.value = true
        fragDamage.value = true
        lowerRange.value = defaultLower
        upperRange.value = defaultUpper
        currentSearch.value = ""
    }

    abstract fun reset()
}

class WeaponFilterData: FilterData(
    lowerRange = UIState(RFSettings.weaponMinRange.toFloat()),
    upperRange = UIState(RFSettings.weaponMaxRange.toFloat())
) {
    val projectileWeapons = UIState(true)
    val beamWeapons = UIState(true)
    val pdWeapons = UIState(true)
    val nonpdWeapons = UIState(true)
    val ammoWeapons = UIState(true)
    val nonAmmoWeapons = UIState(true)

    override fun reset() {
        resetCommonFields(RFSettings.weaponMinRange.toFloat(), RFSettings.weaponMaxRange.toFloat())
        projectileWeapons.value = true
        beamWeapons.value = true
        pdWeapons.value = true
        nonpdWeapons.value = true
        ammoWeapons.value = true
        nonAmmoWeapons.value = true
    }
}

class FighterFilterData: FilterData(
    lowerRange = UIState(RFSettings.fighterMinRange.toFloat()),
    upperRange = UIState(RFSettings.fighterMaxRange.toFloat())
) {
    val supportWing = UIState(true)
    val fighterWing = UIState(true)
    val bomberWing = UIState(true)
    val interceptorWing = UIState(true)

    override fun reset() {
        resetCommonFields(RFSettings.fighterMinRange.toFloat(), RFSettings.fighterMaxRange.toFloat())
        supportWing.value = true
        fighterWing.value = true
        bomberWing.value = true
        interceptorWing.value = true
    }
}