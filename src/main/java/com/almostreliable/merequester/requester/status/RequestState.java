package com.almostreliable.merequester.requester.status;

import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.ticking.TickRateModulation;
import com.almostreliable.merequester.requester.RequesterBlockEntity;

public class RequestState implements StatusState {

    RequestState() {}

    @Override
    public StatusState handle(RequesterBlockEntity host, int slot) {
        return PlanState.BuildPlan(host, slot, false);
    }

    @Override
    public RequestStatus type() {
        return RequestStatus.REQUEST;
    }

    @Override
    public TickRateModulation getTickRateModulation() {
        return TickRateModulation.SLOWER;
    }
}
