package here.lenrik1589.rsmm.mixin.block;

import here.lenrik1589.rsmm.Names;
import here.lenrik1589.rsmm.meter.MeterEvent;
import here.lenrik1589.rsmm.meter.MeterManager;
import here.lenrik1589.rsmm.meter.Meterable;
import here.lenrik1589.rsmm.time.TickTimeGetter;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.rmi.NoSuchObjectException;
import java.util.List;
import java.util.Map;

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
									ordinal = 2,
									shift = At.Shift.BEFORE
					),
					locals = LocalCapture.CAPTURE_FAILHARD
	)
	public void getStateToMoveRedirect (World world, BlockPos pos, Direction facing, boolean retracting, CallbackInfoReturnable<Boolean> info,
	                                    BlockPos blockPos, PistonHandler pistonHandler, Map map, List list, List list2, List list3, BlockState[] blockStates, Direction direction, int j, int i, BlockPos blockPos4) {
		Names.LOGGER.info("don't know if it'll vvork.");
		//		Names.LOGGER.info("piston moved block, facing {} {} and in position {}", facing, retracting, blockPos4);
		try {
			BlockPos oldPos = blockPos4.offset(retracting ? facing.getOpposite() : facing);
			Identifier meterId = MeterManager.get(world.getServer()).getMeterId(oldPos, world.getRegistryKey());
			Names.LOGGER.info("piston moved meter, facing {}, retracting {} and into position {} from {} With id {}", facing, retracting, blockPos4, oldPos, meterId);
			MeterManager.get(world.getServer()).METERS.get(meterId).position = blockPos4;
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			new MeterEvent(((TickTimeGetter) world.getServer()).getTime(), meterId, MeterEvent.Event.moved).writeEvent(buffer);
			buffer.writeBlockPos(blockPos4);
			world.getServer().getPlayerManager().sendToAll(new CustomPayloadS2CPacket(Names.EVENT_CHANNEL, buffer));
		} catch (NoSuchObjectException ignored) {
		}
	}

	//	@Inject(
	//					method = "tryMove",
	//					at = @At(
	//									value = "INVOKE",
	//									target = "Lnet/minecraft/world/World;addSyncedBlockEvent(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V",
	//									ordinal = 0
	//					)
	//	)
	//	private void pistonExtending (World world, BlockPos pos, BlockState state, CallbackInfo info) {
	//		queue.add(true);
	//	}
	//
	//	@Inject(
	//					method = "tryMove",
	//					at = @At(
	//									value = "INVOKE",
	//									target = "Lnet/minecraft/world/World;addSyncedBlockEvent(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V",
	//									ordinal = 1
	//					)
	//	)
	//	private void pistonRetracting (World world, BlockPos pos, BlockState state, CallbackInfo info) {
	//		queue.add(false);
	//	}
	//
	//	@Inject(
	//					method = "move",
	//					at = @At(
	//									value = "HEAD"
	//					)
	//	)
	//	private void pistonMove (World world, BlockPos pos, Direction dir, boolean retract, CallbackInfoReturnable<Boolean> info){
	//		try {
	//			boolean extending = queue.remove();
	//			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
	//			Identifier meterId = MeterManager.get(world.getServer()).getMeterId(pos, (world).getRegistryKey());
	//			MeterEvent event = new MeterEvent(((TickTimeGetter)world.getServer()).getTime(), meterId, MeterEvent.Event.moved);
	//		} catch (NoSuchObjectException suere) {
	//			Names.LOGGER.info("ignoring exception \"{}\" in piston#move", suere.getMessage());
	//		}
	//	}

}
