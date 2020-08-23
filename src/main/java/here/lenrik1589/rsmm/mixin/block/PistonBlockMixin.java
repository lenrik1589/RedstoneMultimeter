package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.meter.Meterable;
import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin implements Meterable {

    @Shadow
    private boolean shouldExtend(World worldIn, BlockPos pos, Direction facing) { return false; }

    public boolean isPowered(BlockState state, WorldAccess source, BlockPos pos) {
        if (source instanceof World) {
            World w = (World)source;
            return this.shouldExtend(w, pos, state.get(FacingBlock.FACING));
        }
        return false;
    }

}
