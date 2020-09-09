package here.lenrik1589.rsmm.meter;

import here.lenrik1589.rsmm.Names;
import here.lenrik1589.rsmm.config.ConfigHandler;
import here.lenrik1589.rsmm.time.TickTime;

import java.rmi.NoSuchObjectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Color4f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL46;


@Environment(EnvType.CLIENT)
public class Render implements IRenderer {

	private static final Render INSTANCE = new Render();
	public static final Map<Long, Integer> eventNumbers = new HashMap<>();

	public static Render getInstance () {
		return INSTANCE;
	}

	/**
	 * Called after vanilla world rendering
	 *
	 * @param partialTicks some parameter, i don't use
	 * @param matrixStack  well, some other parameter i don't use
	 */
	@Override()
	public void onRenderWorldLast (float partialTicks, MatrixStack matrixStack) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) {
			return;
		}

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
						GlStateManager.SrcFactor.SRC_ALPHA,
						GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
						GlStateManager.SrcFactor.ONE,
						GlStateManager.DstFactor.ZERO
		);
		RenderSystem.disableTexture();
		RenderSystem.depthMask(false);
		//		RenderSystem.disableLighting();

		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);

		for (Meter meter : MeterManager.get(MinecraftClient.getInstance()).METERS.values()) {
			if (player.world.getRegistryKey() == meter.dimension) {
				Color4f color = Color4f.fromColor(meter.color, 0.5f);
				BlockState state = player.world.getBlockState(meter.position);
				VoxelShape shape = state.getOutlineShape(player.world, meter.position);
				List<Box> boxes = shape.getBoundingBoxes();
				if (boxes.isEmpty() || (!(player.world.getBlockEntity(meter.position) == null) && player.world.getBlockEntity(meter.position) instanceof PistonBlockEntity)) {//(state.isFullCube(player.world, meter.position) || state.isAir()) {
					Vec3d pos = Vec3d.of(meter.position);
					drawBoxAllSidesBatchedQuads(pos, color, 0.005, buffer);
				} else {
					Vec3d pos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
					for (Box box : boxes) {
						Box drawBox = box.expand(0.005);
						drawBox = drawBox.offset(-pos.x, -pos.y, -pos.z);
						drawBox = drawBox.offset(meter.position);
						RenderUtils.drawBoxAllSidesBatchedQuads(drawBox.minX, drawBox.minY, drawBox.minZ, drawBox.maxX, drawBox.maxY, drawBox.maxZ, color, buffer);
					}
				}
			}
		}
		tessellator.draw();
		RenderSystem.enableTexture();
	}

	public void drawBoxAllSidesBatchedQuads (Vec3d pos, Color4f color, double expand, BufferBuilder buffer) {
		Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
		double minX = pos.getX() - expand - cameraPos.x;
		double minY = pos.getY() - expand - cameraPos.y;
		double minZ = pos.getZ() - expand - cameraPos.z;
		double maxX = pos.getX() + expand - cameraPos.x + 1;
		double maxY = pos.getY() + expand - cameraPos.y + 1;
		double maxZ = pos.getZ() + expand - cameraPos.z + 1;

		RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
	}

	/**
	 * onRenderGameOverlayPost draws gui and stuff…
	 *
	 * @param partialTicks fist parameter that i don't use.
	 * @param matrices     parameter needed for drawing graphics in the right place and stuff.
	 */
	@Override
	public void onRenderGameOverlayPost (float partialTicks, MatrixStack matrices) {
		MinecraftClient client = MinecraftClient.getInstance();
		Profiler profiler = client.getProfiler();
		long startRenderingTime = System.nanoTime();
		boolean hit = false;
		World world = client.world;
		if (world == null) {//return if… world is null!
			Names.LOGGER.info("world is null wtf! exiting hud rendering");
			return;
		}
		MeterManager manager = MeterManager.get(client);
		if (ConfigHandler.Rendering.visible && !manager.METERS.isEmpty()) {
			TextRenderer textRenderer = client.textRenderer;
			profiler.push("variables setup");//                                                                                                  variables setup +1
			int/**/ fHeight/*       */ = textRenderer.fontHeight,
							meterHeight/*   */ = (1 + fHeight),
							tickWidth/*     */ = ConfigHandler.Rendering.tickWidth.getIntegerValue() + 1,
							outlineColor/*  */ = ConfigHandler.Rendering.outlineColor.getIntegerValue(),
							highlightColor/**/ = ConfigHandler.Rendering.selectedTick.getIntegerValue(),
							darkColor/*     */ = ConfigHandler.Rendering.backDark.getIntegerValue(),
							width/*         */ = 0,
							height/*        */ = 1 + meterHeight * manager.METERS.size(),
							previewLength/* */ = ConfigHandler.Generic.previewLength.getIntegerValue(),
							previewHeight/* */ = ConfigHandler.Rendering.previewHeight.getIntegerValue(),
							previewPos/*    */ = Math.max(previewLength - ConfigHandler.Rendering.cursorPosition, 0),
							start/*         */ = previewHeight == 0 ? 0 : ConfigHandler.Rendering.currentLine,
							end/*           */ = previewHeight == 0 ? manager.METERS.size() : Math.min(ConfigHandler.Rendering.currentLine + previewHeight, manager.METERS.size()),
							number/*        */ = end - start,
							sWidth/*        */ = client.getWindow().getScaledWidth(),
							sHeight/*       */ = client.getWindow().getScaledHeight();
			profiler.swap("width calculation");//                                                                                                Width calculation +1
			for (int i = start; i < end; i++) {
				Meter meter = (Meter) manager.METERS.values().toArray()[i];
				width = Math.max(width, textRenderer.getWidth(meter.name) + 6);
			}
			profiler.swap("tick renderer");//                                                                                                    tick renderer +2
			if (!MinecraftClient.getInstance().options.debugEnabled){
				profiler.push("buffer setup");
				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder buffer = tessellator.getBuffer();
				RenderSystem.enableBlend();
				RenderSystem.disableTexture();
				RenderSystem.defaultBlendFunc();
				buffer.begin(GL46.GL_QUADS, VertexFormats.POSITION_COLOR);
				profiler.pop();
				profiler.push("meter getter");
				for (int i = start; i < end; i++) {
					Meter meter = (Meter) manager.METERS.values().toArray()[i];
					profiler.swap(meter.id.toString());//                                                                                                       one meter +3
					fastFill(//meter nameplate outline
									0, meterHeight * (i - start),
									width + 1, meterHeight * (i - start + 1),
									meter.color | 0xff000000, buffer);//0xff2c2b2b
					fastFill(//meter nameplate background
									1, 1 + meterHeight * (i - start),
									width, meterHeight * (i - start + 1),
									darkColor, buffer
					);
					profiler.push("ticks");//                                                                                                          ticks renderer +4
					for (int tickPos = previewLength - 1; tickPos >= 0; tickPos--) {
						int color = previewPos == tickPos ? highlightColor : outlineColor;
						fastFill(//tick outline
										width + tickPos * tickWidth, meterHeight * (i - start),
										width + (tickPos + 1) * tickWidth, meterHeight * (i + 1 - start),
										color, buffer
						);
						//NOTE I don't think this piece of code is in any vay good/ optimised at all, it just works…
						long currentTick = (ConfigHandler.Rendering.paused ? ConfigHandler.Rendering.pauseTick : world.getTime()) - previewLength + tickPos - ConfigHandler.Rendering.scrollPosition;
						MeterEvent lastEvent = meter.eventStorage.getLastPowerBefore(currentTick);
						if (lastEvent == null) {// get an empty event
							MeterEvent.Event action;
							Class<? extends Block> t = world.getBlockState(meter.position).getBlock().getClass();
							if (!t.isInstance(meter.getMeterable().getBlock())) {
								meter.setMeterable((Meterable) world.getBlockState(meter.position).getBlock());
							}
							if (meter.getMeterable().isPowered(world.getBlockState(meter.position), world, meter.position)) {
								action = MeterEvent.Event.powered;
							} else {
								action = MeterEvent.Event.unpowered;
							}
							lastEvent = new MeterEvent(new TickTime(0, TickTime.Phase.start), meter.id, action);
						}
						boolean powered = lastEvent.event == MeterEvent.Event.powered,
										eventTick = lastEvent.time.tick == currentTick;
						if (eventTick) {
							fastFill(
											width + 1 + tickPos * tickWidth, 1 + meterHeight * (i - start),
											width + (tickPos + 1) * tickWidth, meterHeight * (i + 1 - start),
											powered ? darkColor : (meter.color | 0xff000000), buffer
							);
						}
						fastFill(
										width + (eventTick ? 2 : 1) + tickPos * tickWidth, (eventTick ? 2 : 1) + meterHeight * (i - start),
										width + (eventTick ? -1 : 0) + (tickPos + 1) * tickWidth, meterHeight * (i + 1 - start) + (eventTick ? -1 : 0),
										powered ? (meter.color | 0xff000000) : darkColor, buffer
						);
					}
					profiler.pop();//                                                                                                                       +2
				} // per meter render                                                                                                                        +3
				profiler.pop();//
				fastFill(
								0, meterHeight * number,
								width + 1, 1 + meterHeight * number,
								((Meter) manager.METERS.values().toArray()[end - 1]).color | 0xff000000, buffer
				);
				fastFill(
								width + 2 - 1 - 1, meterHeight * number,
								width + previewLength * tickWidth, meterHeight * number + 1,
								outlineColor, buffer
				);
				fastFill(
								width + previewLength * tickWidth, 0,
								width + previewLength * tickWidth + 1, meterHeight * number + 1,
								outlineColor, buffer
				);
				fastFill(
								width + (previewPos + 1) * tickWidth, 0,
								width + (previewPos + 1) * tickWidth + 1, meterHeight * number + 1,
								highlightColor, buffer
				);
				fastFill(
								width + (previewPos) * tickWidth, meterHeight * number,
								width + (previewPos + 1) * tickWidth, 1 + meterHeight * number,
								highlightColor, buffer
				);
				profiler.swap("subtick_events");//                                                                                                  +2
				long currentTick = ConfigHandler.Rendering.pauseTick /*+ previewLength*/ - ConfigHandler.Rendering.cursorPosition - ConfigHandler.Rendering.scrollPosition;
				if (ConfigHandler.Rendering.paused) {
					//				Names.LOGGER.info("{},{},{}",eventNumbers.containsKey(currentTick),currentTick,eventNumbers.keySet());
					if (eventNumbers.containsKey(currentTick) && eventNumbers.get(currentTick) > 1) {
						fastFill(
										width + (previewLength + 2) * tickWidth, 0,
										width + (previewLength + eventNumbers.get(currentTick) + 2) * tickWidth + 1, meterHeight * number + 1,
										outlineColor, buffer
						);
						for (int i = start; i < end; i++) {
							Meter meter = (Meter) manager.METERS.values().toArray()[i];
							for (int tickEvent = 0; tickEvent < eventNumbers.get(currentTick); tickEvent++) {
								fastFill(
												width + (previewLength + tickEvent + 2) * tickWidth + 1, meterHeight * (i - start) + 1,
												width + (previewLength + tickEvent + 3) * tickWidth, meterHeight * (i + 1 - start),
												darkColor, buffer
								);
							}
							List<MeterEvent> events = meter.eventStorage.meterEvents.get(currentTick);
							//						MinecraftClient.getInstance().mouse.unlockCursor();
							if (events != null && !events.isEmpty()) {
								for (MeterEvent event : events) {
									if (event.event.isPower()) {
										fastFill(
														width + (previewLength + event.time.index + 2) * tickWidth + 1, meterHeight * (i - start) + 1,
														width + (previewLength + event.time.index + 3) * tickWidth, meterHeight * (i + 1 - start),
														event.event == MeterEvent.Event.powered ? darkColor : (meter.color | 0xff000000), buffer
										);
										fastFill(
														width + (previewLength + event.time.index + 2) * tickWidth + 2, meterHeight * (i - start) + 2,
														width + (previewLength + event.time.index + 3) * tickWidth - 1, meterHeight * (i + 1 - start) - 1,
														!(event.event == MeterEvent.Event.powered) ? darkColor : (meter.color | 0xff000000), buffer
										);
									} else if (event.event == MeterEvent.Event.moved) {
										fastFill(
														width + (previewLength + event.time.index + 2) * tickWidth, meterHeight * (i - start),
														width + (previewLength + event.time.index + 3) * tickWidth, meterHeight * (i + 1 - start) + 3,
														meter.color | 0xff000000, buffer
										);
										fastFill(
														width + (previewLength + event.time.index + 2) * tickWidth, meterHeight * (i - start) + 1,
														width + (previewLength + event.time.index + 3) * tickWidth, meterHeight * (i + 1 - start) + 2,
														darkColor, buffer
										);

									}
								}
							}
						}
					}
				}
				tessellator.draw();
				if (ConfigHandler.Rendering.paused)
					textRenderer.draw(matrices, new TranslatableText("rsmm.gui.paused", currentTick), 3, height, 0xcfffffff);
				for (int i = start; i < end; i++) {
					textRenderer.draw(matrices, ((Meter) manager.METERS.values().toArray()[i]).name, 4, 1 + meterHeight * (i - start), 0xcfffffff);
				}
				if(ConfigHandler.Generic.showMeterName.getBooleanValue()){
					profiler.swap("pointing at");//                                                                                                     +2
					HitResult pointing = client.crosshairTarget;
					if (pointing instanceof BlockHitResult) {
						try {
							Identifier id = manager.getMeterId(((BlockHitResult) pointing).getBlockPos(), client.world.getRegistryKey());
							Meter meter = manager.METERS.get(id);
							DrawableHelper.drawCenteredText(matrices, client.textRenderer, meter.name, sWidth / 2, sHeight / 2, 0xcfffffff);
							hit = true;
						} catch (NoSuchObjectException | NullPointerException ignored) {
						}
					}
				}
				if(ConfigHandler.Debug.DebugRendering.getBooleanValue()) {
					DrawableHelper.drawCenteredText(matrices, client.textRenderer, new LiteralText(String.format("rendering gui took %fms", (System.nanoTime() - startRenderingTime) / 1000000d)), sWidth / 2, sHeight / 2 + (hit ? client.textRenderer.fontHeight : 0), 0xcfffffff);
				}
			}
			profiler.pop();
		}
		//		double renderTime = (System.nanoTime() - startRenderingTime) / 1000000d;
		//		if (renderTime > 10) {
		//			Names.LOGGER.info("rendering gui took {}ms", renderTime);
		//		}
	}

	public void fastFill (int startX, int startY, int endX, int endY, int color, BufferBuilder buffer) {
		int a = ((color >> 24) & 0xff);
		int r = ((color >> 16) & 0xff);
		int g = ((color >> 8) & 0xff);
		int b = ((color) & 0xff);
		buffer.vertex(startX, startY, 0).color(r, g, b, a).next();
		buffer.vertex(startX, endY, 0).color(r, g, b, a).next();
		buffer.vertex(endX, endY, 0).color(r, g, b, a).next();
		buffer.vertex(endX, startY, 0).color(r, g, b, a).next();

	}

	@Override
	public Supplier<String> getProfilerSectionSupplier () {
		//		return () -> this.getClass().getName();
		return () -> "rsmm meter renderer";
	}

}
