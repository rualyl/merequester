package com.almostreliable.merequester.requester.status;

import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.CraftingSubmitErrorCode;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.ticking.TickRateModulation;
import com.almostreliable.merequester.requester.RequesterBlockEntity;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class PlanState implements StatusState {

    private final boolean wasMissing;
    private final Future<? extends ICraftingPlan> future;

    PlanState(boolean wasMissing, Future<? extends ICraftingPlan> future) {
        this.wasMissing = wasMissing;
        this.future = future;
    }

    static PlanState BuildPlan(RequesterBlockEntity host, int slot, boolean wasMissing)
    {
        // Once BuildPlan is invoked, we're crafting no matter what, rather than returning a null PlanState
        var amountToCraft = Math.max(1, host.getStorageManager().computeAmountToCraft(slot));
        var key = host.getRequests().getKey(slot);

        var future = host.getMainNodeGrid()
            .getCraftingService()
            .beginCraftingCalculation(
                host.getLevel(),
                host::getActionSource,
                key,
                amountToCraft,
                CalculationStrategy.CRAFT_LESS
            );

        return new PlanState(wasMissing, future);
    }

    @Override
    public StatusState handle(RequesterBlockEntity host, int index) {
        if (!future.isDone()) return this;
        if (future.isCancelled()) return StatusState.IDLE;

        try {
            var plan = future.get();
            var submitResult = host.getMainNodeGrid()
                .getCraftingService()
                .submitJob(plan, host, null, false, host.getActionSource());

            if (!submitResult.successful() || submitResult.link() == null) {
                if (submitResult.errorCode() == CraftingSubmitErrorCode.INCOMPLETE_PLAN &&
                    !plan.missingItems().isEmpty()) {
                    return StatusState.MISSING;
                }
                return StatusState.IDLE;
            }

            host.getStorageManager().get(index).setTotalAmount(plan.finalOutput().amount());
            return new LinkState(Objects.requireNonNull(submitResult.link()));
        } catch (InterruptedException | ExecutionException e) {
            return StatusState.IDLE;
        }
    }

    @Override
    public RequestStatus type() {
        return wasMissing ? RequestStatus.MISSING : RequestStatus.PLAN;
    }

    @Override
    public TickRateModulation getTickRateModulation() {
        return future.isDone() && !future.isCancelled() ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
    }
}
