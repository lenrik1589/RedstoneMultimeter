package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.Names;
import here.lenrik1589.rsmm.meter.MeterEvent;
import here.lenrik1589.rsmm.meter.MeterManager;
import here.lenrik1589.rsmm.time.TickTimeGetter;
import io.netty.buffer.Unpooled;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.rmi.NoSuchObjectException;

@Mixin(PistonBlockEntity.class)
public class PistonBlockEntityMixin {
	@Shadow private Direction facing;

	@Shadow private boolean extending;

	@Inject(
					method = "tick",
					at = @At(
									value = "INVOKE",
									target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
									ordinal = 1
					)
	)
	private void pistonTick (CallbackInfo info){
		PistonBlockEntity be = ((PistonBlockEntity) (Object) this);
//		Names.LOGGER.info("piston moved block, facing {} {} and in position {}", this.facing, this.extending, be.getPos());
		try {
			BlockPos oldPos = be.getPos().offset(extending ? facing.getOpposite() : facing);
			Identifier meterId = MeterManager.get(be.getWorld().getServer()).getMeterId(oldPos, be.getWorld().getRegistryKey());
			Names.LOGGER.info("piston moved meter, facing {}, extending {} and into position {} from {} With id {}", this.facing, this.extending, be.getPos(), oldPos, meterId);
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			new MeterEvent(((TickTimeGetter) be.getWorld().getServer()).getTime(), meterId, MeterEvent.Event.moved).writeEvent(buffer);
			buffer.writeBlockPos(be.getPos());
			be.getWorld().getServer().getPlayerManager().sendToAll(new CustomPayloadS2CPacket(Names.EVENT_CHANNEL, buffer));
		} catch (NoSuchObjectException ignored){
		}
	}
}
