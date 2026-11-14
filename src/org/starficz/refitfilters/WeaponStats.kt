package org.starficz.refitfilters

import com.fs.starfarer.api.loading.FighterWingSpecAPI
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
        SortKey.OP -> spec.getOrdnancePointCost(null)   // base cost; skills change every weapon of a size alike
        SortKey.RANGE -> spec.maxRange
        SortKey.DMG -> damagePerShot(spec)
        SortKey.DPS -> dps(spec)
        SortKey.EFF -> efficiency(spec)
        else -> 0f
    }

    /** Sorts the weapon picker's list by [key]; see [PickerSort.sort]. */
    fun sort(items: MutableList<Any?>, key: SortKey, descending: Boolean) = PickerSort.sort(items, key, descending) { item ->
        (item as? AutofitPlugin.AvailableWeapon)?.let { weapon ->
            PickerSort.Entry(weapon.price, weapon.spec.weaponName, value(key, weapon.spec))
        }
    }
}

/** The wing stats the fighter picker's sort row and stat slider use. */
object WingStats {
    fun refitTime(spec: FighterWingSpecAPI): Float = spec.refitTime
    fun size(spec: FighterWingSpecAPI): Float = spec.numFighters.toFloat()

    private fun value(key: SortKey, spec: FighterWingSpecAPI): Float = when (key) {
        SortKey.OP -> spec.getOpCost(null)   // base cost, as for weapons
        SortKey.RANGE -> spec.range
        SortKey.REFIT -> refitTime(spec)
        SortKey.SIZE -> size(spec)
        else -> 0f
    }

    /** Sorts the fighter picker's list by [key]; see [PickerSort.sort]. */
    fun sort(items: MutableList<Any?>, key: SortKey, descending: Boolean) = PickerSort.sort(items, key, descending) { item ->
        (item as? AutofitPlugin.AvailableFighter)?.let { wing ->
            PickerSort.Entry(wing.price, wing.wingSpec.wingName, value(key, wing.wingSpec))
        }
    }
}

object PickerSort {
    /** What the sort needs of one picker item: its price (vanilla groups free ones first), name and sort value. */
    class Entry(price: Float, val name: String, val value: Float) {
        val group: Int = if (price == 0f) 0 else 1
    }

    /**
     * Sorts a picker's list by [key]. Vanilla's own order is OP, highest first, so that one is left as vanilla made
     * it. Every other order keeps vanilla's first grouping, items from your cargo (free) before ones you would buy,
     * and is stable, so equal values keep vanilla's order. Items [entry] does not know go last, in their order.
     */
    fun sort(items: MutableList<Any?>, key: SortKey, descending: Boolean, entry: (Any?) -> Entry?) {
        if (key == SortKey.OP && descending) return
        val order = Comparator<Pair<Any?, Entry?>> { (_, a), (_, b) ->
            if (a == null || b == null) return@Comparator (if (a == null) 1 else 0) - (if (b == null) 1 else 0)
            if (a.group != b.group) return@Comparator a.group - b.group
            val byKey = if (key == SortKey.NAME) String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name)
                        else a.value.compareTo(b.value)
            if (descending) -byKey else byKey
        }
        val sorted = items.map { it to entry(it) }.sortedWith(order)
        items.clear()
        items.addAll(sorted.map { it.first })
    }
}
