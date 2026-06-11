package com.r3ct.bestiary.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TrophyBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_Z = Shapes.or(
            Block.box(3.0D, 0.0D, 5.0D, 13.0D, 1.0D, 11.0D),
            Block.box(4.0D, 1.0D, 7.5D, 12.0D, 11.0D, 8.5D)
    );

    private static final VoxelShape SHAPE_X = Shapes.or(
            Block.box(5.0D, 0.0D, 3.0D, 11.0D, 1.0D, 13.0D),
            Block.box(7.5D, 1.0D, 4.0D, 8.5D, 11.0D, 12.0D)
    );

    public TrophyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction dir = state.getValue(FACING);
        return (dir == Direction.EAST || dir == Direction.WEST) ? SHAPE_X : SHAPE_Z;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyBlockEntity(ModBlocks.TROPHY_BE_TYPE, pos, state);
    }

    @Override
    protected net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        net.minecraft.world.item.ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);

        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TrophyBlockEntity trophyBE) {
            if (trophyBE.getCustomName() != null) {
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, trophyBE.getCustomName());
            }
        }

        return stack;
    }
}