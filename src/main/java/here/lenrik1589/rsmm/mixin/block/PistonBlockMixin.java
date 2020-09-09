package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.Names;
import here.lenrik1589.rsmm.meter.MeterEvent;
import here.lenrik1589.rsmm.meter.MeterManager;
import here.lenrik1589.rsmm.meter.Meterable;
import here.lenrik1589.rsmm.time.TickTimeGetter;

import java.rmi.NoSuchObjectException;
import java.util.List;
import java.util.Map;

import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin extends BlockMixin implements Meterable {

	//	private Queue<Boolean> queue = new ConcurrentLinkedQueue<>();

	@Shadow
	private boolean shouldExtend (World worldIn, BlockPos pos, Direction facing) { return false; }

	public boolean isPowered (BlockState state, WorldAccess source, BlockPos pos) {
		if (source instanceof World) {
			World w = (World) source;
			return this.shouldExtend(w, pos, state.get(FacingBlock.FACING));
		}
		return false;
	}

	@Inject(
					method = "move",
					at = @At(
									value = "INVOKE",
									target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;",
									ordinal = 3,
									shift = At.Shift.BEFORE
					),
					locals = LocalCapture.CAPTURE_FAILHARD
	)
	public void getStateToMoveRedirect (World world, BlockPos pos, Direction facing, boolean extending, CallbackInfoReturnable<Boolean> info,
	                                    BlockPos blockPos, PistonHandler pistonHandler, Map<BlockPos, BlockState> map, List<BlockPos> list, List<BlockState> list2, List<BlockPos> list3, BlockState[] blockStates, Direction direction, int j, int i, BlockPos blockPos4) {
		if (world.isClient || world.getServer() == null) {
			return;
		}
		//		Names.LOGGER.info("piston moved block, facing {} {} and in position {}", facing, extending, blockPos4);
		try {
			//			BlockPos oldPos = blockPos4.offset(extending ? facing : facing.getOpposite());
			BlockPos nevPos = blockPos4.offset(extending ? facing : facing.getOpposite());
			Identifier meterId = MeterManager.get(world.getServer()).getMeterId(blockPos4, world.getRegistryKey());
			if(MeterManager.get(world.getServer()).METERS.get(meterId).movable) {
				//			Names.LOGGER.info("piston moved meter, facing {}, extending {},\t and into position {} from {} With id {}", facing, extending, nevPos, blockPos4, meterId);
				MeterManager.get(world.getServer()).METERS.get(meterId).position = nevPos;
				PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
				new MeterEvent(((TickTimeGetter) world.getServer()).getTime(), meterId, MeterEvent.Event.moved).writeEvent(buffer);
				buffer.writeBlockPos(nevPos);
				world.getServer().getPlayerManager().sendToAll(new CustomPayloadS2CPacket(Names.EVENT_CHANNEL, buffer));
			}
		} catch (NoSuchObjectException ignored) {
		} catch (NullPointerException e) {
			Names.LOGGER.error(e);
		}
	}

}
