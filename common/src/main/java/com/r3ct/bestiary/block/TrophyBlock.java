package com.r3ct.bestiary.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TrophyBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    private static final VoxelShape FLOOR_Z = Block.box(3.0D, 0.0D, 5.0D, 13.0D, 2.0D, 11.0D);
    private static final VoxelShape FLOOR_X = Block.box(5.0D, 0.0D, 3.0D, 11.0D, 2.0D, 13.0D);

    private static final VoxelShape WALL_NORTH = Block.box(3.0D, 0.0D, 10.0D, 13.0D, 2.0D, 16.0D);
    private static final VoxelShape WALL_SOUTH = Block.box(3.0D, 0.0D, 0.0D, 13.0D, 2.0D, 6.0D);
    private static final VoxelShape WALL_WEST = Block.box(10.0D, 0.0D, 3.0D, 16.0D, 2.0D, 13.0D);
    private static final VoxelShape WALL_EAST = Block.box(0.0D, 0.0D, 3.0D, 6.0D, 2.0D, 13.0D);

    public TrophyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.FLOOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.DOWN) {
            return null;
        }

        AttachFace face = clickedFace == Direction.UP ? AttachFace.FLOOR : AttachFace.WALL;
        Direction facing = face == AttachFace.WALL ? clickedFace : context.getHorizontalDirection().getOpposite();

        return this.defaultBlockState().setValue(FACE, face).setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TrophyBlockEntity trophyBE) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null && !customData.isEmpty()) {
                net.minecraft.nbt.CompoundTag tag = customData.copyTag();

                tag.getString("DisplayEntity").ifPresent(trophyBE::setDisplayEntityId);
                tag.getString("OwnerName").ifPresent(trophyBE::setOwnerName);
            }
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        AttachFace face = state.getValue(FACE);
        if (face == AttachFace.CEILING) return false;

        Direction direction = getConnectedDirection(state).getOpposite();
        BlockPos supportPos = pos.relative(direction);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, direction.getOpposite());
    }

    protected static Direction getConnectedDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case CEILING -> Direction.DOWN;
            case FLOOR -> Direction.UP;
            case WALL -> state.getValue(FACING);
        };
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return getConnectedDirection(state).getOpposite() == directionToNeighbour && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(FACE) == AttachFace.FLOOR) {
            Direction dir = state.getValue(FACING);
            return (dir == Direction.EAST || dir == Direction.WEST) ? FLOOR_X : FLOOR_Z;
        } else {
            return switch (state.getValue(FACING)) {
                case NORTH -> WALL_NORTH;
                case SOUTH -> WALL_SOUTH;
                case WEST -> WALL_WEST;
                case EAST -> WALL_EAST;
                default -> FLOOR_Z;
            };
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyBlockEntity(ModBlocks.TROPHY_BE_TYPE, pos, state);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof TrophyBlockEntity trophyBE) {
            if (trophyBE.getCustomName() != null) {
                stack.set(DataComponents.CUSTOM_NAME, trophyBE.getCustomName());
            }

            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                if (trophyBE.getDisplayEntityId() != null && !trophyBE.getDisplayEntityId().isEmpty()) {
                    tag.putString("DisplayEntity", trophyBE.getDisplayEntityId());
                }
                if (trophyBE.getOwnerName() != null && !trophyBE.getOwnerName().isEmpty()) {
                    tag.putString("OwnerName", trophyBE.getOwnerName());
                }
            });
        }
        return stack;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (level.isClientSide()) {
            net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TrophyBlockEntity trophyBE) {
                com.r3ct.bestiary.client.ClientHooks.openTrophyScreen(trophyBE);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return net.minecraft.world.InteractionResult.CONSUME;
    }
}