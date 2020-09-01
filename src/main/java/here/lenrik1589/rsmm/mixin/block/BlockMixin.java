package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.meter.Meterable;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Block.class)
public abstract class BlockMixin implements Meterable {

	@Shadow
	protected Block asBlock () {
		return null;
	}

	public boolean isPowered (BlockState state, WorldAccess source, BlockPos pos) {
		if (source instanceof World) {
			World w = (World) source;
			return w.isReceivingRedstonePower(pos);
		}
		return false;
	}

	@Override
	public AbstractBlock getBlock () {
		return asBlock();
	}

}
