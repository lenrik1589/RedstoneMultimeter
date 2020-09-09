package here.lenrik1589.rsmm.mixin.other;

import here.lenrik1589.rsmm.meter.MeterI;
import here.lenrik1589.rsmm.meter.MeterManager;
import here.lenrik1589.rsmm.time.TickTime;
import here.lenrik1589.rsmm.time.TickTimeGetter;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements MeterI, TickTimeGetter {
	public final MeterManager meterManager = new MeterManager();
	public TickTime time = new TickTime(0, TickTime.Phase.start);

	@Override
	public TickTime getTime () {
		return time;
	}

	@Override
	public MeterManager getMeterManager () {
		return meterManager;
	}

}
