package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.meter.Meterable;
import net.minecraft.block.BlockState;
import net.minecraft.block.ObserverBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ObserverBlock.class)
public abstract class ObserverBlockMixin implements Meterable {

    public boolean isPowered(BlockState state, WorldAccess source, BlockPos pos) {
        return state.get(ObserverBlock.POWERED);
    }

}
