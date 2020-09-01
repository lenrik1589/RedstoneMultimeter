package here.lenrik1589.rsmm.meter;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

/**
 * This interface is used for determining whether a given block is powered or not. Mixins are used to implement this for
 * the Block class hierarchy.
 */
public interface Meterable {

	boolean isPowered (BlockState state, WorldAccess source, BlockPos pos);
	AbstractBlock getBlock();
}
