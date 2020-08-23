package here.lenrik1589.rsmm.meter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Color4f;
import jdk.jfr.BooleanFlag;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;


@Environment(EnvType.CLIENT)
public class Render implements IRenderer {

	private static final Render INSTANCE = new Render();

	public static Render getInstance () {
		return INSTANCE;
	}

	/**
	 * Called after vanilla world rendering
	 *
	 * @param partialTicks i guess this should be useful huh?
	 * @param matrixStack  i literally have no idea what the heck is this variable this and why every rendering method
	 *                     need it (i so far only worked with GUIs)
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

		for (Meter m : MeterManager.get(MinecraftClient.getInstance()).METERS.values()) {
			Vec3d pos = Vec3d.of(m.position);
			if (player.world.getRegistryKey() == m.dimension) {
				Color4f color = Color4f.fromColor(m.color, 0.5f);
				drawBoxAllSidesBatchedQuads(pos, color, 0.005, buffer);
			}
		}
		tessellator.draw();
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

}
