package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.meter.Meterable;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.entity.ComparatorBlockEntity;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ComparatorBlock.class)
public abstract class ComparatorBlockMixin extends BlockMixin implements Meterable {

    public boolean isPowered(BlockState state, WorldAccess source, BlockPos pos) {

        if (state.get(ComparatorBlock.MODE) == ComparatorMode.COMPARE) {
            return state.get(ComparatorBlock.POWERED);
        } else {
            return ((ComparatorBlockEntity) source.getBlockEntity(pos)).getOutputSignal() > 0;
        }
    }

}
