package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.meter.Meterable;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractButtonBlock.class)
public abstract class AbstractButtonBlockMixin implements Meterable {

    public boolean isPowered(BlockState state, WorldAccess source, BlockPos pos) {
        return state.get(AbstractButtonBlock.POWERED);
    }

}
