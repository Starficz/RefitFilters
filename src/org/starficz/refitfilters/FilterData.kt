package org.starficz.refitfilters

import org.starficz.UIFramework.Flag

abstract class FilterData(
    var kineticDamage: Flag = Flag(),
    var heDamage: Flag = Flag(),
    var energyDamage: Flag = Flag(),
    var fragDamage: Flag = Flag(),
    var lowerRange: Float,
    var upperRange: Float,
    var currentSearch: String = ""
) {
    protected fun resetCommonFields(defaultLower: Float, defaultUpper: Float) {
        kineticDamage = Flag()
        heDamage = Flag()
        energyDamage = Flag()
        fragDamage = Flag()
        lowerRange = defaultLower
        upperRange = defaultUpper
        currentSearch = ""
    }

    abstract fun reset()
}

class WeaponFilterData: FilterData(
    lowerRange = RFSettings.weaponMinRange.toFloat(),
    upperRange = RFSettings.weaponMaxRange.toFloat()
) {
    var projectileWeapons: Flag = Flag()
    var beamWeapons: Flag = Flag()
    var pdWeapons: Flag = Flag()
    var nonpdWeapons: Flag = Flag()
    var ammoWeapons: Flag = Flag()
    var nonAmmoWeapons: Flag = Flag()

    override fun reset() {
        resetCommonFields(RFSettings.weaponMinRange.toFloat(), RFSettings.weaponMaxRange.toFloat())
        projectileWeapons = Flag()
        beamWeapons = Flag()
        pdWeapons = Flag()
        nonpdWeapons = Flag()
        ammoWeapons = Flag()
        nonAmmoWeapons = Flag()
    }
}

class FighterFilterData: FilterData(
    lowerRange = RFSettings.fighterMinRange.toFloat(),
    upperRange = RFSettings.fighterMaxRange.toFloat()
) {
    var supportWing: Flag = Flag()
    var fighterWing: Flag = Flag()
    var bomberWing: Flag = Flag()
    var interceptorWing: Flag = Flag()

    override fun reset() {
        resetCommonFields(RFSettings.fighterMinRange.toFloat(), RFSettings.fighterMaxRange.toFloat())
        supportWing = Flag()
        fighterWing = Flag()
        bomberWing = Flag()
        interceptorWing = Flag()
    }
}

