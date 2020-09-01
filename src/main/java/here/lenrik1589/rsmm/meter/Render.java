package here.lenrik1589.rsmm.meter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Color4f;
import here.lenrik1589.rsmm.config.ConfigHandler;
import here.lenrik1589.rsmm.time.TickTime;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.rmi.NoSuchObjectException;
import java.util.List;


@Environment(EnvType.CLIENT)
public class Render implements IRenderer {

	private static final Render                      INSTANCE = new Render();

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
				if (state.isFullCube(player.world, meter.position) || state.isAir()) {
					Vec3d pos = Vec3d.of(meter.position);
					drawBoxAllSidesBatchedQuads(pos, color, 0.005, buffer);
				} else {
					Vec3d pos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
					VoxelShape shape = state.getOutlineShape(player.world, meter.position);
					List<Box> boxes = shape.getBoundingBoxes();
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

	@Override
	public void onRenderGameOverlayPost (float partialTicks, MatrixStack matrices) {
		MeterManager manager = MeterManager.get(MinecraftClient.getInstance());
		if (ConfigHandler.Rendering.visible && !manager.METERS.isEmpty()) {
			TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
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
							number/*        */ = end - start;
			for (int i = start; i < end; i++) {
				Meter meter = (Meter) manager.METERS.values().toArray()[i];
				width = Math.max(width, textRenderer.getWidth(meter.name) + 6);
			}
			for (int i = start; i < end; i++) {
				Meter meter = (Meter) manager.METERS.values().toArray()[i];
				DrawableHelper.fill(matrices,
								0, meterHeight * (i - start),
								width + 1, meterHeight * (i - start + 1),
								meter.color | 0xff000000);//0xff2c2b2b
				DrawableHelper.fill(matrices,
								1, 1 + meterHeight * (i - start),
								width, meterHeight * (i - start + 1),
								darkColor
				);
				for (int tickPos = previewLength - 1; tickPos >= 0; tickPos--) {
					int color = previewPos == tickPos ? highlightColor : outlineColor;
					DrawableHelper.fill(matrices,
									width + tickPos * tickWidth, meterHeight * (i - start),
									width + (tickPos + 1) * tickWidth, meterHeight * (i + 1 - start),
									color
					);
					//TODO: figure out how to get/store the meter value to render it here.
					MeterEvent lastEvent;
					World world = MinecraftClient.getInstance().world;
					long currentTick = (ConfigHandler.Rendering.paused? ConfigHandler.Rendering.pauseTick : world.getTime()) - previewLength + tickPos - ConfigHandler.Rendering.scrollPosition;
					if (meter.events.size() > 0) {
						 int eventInd = meter.events.size() - 1;
						lastEvent = meter.events.get(eventInd);
						while (lastEvent.time.tick> currentTick){
							if(eventInd>0){
								eventInd--;
								lastEvent = meter.events.get(eventInd);
							}else{
								lastEvent.event = lastEvent.event == MeterEvent.Event.moved? MeterEvent.Event.moved : lastEvent.event == MeterEvent.Event.powered? MeterEvent.Event.unpowered : MeterEvent.Event.powered;
								break;
							}
						}
					} else {
						MeterEvent.Event action;
						if (meter.getMeterable().isPowered(world.getBlockState(meter.position), world, meter.position)){
							action = MeterEvent.Event.powered;
						}else{
							action = MeterEvent.Event.unpowered;
						}
						lastEvent = new MeterEvent(new TickTime(0, TickTime.Phase.start), meter.id, action);
					}
					boolean powered = lastEvent == null || lastEvent.event == MeterEvent.Event.powered,
									eventTick = lastEvent.time.tick == currentTick;
					if(eventTick)
						DrawableHelper.fill(matrices,
										width + 1 + tickPos * tickWidth, 1 + meterHeight * (i - start),
										width +(tickPos + 1) * tickWidth, meterHeight * (i + 1 - start),
										powered ? darkColor : (meter.color | 0xff000000)
						);
					DrawableHelper.fill(matrices,
									width + (eventTick? 2 : 1) + tickPos * tickWidth, (eventTick? 2 : 1) + meterHeight * (i - start),
									width + (eventTick? -1 : 0) +(tickPos + 1) * tickWidth, meterHeight * (i + 1 - start) + (eventTick? -1 : 0),
									powered ? (meter.color | 0xff000000) : darkColor
					);
				}
				textRenderer.draw(matrices, meter.name, 4, 1 + meterHeight * (i - start), 0xcfffffff);
			}
			DrawableHelper.fill(matrices,
							0, meterHeight * number,
							width + 1, 1 + meterHeight * number,
							((Meter) manager.METERS.values().toArray()[end - 1]).color | 0xff000000);
			DrawableHelper.fill(matrices,
							width + 2 - 1 - 1, 1 + meterHeight * number,
							width + previewLength * tickWidth, meterHeight * number,
							outlineColor
			);
			DrawableHelper.fill(matrices,
							width + previewLength * tickWidth, 0,
							width + previewLength * tickWidth + 1, meterHeight * number + 1,
							outlineColor
			);
			DrawableHelper.fill(matrices,
							width + (previewPos + 1) * tickWidth, 0,
							width + (previewPos + 1) * tickWidth + 1, meterHeight * number + 1,
							highlightColor
			);
			DrawableHelper.fill(matrices,
							width + (previewPos) * tickWidth, 1 + meterHeight * number,
							width + (previewPos + 1) * tickWidth, meterHeight * number,
							highlightColor
			);
			if (ConfigHandler.Rendering.paused) {
				textRenderer.draw(matrices, new TranslatableText("rsmm.gui.paused"), 3, height, 0xcfffffff);
			}
			HitResult pointing = MinecraftClient.getInstance().crosshairTarget;
			if (pointing instanceof BlockHitResult) {
				try {
					Identifier id = manager.getMeterId(((BlockHitResult) pointing).getBlockPos(), MinecraftClient.getInstance().world.getRegistryKey());
					Meter meter = manager.METERS.get(id);
					int sWidth = MinecraftClient.getInstance().getWindow().getScaledWidth(), sHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
					DrawableHelper.drawCenteredText(matrices, MinecraftClient.getInstance().textRenderer, meter.name, sWidth / 2, sHeight / 2, 0xcfffffff);
				} catch (NoSuchObjectException | NullPointerException ignored) {
				}
			}
		}

	}

}
