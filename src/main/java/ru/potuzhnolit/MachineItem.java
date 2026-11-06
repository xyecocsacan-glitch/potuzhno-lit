package ru.potuzhnolit;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class MachineItem extends Item {
    private final int variant;
    public MachineItem(int variant, Properties props) { super(props); this.variant = variant; }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.isClientSide()) {
            FlyingMachineEntity machine = new FlyingMachineEntity(PotuzhnoLitMod.MACHINE, level);
            machine.setVariant(variant);
            machine.setPos(pos.getX() + .5, pos.getY() + .15, pos.getZ() + .5);
            machine.setYRot(context.getPlayer() == null ? 0 : context.getPlayer().getYRot());
            if (!level.noCollision(machine)) return InteractionResult.FAIL;
            level.addFreshEntity(machine);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
