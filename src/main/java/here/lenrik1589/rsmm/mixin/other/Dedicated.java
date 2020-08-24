package here.lenrik1589.rsmm.mixin.other;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import here.lenrik1589.rsmm.meter.MeterI;
import here.lenrik1589.rsmm.meter.MeterManager;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.dedicated.ServerPropertiesLoader;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftDedicatedServer.class)
public class Dedicated implements MeterI {
	public final MeterManager meterManager = new MeterManager();

	@Inject(method = "<init>", at = @At("RETURN"))
	public void Init (Thread thread, DynamicRegistryManager.Impl impl, LevelStorage.Session session, ResourcePackManager resourcePackManager, ServerResourceManager serverResourceManager, SaveProperties saveProperties, ServerPropertiesLoader serverPropertiesLoader, DataFixer dataFixer, MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository, UserCache userCache, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci) {
	}

	@Override
	public MeterManager getMeterManager () {
		return meterManager;
	}

}
