package gregtech.common.blocks;

import codechicken.lib.vec.Cuboid6;
import com.google.common.collect.ImmutableMap;
import gregtech.api.unification.OreDictUnifier;
import gregtech.api.unification.material.type.IngotMaterial;
import gregtech.api.unification.material.type.Material;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.util.XSTR;
import gregtech.common.blocks.properties.PropertyMaterial;
import gregtech.common.render.StoneRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockStateContainer.StateImplementation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sun.reflect.Reflection;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

@SuppressWarnings("deprecation")
public class BlockSurfaceRock extends DelayedStateBlock {

    public final PropertyMaterial materialProperty;

    public BlockSurfaceRock(Material[] allowedValues) {
        super(net.minecraft.block.material.Material.ROCK);
        this.materialProperty = PropertyMaterial.create("material", allowedValues);
        setHardness(1.0f);
        setResistance(0.3f);
        setSoundType(SoundType.STONE);
        setUnlocalizedName("surface_rock");
        initBlockState();
    }

    @Override
    protected BlockStateContainer createStateContainer() {
        return new BlockStateContainer(this, materialProperty) {
            @Override
            protected StateImplementation createState(Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties, @Nullable ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties) {
                return new RockState(block, properties);
            }
        };
    }

    //hack used for making flooding work with surface rocks
    private class RockState extends StateImplementation {

        public RockState(Block blockIn, ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn) {
            super(blockIn, propertiesIn);
        }

        @Override
        public net.minecraft.block.material.Material getMaterial() {
            try {
                Class<?> callerClass = Reflection.getCallerClass(2);
                if(callerClass.getName().equals("net.minecraft.block.BlockLiquid")) {
                    //return water material when being requested by BlockLiquid
                    //this allows water blocks to skip sides rendering, avoiding air pockets in water areas
                    return net.minecraft.block.material.Material.WATER;
                }
            } catch (Throwable ignored) {}
            return super.getMaterial();
        }

        @Override
        public <T extends Comparable<T>> T getValue(IProperty<T> property) {
            if(property == BlockLiquid.LEVEL) {
                //to avoid crash of BlockLiquid getting LEVEL from block state
                //we always mimic full block of water in rendering
                return (T) Integer.valueOf(15);
            }
            return super.getValue(property);
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(materialProperty, materialProperty.getAllowedValues().get(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return materialProperty.getAllowedValues().indexOf(state.getValue(materialProperty));
    }

    public static Cuboid6 getShapeFromBlockPos(BlockPos blockPos) {
        XSTR random = new XSTR(Arrays.hashCode(new int[] {blockPos.getX(), blockPos.getY(), blockPos.getZ(), 135}));
        Cuboid6 cuboid = new Cuboid6(
            6 / 16.0, 0 / 16.0, 6 / 16.0,
            10 / 16.0, (1 + random.nextInt(3)) / 16.0, 10 / 16.0);
        if(random.nextBoolean()) {
            cuboid.subtract(random.nextInt(6) / 16.0, 0.0, random.nextInt(6) / 16.0);
        } else cuboid.add(random.nextInt(6) / 16.0, 0.0, random.nextInt(6) / 16.0);
        return cuboid;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return getShapeFromBlockPos(pos).aabb();
    }

    private ItemStack getDropStack(IBlockState blockState, int amount) {
        Material material = blockState.getValue(materialProperty);
        if(material instanceof IngotMaterial && ((IngotMaterial) material).blastFurnaceTemperature == 0)
            return OreDictUnifier.get(OrePrefix.nugget, material, amount);
        return OreDictUnifier.get(OrePrefix.dustTiny, material, amount);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return getDropStack(state, 1);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random rand = world instanceof World ? ((World)world).rand : RANDOM;
        int amount = 1 + rand.nextInt(fortune == 0 ? 1 : fortune);
        drops.add(getDropStack(state, amount));
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return super.canRenderInLayer(state, layer) || layer == BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return StoneRenderer.BLOCK_RENDER_TYPE;
    }

    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (fromPos.up().equals(pos)) {
            if(worldIn.getBlockState(fromPos).getBlockFaceShape(worldIn, fromPos, EnumFacing.UP) != BlockFaceShape.SOLID) {
                worldIn.destroyBlock(pos, true);
            }
        }
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

}
