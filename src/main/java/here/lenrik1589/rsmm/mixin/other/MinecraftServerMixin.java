package here.lenrik1589.rsmm.mixin.other;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import here.lenrik1589.rsmm.meter.MeterI;
import here.lenrik1589.rsmm.meter.MeterManager;
import here.lenrik1589.rsmm.time.TickTime;
import here.lenrik1589.rsmm.time.TickTimeGetter;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements MeterI, TickTimeGetter {
	public final MeterManager meterManager = new MeterManager();
	public TickTime time;

	@Inject(method = "<init>", at = @At("RETURN"))
	public void Init (Thread thread, DynamicRegistryManager.Impl impl, LevelStorage.Session session, SaveProperties saveProperties, ResourcePackManager resourcePackManager, Proxy proxy, DataFixer dataFixer, ServerResourceManager serverResourceManager, MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository, UserCache userCache, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci) {
		time = new TickTime(0, TickTime.Phase.start);
	}

	@Override
	public TickTime getTime () {
		return time;
	}

	@Override
	public MeterManager getMeterManager () {
		return meterManager;
	}

}
