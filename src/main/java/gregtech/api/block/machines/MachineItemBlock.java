package gregtech.api.block.machines;

import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.TieredMetaTileEntity;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class MachineItemBlock extends ItemBlock {

    public MachineItemBlock(BlockMachine block) {
        super(block);
        setHasSubtypes(true);
    }

    public static MetaTileEntity getMetaTileEntity(ItemStack itemStack) {
        return GregTechAPI.META_TILE_ENTITY_REGISTRY.getObjectById(itemStack.getItemDamage());
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(stack);
        return metaTileEntity == null ? "unnamed" : metaTileEntity.getMetaName();
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(stack);
        if(metaTileEntity == null) return;

        //item specific tooltip like: gregtech.machine.lathe.lv.tooltip
        String tooltipLocale = metaTileEntity.getMetaName() + ".tooltip";
        if (I18n.hasKey(tooltipLocale)) {
            String[] lines = I18n.format(tooltipLocale).split("/n");
            tooltip.addAll(Arrays.asList(lines));
        }

        //tier less tooltip for a electric machine like: gregtech.machine.lathe.tooltip
        if (metaTileEntity instanceof TieredMetaTileEntity) {
            String tierlessTooltipLocale = ((TieredMetaTileEntity) metaTileEntity).getTierlessTooltipKey();
            //only add tierless tooltip if it's key is not equal to normal tooltip key (i.e if machine name has dot in it's name)
            //case when it's not true would be any machine extending from TieredMetaTileEntity but having only one tier
            if (!tooltipLocale.equals(tierlessTooltipLocale) && I18n.hasKey(tierlessTooltipLocale)) {
                String[] lines = I18n.format(tierlessTooltipLocale).split("/n");
                tooltip.addAll(Arrays.asList(lines));
            }
        }

        if(flagIn.isAdvanced()) {
            tooltip.add(String.format("MetaTileEntity Id: %s", metaTileEntity.metaTileEntityId));
        }
        metaTileEntity.addInformation(stack, worldIn, tooltip, flagIn.isAdvanced());
    }
}
