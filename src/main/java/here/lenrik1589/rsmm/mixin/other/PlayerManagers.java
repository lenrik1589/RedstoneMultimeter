package here.lenrik1589.rsmm.mixin.other;

import here.lenrik1589.rsmm.Names;
import here.lenrik1589.rsmm.meter.Command;
import here.lenrik1589.rsmm.meter.Meter;
import here.lenrik1589.rsmm.meter.MeterManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagers {
	@Final
	@Shadow
	private MinecraftServer server;

	@Inject(
					method = "onPlayerConnect",
					at = @At(
									target = "Lnet/minecraft/server/network/ServerPlayerEntity;onSpawn()V",
									value = "INVOKE"
					)
	) public void playerConnected (ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		buffer.writeEnumConstant(MeterManager.Action.clear);
		CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(Names.METER_CHANNEL, buffer);
		connection.send(packet);
		MeterManager manager = MeterManager.get(server);
		for(Meter meter : manager.METERS.values()){
			PacketByteBuf meterBuffer = new PacketByteBuf(Unpooled.buffer());
			meterBuffer.writeEnumConstant(MeterManager.Action.add);
			Command.writeMeter(meterBuffer, meter);
			CustomPayloadS2CPacket meterPacket = new CustomPayloadS2CPacket(Names.METER_CHANNEL, meterBuffer);
			connection.send(meterPacket);
		}
	}
}
