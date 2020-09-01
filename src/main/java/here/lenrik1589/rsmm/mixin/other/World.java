package here.lenrik1589.rsmm.mixin.other;

import here.lenrik1589.rsmm.Names;
import here.lenrik1589.rsmm.meter.MeterEvent;
import here.lenrik1589.rsmm.meter.MeterManager;
import here.lenrik1589.rsmm.meter.Meterable;
import here.lenrik1589.rsmm.time.TickTime;
import here.lenrik1589.rsmm.time.TickTimeGetter;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.ScheduledTick;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Spawner;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.rmi.NoSuchObjectException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class World{
	@Shadow
	@Final
	private MinecraftServer server;

	@Inject(method = "<init>", at = @At("RETURN"))
	public void init (MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<net.minecraft.world.World> registryKey, DimensionType dimensionType, WorldGenerationProgressListener worldGenerationProgressListener, ChunkGenerator chunkGenerator, boolean bl, long l, List<Spawner> list, boolean bl2, CallbackInfo info) {
		((TickTimeGetter)server).getTime().tick = properties.getTime();
		((TickTimeGetter)server).getTime().index = 0;
	}

	@Inject(
					method = "tick",
					at = @At(
									value = "HEAD"
					)
	)
	private void tickStart (BooleanSupplier shouldKeepTicking, CallbackInfo info) {
		((TickTimeGetter)server).getTime().tick = ((ServerWorld)(Object)this).getTime();
		((TickTimeGetter)server).getTime().index = 0;
	}

	@Inject(
					method = "tick",
					at = @At(
									value = "INVOKE_STRING",
									target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
									args = "ldc=tickPending"
					)
	)
	private void tickPending (BooleanSupplier shouldKeepTicking, CallbackInfo info) {
		((TickTimeGetter)server).getTime().phase = TickTime.Phase.tickPending;
	}

	@Inject(
					method = "tick",
					at = @At(
									value = "INVOKE_STRING",
									target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
									args = "ldc=blockEvents"
					)
	)
	private void blockEvents (BooleanSupplier shouldKeepTicking, CallbackInfo info) {
		((TickTimeGetter)server).getTime().phase = TickTime.Phase.blockEvents;
	}

	@Inject(
					method = "tick",
					at = @At(
									value = "INVOKE_STRING",
									target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
									args = "ldc=entities"
					)
	)
	private void entities (BooleanSupplier shouldKeepTicking, CallbackInfo info) {
		((TickTimeGetter)server).getTime().phase = TickTime.Phase.entities;
	}

	@Inject(
					method = "tick",
					at = @At(
									value = "INVOKE",
									target = "Lnet/minecraft/server/world/ServerWorld;tickBlockEntities()V"
					)
	)
	private void blockEntities (BooleanSupplier shouldKeepTicking, CallbackInfo info) {
		((TickTimeGetter)server).getTime().phase = TickTime.Phase.blockEntities;
	}

	BlockPos lastPos;
	boolean  lastPower;

	@Inject(
					method = "tickBlock",
					at = @At(
									target = "Lnet/minecraft/block/BlockState;scheduledTick(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V",
									value = "INVOKE",
									shift = At.Shift.BEFORE
					),
					locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void beforeTickBlock (ScheduledTick<Block> tick, CallbackInfo info, BlockState state){
		BlockPos pos = tick.pos;
		lastPos = pos;
		lastPower = ((Meterable)tick.getObject()).isPowered(state, (ServerWorld)(Object)this, pos);
	}

	@Inject(
					method = "tickBlock",
					at = @At(
									target = "Lnet/minecraft/block/BlockState;scheduledTick(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V",
									value = "INVOKE",
									shift = At.Shift.AFTER
					),
					locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void afterTickBlock (ScheduledTick<Block> tick, CallbackInfo info, BlockState state){
		boolean newPower = ((Meterable)tick.getObject()).isPowered(((ServerWorld)(Object)this).getBlockState(lastPos), (ServerWorld)(Object)this, lastPos);
		if (lastPower != newPower){
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			try {
				Identifier meterId = MeterManager.get(server).getMeterId(lastPos, ((ServerWorld)(Object)this).getRegistryKey());
				MeterEvent event = new MeterEvent(((TickTimeGetter)server).getTime(), meterId, lastPower? MeterEvent.Event.unpowered: MeterEvent.Event.powered);
				event.writeEvent(buffer);
				CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(Names.EVENT_CHANNEL, buffer);
				server.getPlayerManager().sendToAll(packet);
				++((TickTimeGetter)server).getTime().index;
			} catch (NoSuchObjectException ignored){
			}
		}
//		Names.LOGGER.info("scheduled tick {}->{} {} {}", lastPower, newPower, lastPos, tick.getObject().getName().getString());
	}

}
