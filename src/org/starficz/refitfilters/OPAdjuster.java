package org.starficz.refitfilters;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

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

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        if (amount > 0 && member.getVariant().hasHullMod("RF_OPAdjuster"))
            member.getVariant().removeMod("RF_OPAdjuster");
    }
}
