package xueluoanping.fluiddrawerslegacy.block;

import com.jaquadro.minecraft.storagedrawers.api.storage.EmptyDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributesModifiable;
import com.jaquadro.minecraft.storagedrawers.api.storage.INetworked;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import com.jaquadro.minecraft.storagedrawers.capabilities.CapabilityDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.config.CommonConfig;
import com.jaquadro.minecraft.storagedrawers.core.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.network.NetworkHooks;
import xueluoanping.fluiddrawerslegacy.FluidDrawersLegacyMod;
import xueluoanping.fluiddrawerslegacy.ModContents;
import xueluoanping.fluiddrawerslegacy.block.tileentity.TileEntityFluidDrawer;
import xueluoanping.fluiddrawerslegacy.client.gui.ContainerFluiDrawer;

import javax.annotation.Nullable;
import java.util.EnumSet;


public class BlockFluidDrawer extends HorizontalDirectionalBlock implements INetworked, EntityBlock {
    public static final VoxelShape center = Block.box(1, 1, 1, 15, 15, 15);
    public static final VoxelShape base = Block.box(0, 0, 0, 16, 1, 16);
    public static final VoxelShape column1 = Block.box(0, 1, 0, 1, 15, 1);
    public static final VoxelShape column2 = Block.box(15, 1, 0, 16, 15, 1);
    public static final VoxelShape column3 = Block.box(0, 1, 15, 1, 15, 16);
    public static final VoxelShape column4 = Block.box(15, 1, 15, 16, 15, 16);
    public static final VoxelShape top = Block.box(0, 15, 0, 16, 16, 16);

    
//    public FluidDrawer(int drawerCount, boolean halfDepth, int storageUnits, Properties properties) {
//        super(properties);
//    }

    public BlockFluidDrawer(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return Shapes.or(center, base, column1, column2, column3, column4, top);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (hit.getDirection() == Direction.UP || hit.getDirection() == Direction.DOWN)
            return InteractionResult.PASS;

//        FluidDrawersLegacyMod.logger("ss2s22");
        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof TileEntityFluidDrawer) {
            TileEntityFluidDrawer tile = (TileEntityFluidDrawer) tileEntity;
            ItemStack heldStack = player.getItemInHand(hand);
            ItemStack offhandStack = player.getOffhandItem();
//            FluidDrawersLegacyMod.logger("hello，screen" + world + player.isShiftKeyDown());
            if (heldStack.isEmpty() && player.isShiftKeyDown()) {
                if (CommonConfig.GENERAL.enableUI.get() && !world.isClientSide()) {
//                    FluidDrawersLegacyMod.logger("hello，screen");
                    NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return new TranslatableComponent("gui.fluiddrawerslegacy.tittle");
                        }

                        @Nullable
                        @Override
                        public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player playerEntity) {

                            return new ContainerFluiDrawer(ModContents.containerType,windowId, playerInv, tile);
                        }
                    }, extraData -> {
                        extraData.writeBlockPos(pos);
                    });
                    return InteractionResult.SUCCESS;
                }
            }
            if (offhandStack == ItemStack.EMPTY) {
                if (heldStack.getItem() instanceof BucketItem) {
                    BucketItem bucketItem = (BucketItem) heldStack.getItem();
                    if (bucketItem.getFluid() == Fluids.EMPTY && tile.getTankFLuid().getAmount() >= FluidAttributes.BUCKET_VOLUME) {
                        tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN)
                                .ifPresent(handler -> {
                                    FluidStack fluidStack = handler.drain(new FluidStack(tile.getTankFLuid().getFluid(), FluidAttributes.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
                                    Fluid fluid = fluidStack.getFluid();
                                    if (heldStack.getCount() > 1) {
                                        if (!player.addItem(new ItemStack(fluid.getBucket())))

                                            Containers.dropItemStack(world, player.getX(), player.getY(), player.getZ(), new ItemStack(fluid.getBucket()));
                                        if (!player.isCreative())
                                            heldStack.shrink(1);
                                    } else {
                                        if (!player.isCreative()) {
                                            player.setItemInHand(hand, ItemUtils.createFilledResult(heldStack, player, new ItemStack(fluid.getBucket())));
                                        } else {
//                                            player.addItem(new ItemStack(fluid.getBucket()));
                                        }
                                    }
                                });
                    } else if (tile.hasNoFluid()) {
                        tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN)
                                .ifPresent(handler -> {
                                    handler.fill(new FluidStack(bucketItem.getFluid(), FluidAttributes.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
                                    if (!player.isCreative())
                                        player.setItemInHand(hand, heldStack.getContainerItem());
                                });
                        return InteractionResult.SUCCESS;
                    } else {
                        if (tile.getTankFLuid().getAmount() + FluidAttributes.BUCKET_VOLUME <= tile.getEffectiveCapacity()
                                && tile.getTankFLuid().getFluid() == bucketItem.getFluid()) {
                            tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN)
                                    .ifPresent(handler -> {
                                        handler.fill(new FluidStack(bucketItem.getFluid(), FluidAttributes.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);

                                    });
                            if (!player.isCreative())
                                player.setItemInHand(hand, heldStack.getContainerItem());
                            return InteractionResult.SUCCESS;
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
//                Maybe so simple
                else if (FluidUtil.interactWithFluidHandler(player, hand, tile.getTank())) {
                    return InteractionResult.SUCCESS;
                } else if (heldStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).isPresent()) {
                    heldStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY)
                            .ifPresent((handler) -> {

                                if (tile.hasNoFluid()) {
                                    if (tile.getEffectiveCapacity() > handler.getTankCapacity(0)) {
                                        tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN)
                                                .ifPresent(TEhandler -> {
                                                    TEhandler.fill(handler.drain(handler.getTankCapacity(0), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                                                });

                                    } else {
                                        tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN)
                                                .ifPresent(TEhandler -> {
                                                    TEhandler.fill(handler.drain(tile.getEffectiveCapacity(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                                                });
                                    }
                                } else if (tile.getTankFLuid().getFluid() == handler.drain(1, IFluidHandler.FluidAction.SIMULATE).getFluid()) {
                                    if (tile.getEffectiveCapacity() < handler.getTankCapacity(0) + tile.getTankFLuid().getAmount()) {
                                        tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN)
                                                .ifPresent(TEhandler -> {
                                                    TEhandler.fill(handler.drain(handler.getTankCapacity(0), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
//                                               if(!player.isCreative())

                                                });
                                    } else {
                                        tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN)
                                                .ifPresent(TEhandler -> {
                                                    TEhandler.fill(handler.drain(tile.getEffectiveCapacity() - tile.getTankFLuid().getAmount(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                                                });
                                    }
                                }

                            });
                    return InteractionResult.SUCCESS;
                }

            }

        }
        return super.use(state,world,pos,player,hand,hit);
    }




    @Override
    public RenderShape getRenderShape(BlockState p_149645_1_) {
        return RenderShape.MODEL;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = ModContents.itemBlock.asItem().getDefaultInstance();
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (tileEntity instanceof TileEntityFluidDrawer) {
            TileEntityFluidDrawer tile = (TileEntityFluidDrawer) tileEntity;
            final FluidStack[] fluidStackDown = new FluidStack[1];
            tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN)
                    .ifPresent(handler -> {
                        fluidStackDown[0] = handler.getFluidInTank(0);
                        CompoundTag nbt = new CompoundTag();
                        handler.getFluidInTank(0).writeToNBT(nbt);
                        stack.addTagElement("tank", ((TileEntityFluidDrawer.betterFluidHandler)handler).serializeNBT());

                    });
            stack.addTagElement("Upgrades", tile.getUpdateTag().get("Upgrades"));


            EnumSet<LockAttribute> attrs = EnumSet.noneOf(LockAttribute.class);
            if (((IDrawerAttributesModifiable) tile.getDrawerAttributes()).isItemLocked(LockAttribute.LOCK_EMPTY))
                attrs.add(LockAttribute.LOCK_EMPTY);
            if (((IDrawerAttributesModifiable) tile.getDrawerAttributes()).isItemLocked(LockAttribute.LOCK_POPULATED))
                attrs.add(LockAttribute.LOCK_POPULATED);
            if (!attrs.isEmpty()) {

                stack.getOrCreateTag().putByte("Lock", (byte) LockAttribute.getBitfield(attrs));
            }

            if (((IDrawerAttributesModifiable) tile.getDrawerAttributes()).isConcealed())
                stack.getOrCreateTag().putBoolean("Shr", true);

            if (((IDrawerAttributesModifiable) tile.getDrawerAttributes()).isShowingQuantity())
                stack.getOrCreateTag().putBoolean("Qua", true);

        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (tileEntity instanceof TileEntityFluidDrawer) {
            TileEntityFluidDrawer tile = (TileEntityFluidDrawer) tileEntity;
            if (stack.getOrCreateTag().contains("tank")) {
                tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN)
                        .ifPresent(handler -> {
                            FluidStack fluidStack = FluidStack.loadFluidStackFromNBT((CompoundTag) stack.getOrCreateTag().get("tank"));
                            handler.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
//                        FluidDrawersLegacyMod.logger(level.toString()+fluidStack.getDisplayName());
                        });
            }
            if (stack.getTag().contains("Upgrades")) {
                CompoundTag nbt = new CompoundTag();
                nbt.put("Upgrades", stack.getTag().get("Upgrades"));
                tile.upgrades().read(nbt);
            }
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains("Lock")) {
                EnumSet<LockAttribute> attrs = LockAttribute.getEnumSet(tag.getByte("Lock"));
                if (attrs != null) {
                    ((IDrawerAttributesModifiable) tile.getDrawerAttributes()).setItemLocked(LockAttribute.LOCK_EMPTY, attrs.contains(LockAttribute.LOCK_EMPTY));
                    ((IDrawerAttributesModifiable) tile.getDrawerAttributes()).setItemLocked(LockAttribute.LOCK_POPULATED, attrs.contains(LockAttribute.LOCK_POPULATED));
                }
            } else {
                ((IDrawerAttributesModifiable) tile.getDrawerAttributes()).setItemLocked(LockAttribute.LOCK_EMPTY, false);
                ((IDrawerAttributesModifiable) tile.getDrawerAttributes()).setItemLocked(LockAttribute.LOCK_POPULATED, false);
            }
            if (stack.getTag().contains("Shr")) {
                ((IDrawerAttributesModifiable) tile.getDrawerAttributes()).setIsConcealed(tag.getBoolean("Shr"));
            } else {
                ((IDrawerAttributesModifiable) tile.getDrawerAttributes()).setIsConcealed(false);
            }
            if (stack.getTag().contains("Qua")) {
                ((IDrawerAttributesModifiable) tile.getDrawerAttributes()).setIsShowingQuantity(tag.getBoolean("Qua"));
            } else {
                ((IDrawerAttributesModifiable) tile.getDrawerAttributes()).setIsShowingQuantity(false);
            }
            if (entity != null && entity.getOffhandItem().getItem() == ModItems.DRAWER_KEY) {
                IDrawerAttributes _attrs = (IDrawerAttributes) tile.getCapability(CapabilityDrawerAttributes.DRAWER_ATTRIBUTES_CAPABILITY).orElse(new EmptyDrawerAttributes());
                if (_attrs instanceof IDrawerAttributesModifiable) {
                    IDrawerAttributesModifiable attrs = (IDrawerAttributesModifiable) _attrs;
                    attrs.setItemLocked(LockAttribute.LOCK_EMPTY, true);
                    attrs.setItemLocked(LockAttribute.LOCK_POPULATED, true);

                }
            }
        }

        super.setPlacedBy(level, pos, state, entity, stack);
    }


    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level,pos,state);
        level.playSound(null, pos, Fluids.WATER.getAttributes().getEmptySound(), SoundSource.BLOCKS, 1.0F, 1.0F);

    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }


    @Override
    public int getSignal(BlockState state, BlockGetter blockAccess, BlockPos pos, Direction side) {
        if (!this.isSignalSource(state)||!(blockAccess.getBlockEntity(pos) instanceof TileEntityFluidDrawer)) {
            return 0;
        } else {
            TileEntityFluidDrawer tile = (TileEntityFluidDrawer) blockAccess.getBlockEntity(pos);
//            FluidDrawersLegacyMod.logger("get"+tile.isRedstone()+tile.getRedstoneLevel() +tile.upgrades().serializeNBT());
            return tile != null && tile.isRedstone() ? tile.getRedstoneLevel() : 0;
        }
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter worldIn, BlockPos pos, Direction side) {
        return side == Direction.UP ? this.getSignal(state, worldIn, pos, side):0 ;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFluidDrawer(pos,state);
    }
}
