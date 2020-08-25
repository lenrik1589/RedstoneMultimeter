package here.lenrik1589.rsmm;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import here.lenrik1589.rsmm.config.ConfigHandler;
import here.lenrik1589.rsmm.config.RsmmConfigGui;
import here.lenrik1589.rsmm.meter.Command;
import here.lenrik1589.rsmm.meter.Meter;
import here.lenrik1589.rsmm.meter.MeterManager;
import here.lenrik1589.rsmm.meter.Render;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.rmi.NoSuchObjectException;

public class RsmmInitializer implements ModInitializer {

	MeterS2CPacketHandler MCPH = new MeterS2CPacketHandler();
	MeterC2SPacketHandler MSPH = new MeterC2SPacketHandler();
	EventPacketHandler EPH = new EventPacketHandler();

	@Override
	public void onInitialize () {
		CommandRegistrationCallback.EVENT.register(Command::register);
		ClientSidePacketRegistry.INSTANCE.register(
						Names.METER_CHANNEL,
						MCPH::onPacketReceived
		);
		ServerSidePacketRegistry.INSTANCE.register(
						Names.METER_CHANNEL,
						MSPH::onPacketReceived
		);
		ClientSidePacketRegistry.INSTANCE.register(
						Names.EVENT_CHANNEL,
						EPH::onPacketReceived
		);
		InitializationHandler.getInstance().registerInitializationHandler(new Initializer());
	}

	private static class Initializer implements IInitializationHandler {

		@Override
		public void registerModHandlers () {
			Names.LOGGER.info("Your game is now empowered with Redstone Multimeter.");
			ConfigManager.getInstance().registerConfigHandler(Names.ModId, new ConfigHandler());
			InputEventHandler.getKeybindManager().registerKeybindProvider(RsmmInputHandler.getInstance());

			ConfigHandler.Generic.openConfig.getKeybind().setCallback(new OpenConfigGui());
			ConfigHandler.Generic.meterKey.getKeybind().setCallback(new ToggleMeter());

			RenderEventHandler.getInstance().registerGameOverlayRenderer(Render.getInstance());
			RenderEventHandler.getInstance().registerWorldLastRenderer(Render.getInstance());

		}

		private static class OpenConfigGui implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				GuiBase.openGui(new RsmmConfigGui());
				return true;
			}

		}

		private static class ToggleMeter implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				//				MinecraftClient.getInstance().mouse.unlockCursor();
				HitResult pointing = MinecraftClient.getInstance().crosshairTarget;
				if (!(pointing == null) && pointing.getType() == HitResult.Type.BLOCK) {
					BlockHitResult blockHit = (BlockHitResult) pointing;
					PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
					try {
						BlockPos pos = blockHit.getBlockPos();
						Identifier id = MeterManager.get(MinecraftClient.getInstance()).getMeterId(pos, MinecraftClient.getInstance().player.world.getRegistryKey());
						MeterManager.get(MinecraftClient.getInstance()).METERS.remove(id);
						buffer.writeEnumConstant(MeterManager.Action.remove);
						buffer.writeIdentifier(id);
					} catch (NoSuchObjectException e) {
						try {
							Meter meter = new Meter(
											blockHit.getBlockPos(),
											MinecraftClient.getInstance().world.getRegistryKey(),
											MeterManager.get(MinecraftClient.getInstance()).getNextId()
							).setColor(
											Command.Color.getNextColor().color
							);
							MeterManager.get(MinecraftClient.getInstance()).addMeter(meter);
							buffer.writeEnumConstant(MeterManager.Action.add);
							buffer.writeIdentifier(meter.id);
							buffer.writeBlockPos(meter.position);
							buffer.writeIdentifier(meter.dimension.getValue());
							buffer.writeInt(meter.color);
							buffer.writeText(meter.name);
						} catch (CommandSyntaxException ignored) {
						}
					} catch (NullPointerException ignored) {

					}
					ClientSidePacketRegistry.INSTANCE.sendToServer(Names.METER_CHANNEL, buffer);
				}
				return true;
			}

		}

	}

	private static class MeterPacketHandler {
		protected static Meter readMeter (PacketByteBuf buffer) {
			Identifier id = buffer.readIdentifier();
			BlockPos position = buffer.readBlockPos();
			Identifier dimId = buffer.readIdentifier();
			RegistryKey<World> dimension = RegistryKey.of(Registry.DIMENSION, dimId);
			int color = buffer.readInt();
			Text name = buffer.readText();
			return new Meter(position, dimension, id, name).setColor(color);
		}

	}

	private static class MeterS2CPacketHandler extends MeterPacketHandler {
		private static final Meter clearMeter = new Meter(
						new BlockPos(-2147483648, -2147483648, -2147483648),
						null,
						new Identifier("remove", "all")
		);

		public void onPacketReceived (PacketContext context, PacketByteBuf buf) {
			MeterManager.Action action = buf.readEnumConstant(MeterManager.Action.class);
			final Meter meter;
			switch (action) {
				case add -> meter = readMeter(buf);
				case name -> meter = MeterManager.get(MinecraftClient.getInstance()).METERS.get(buf.readIdentifier()).setName(buf.readText());
				case color -> meter = MeterManager.get(MinecraftClient.getInstance()).METERS.get(buf.readIdentifier()).setColor(buf.readInt());
				case remove -> meter = MeterManager.get(MinecraftClient.getInstance()).METERS.get(buf.readIdentifier());
				case clear -> meter = clearMeter;
				default -> throw new IllegalStateException("Unexpected value: " + action);
			}
			context.getTaskQueue().execute(
							() -> {
								switch (action) {
									case add -> MeterManager.get(MinecraftClient.getInstance()).METERS.put(meter.id, meter);
									case remove -> MeterManager.get(MinecraftClient.getInstance()).METERS.remove(meter.id);
									case clear -> MeterManager.get(MinecraftClient.getInstance()).METERS.clear();
								}
								Names.LOGGER.info("Received packet for {}, with action {}", meter.id, action);
							}
			);
		}

	}

	private static class EventPacketHandler {
		public void onPacketReceived (PacketContext context, PacketByteBuf buf) {
			context.getTaskQueue().execute(
							() -> {

							}
			);
		}

	}

	private static class MeterC2SPacketHandler extends MeterPacketHandler {
		public void onPacketReceived (PacketContext context, PacketByteBuf buf) {
			MeterManager.Action action = buf.readEnumConstant(MeterManager.Action.class);
			MeterManager manager = MeterManager.get(context.getPlayer().getServer());
			switch (action) {
				case add -> {
					var meter = readMeter(buf);
					manager.METERS.put(meter.id, meter);
				}
				case remove -> manager.METERS.remove(buf.readIdentifier());
				default -> throw new IllegalStateException("Unexpected value: " + action);
			}
		}
	}

}
