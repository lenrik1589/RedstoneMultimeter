package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.meter.Meterable;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractPressurePlateBlock.class)
public abstract class AbstractPressurePlateBlockMixin implements Meterable {

	@Shadow
	protected abstract int getRedstoneOutput (World world, BlockPos pos);

	public boolean isPowered(BlockState state, WorldAccess source, BlockPos pos) {
		if (source instanceof World) {
			World w = (World)source;
			return this.getRedstoneOutput(w, pos) > 0;
		}
		return false;
	}

}
