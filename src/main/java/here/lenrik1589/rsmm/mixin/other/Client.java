package here.lenrik1589.rsmm.mixin.other;

import here.lenrik1589.rsmm.Names;
import here.lenrik1589.rsmm.meter.MeterI;
import here.lenrik1589.rsmm.meter.MeterManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class Client implements MeterI {

	public MeterManager meterManager;

	@Inject(method = "<init>", at = @At("HEAD"), require = 10)
	public void Init (RunArgs args, CallbackInfo ci) {
		meterManager = new MeterManager();
		Names.LOGGER.info("fail did it do really?");
	}

	public MeterManager getMeterManager () {
		return meterManager;
	}

}
