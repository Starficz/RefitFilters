package org.starficz.refitfilters

import org.starficz.UIFramework.Flag

var FilterData = FilterDataClass()

data class FilterDataClass(
    var kineticDamage: Flag = Flag(),
    var heDamage: Flag = Flag(),
    var energyDamage: Flag = Flag(),
    var fragDamage: Flag = Flag(),
    var projectileWeapons: Flag = Flag(),
    var beamWeapons: Flag = Flag(),
    var pdWeapons: Flag = Flag(),
    var nonpdWeapons: Flag = Flag(),
    var ammoWeapons: Flag = Flag(),
    var nonAmmoWeapons: Flag = Flag(),
    var lowerRange: Float  = 0f,
    var upperRange: Float  = 1500F,
    var currentSearch: String = ""
)


