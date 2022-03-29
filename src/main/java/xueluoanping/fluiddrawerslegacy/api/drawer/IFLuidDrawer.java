package xueluoanping.fluiddrawerslegacy.api.drawer;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

public interface IFLuidDrawer extends IDrawer {
    @Nullable
    FluidStack getStoredFluid();

    FluidDrawer setStoredFluid(@Nullable FluidStack fluid);

    default FluidDrawer setStoredFluidPrototype(@Nullable FluidStack fluid) {
        if (fluid == null) {
            return setStoredFluid(null);
        } else {
            FluidStack prototype = fluid.copy();
            prototype.setAmount(0);
            return setStoredFluid(prototype);
        }
    }

    default int insertFluid(@Nullable FluidStack fluid, boolean commit, boolean bypass) {
        if (fluid == null || fluid.getAmount() <= 0 || !(bypass || canFluidBeStored(fluid))) {
            return 0;
        }
        FluidStack stored = getStoredFluid();
        if (stored != null && !stored.isFluidEqual(fluid)) {
            return 0;
        }
        int toTransfer = Math.min(getAcceptingRemainingCapacity(), fluid.getAmount());
        if (commit && toTransfer > 0) {
            if (stored != null) {
                stored = stored.copy();
                if (stored.getAmount() > Integer.MAX_VALUE - toTransfer) {
                    stored.setAmount(Integer.MAX_VALUE);
                } else {
                    stored.setAmount(stored.getAmount()+toTransfer);
                }
            } else {
                stored = fluid.copy();
                stored.setAmount(toTransfer);
            }
            setStoredFluid(stored);
        }
        return toTransfer;
    }

    @Nullable
    default FluidStack extractFluid(@Nullable FluidStack fluid, boolean commit, boolean bypass) {
        if (fluid == null || fluid.getAmount() <= 0 || !(bypass || canFluidBeExtracted(fluid))) {
            return null;
        }
        FluidStack stored = getStoredFluid();
        if (stored == null || !stored.isFluidEqual(fluid)) {
            return null;
        }
        int toTransfer = Math.min(stored.getAmount(), fluid.getAmount());
        if (commit && toTransfer > 0) {
            stored = stored.copy();
            stored.setAmount(stored.getAmount()-toTransfer);
            setStoredFluid(stored);
        }
        fluid = stored.copy();
        fluid.setAmount(toTransfer);
        return fluid;
    }

    @Nullable
    default FluidStack extractFluid(int amount, boolean commit, boolean bypass) {
        if (amount <= 0) {
            return null;
        }
        FluidStack fluid = getStoredFluid();
        if (fluid == null) {
            return null;
        }
        fluid = fluid.copy();
        fluid.setAmount(amount);
        return extractFluid(fluid, commit, bypass);
    }

    default int getMaxCapacity() {
        return getMaxCapacity(getStoredFluid());
    }

    int getMaxCapacity(@Nullable FluidStack fluid);

    default int getAcceptingMaxCapacity(@Nullable FluidStack fluid) {
        return getMaxCapacity(fluid);
    }

    int getRemainingCapacity();

    default int getAcceptingRemainingCapacity() {
        return getRemainingCapacity();
    }

    boolean canFluidBeStored(@Nullable FluidStack fluid);

    boolean canFluidBeExtracted(@Nullable FluidStack fluid);

    default boolean isEmpty() {
        FluidStack fluid = getStoredFluid();
        return fluid == null || fluid.getAmount() <= 0;
    }
}
