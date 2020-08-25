package here.lenrik1589.rsmm.meter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class Meter {
	public Text name;
	public final Identifier id;
	public final RegistryKey<World> dimension;
	public BlockPos position;
	private Meterable meterable;
	public Integer color;
	public boolean movable = false;

	public Meter(BlockPos pos, RegistryKey<World> dimension, Identifier id){
		this(pos, dimension, id, new LiteralText(id.getPath()));
	}

	public Meter(BlockPos pos, RegistryKey<World> dimension, Identifier id, Text name){
		this.dimension = dimension;
		this.position = pos;
		this.id = id;
		this.name = name;
		this.color = Command.Color.values()[Command.Color.values().length - 1].color;
	}

	public void setMeterable (Meterable meterable) {
		this.meterable = meterable;
	}

	public Meterable getMeterable () {
		return meterable;
	}

	public Meter setColor (Integer color) {
		this.color = color;
		return this;
	}

	public Meter setName (Text name) {
		this.name = name;
		return this;
	}

}
