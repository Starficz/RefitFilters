package org.starficz.refitfilters

import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.plugins.AutofitPlugin

/** The weapon stats the sort row and the stat slider use: the game's own derived stats, as the weapon tooltips show them. */
object WeaponStats {
    /** Damage per shot; linked barrels and MIRVs count all their projectiles. */
    fun damagePerShot(spec: WeaponSpecAPI): Float = spec.derivedStats.damagePerShot

    /** Sustained damage per second: burst weapons over their whole cycle, ammo-limited ones at their reload rate. */
    fun dps(spec: WeaponSpecAPI): Float = spec.derivedStats.sustainedDps

    /** Damage per point of flux. A damaging weapon that makes no flux is infinitely efficient; one that does no damage is 0. */
    fun efficiency(spec: WeaponSpecAPI): Float {
        val fluxPerDamage = spec.derivedStats.fluxPerDam
        return when {
            fluxPerDamage.isNaN() || damagePerShot(spec) <= 0f && dps(spec) <= 0f -> 0f
            fluxPerDamage <= 0f -> Float.POSITIVE_INFINITY
            else -> 1f / fluxPerDamage
        }
    }

    /** Outside [lower]..[upper], where a limit at the end of [scale] is no limit (so values beyond the scale stay in). */
    fun outside(value: Float, lower: Float, upper: Float, scale: StatScale): Boolean =
        (lower > scale.min && value < lower) || (upper < scale.max && value > upper)

    private fun value(key: SortKey, spec: WeaponSpecAPI): Float = when (key) {
        SortKey.NAME -> 0f
        SortKey.OP -> spec.getOrdnancePointCost(null)   // base cost; skills change every weapon of a size alike
        SortKey.RANGE -> spec.maxRange
        SortKey.DMG -> damagePerShot(spec)
        SortKey.DPS -> dps(spec)
        SortKey.EFF -> efficiency(spec)
    }

    /**
     * Sorts the picker's list by [key]. Vanilla's own order is OP, highest first, so that one is left as vanilla made
     * it. Every other order keeps vanilla's first grouping, weapons from your cargo (free) before ones you would buy,
     * and is stable, so equal values keep vanilla's order.
     */
    fun sort(items: MutableList<Any?>, key: SortKey, descending: Boolean) {
        if (key == SortKey.OP && descending) return
        val order = Comparator<Entry> { a, b ->
            if (a.group != b.group) return@Comparator a.group - b.group
            val specA = a.spec ?: return@Comparator 0
            val specB = b.spec ?: return@Comparator 0
            val byKey = if (key == SortKey.NAME) String.CASE_INSENSITIVE_ORDER.compare(specA.weaponName, specB.weaponName)
                        else value(key, specA).compareTo(value(key, specB))
            if (descending) -byKey else byKey
        }
        val sorted = items.map { Entry(it) }.sortedWith(order)
        items.clear()
        items.addAll(sorted.map { it.item })
    }

    private class Entry(val item: Any?) {
        private val weapon = item as? AutofitPlugin.AvailableWeapon
        val spec: WeaponSpecAPI? = weapon?.spec
        /** Vanilla's first grouping: weapons from your cargo (free) first; anything else after the weapons. */
        val group: Int = when {
            weapon == null -> 2
            weapon.price == 0f -> 0
            else -> 1
        }
    }
}
