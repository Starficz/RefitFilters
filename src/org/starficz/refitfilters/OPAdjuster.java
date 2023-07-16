package org.starficz.refitfilters;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class OPAdjuster extends BaseHullMod {
    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!ship.getVariant().hasHullMod("RF_MainGUI")){
            ship.getVariant().removeMod(id);
        }
    }
}
