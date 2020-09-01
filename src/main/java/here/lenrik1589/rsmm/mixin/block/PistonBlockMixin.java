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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.rmi.NoSuchObjectException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
