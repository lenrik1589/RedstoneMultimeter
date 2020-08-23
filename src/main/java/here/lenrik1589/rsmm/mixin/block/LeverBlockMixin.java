package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.meter.Meterable;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LeverBlock.class)
public abstract class LeverBlockMixin implements Meterable {

    public boolean isPowered(BlockState state, WorldAccess source, BlockPos pos) {
        return state.get(LeverBlock.POWERED);
    }

}
