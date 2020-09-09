package here.lenrik1589.rsmm.meter;

import java.util.Objects;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class DimPos {
	public final RegistryKey<World> dimension;
	public       BlockPos           position;
	public DimPos(RegistryKey<World> dim, BlockPos pos){
		this.dimension = dim;
		this.position  = pos;
	}

	public DimPos (BlockPos pos, RegistryKey<World> dim) {
		this(dim, pos);
	}

	@Override
	public boolean equals (Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DimPos other = (DimPos) o;
		return dimension.equals(other.dimension) &&
						position.equals(other.position);
	}

	@Override
	public int hashCode () {
		return Objects.hash(dimension, position);
	}

}
