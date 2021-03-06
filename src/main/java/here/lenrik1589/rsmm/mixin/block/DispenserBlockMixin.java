package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.meter.Meterable;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin extends BlockMixin implements Meterable {

    public boolean isPowered(BlockState state, WorldAccess source, BlockPos pos) {
        if (source instanceof World) {
            World w = (World)source;
            return w.isReceivingRedstonePower(pos) || w.isReceivingRedstonePower(pos.up());
        }
        return false;
    }

}
