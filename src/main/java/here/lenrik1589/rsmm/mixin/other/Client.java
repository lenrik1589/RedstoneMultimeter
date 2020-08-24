package here.lenrik1589.rsmm.mixin.other;

import here.lenrik1589.rsmm.meter.MeterI;
import here.lenrik1589.rsmm.meter.MeterManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftClient.class)
public class Client implements MeterI {

	public final MeterManager meterManager = new MeterManager();

	public MeterManager getMeterManager () {
		return meterManager;
	}

}
