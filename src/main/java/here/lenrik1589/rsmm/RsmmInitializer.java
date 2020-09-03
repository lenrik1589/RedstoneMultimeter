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
import here.lenrik1589.rsmm.meter.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.rmi.NoSuchObjectException;

public class RsmmInitializer implements ModInitializer, ClientModInitializer {

	Packets.MeterS2CPacketHandler CMPH = new Packets.MeterS2CPacketHandler();
	Packets.MeterC2SPacketHandler SMPH = new Packets.MeterC2SPacketHandler();
	Packets.EventPacketHandler EPH = new Packets.EventPacketHandler();

	@Override
	public void onInitialize () {
		ArgumentTypes.register("rssm:color", IntColorArgument.class, new ConstantArgumentSerializer<>(IntColorArgument::color));
		CommandRegistrationCallback.EVENT.register(Command::register);
		ServerSidePacketRegistry.INSTANCE.register(
						Names.METER_CHANNEL,
						SMPH::onPacketReceived
		);
		ServerSidePacketRegistry.INSTANCE.register(
						Names.EVENT_CHANNEL,
						(context, buffer) -> Names.LOGGER.info("packet {} in context {}", buffer.array(), context)
		);
		InitializationHandler.getInstance().registerInitializationHandler(new Initializer());
	}

	public void onInitializeClient () {
		ClientSidePacketRegistry.INSTANCE.register(
						Names.EVENT_CHANNEL,
						EPH::onPacketReceived
		);
		ClientSidePacketRegistry.INSTANCE.register(
						Names.METER_CHANNEL,
						CMPH::onPacketReceived
		);
	}

	private static class Initializer implements IInitializationHandler {

		@Override
		public void registerModHandlers () {
			Names.LOGGER.info("Your game is now empowered with Redstone Multimeter.");
			ConfigManager.getInstance().registerConfigHandler(Names.ModId, new ConfigHandler());
			InputEventHandler.getKeybindManager().registerKeybindProvider(RsmmInputHandler.getInstance());

			ConfigHandler.Generic.openConfig.getKeybind().setCallback(new OpenConfigGui());
			ConfigHandler.Generic.pauseKey.getKeybind().setCallback(new PausePreview());
			ConfigHandler.Generic.meterKey.getKeybind().setCallback(new ToggleMeter());
			ConfigHandler.Generic.previewKey.getKeybind().setCallback(new TogglePreviewVisible());
			ConfigHandler.Generic.upKey.getKeybind().setCallback(new ScrollUp());
			ConfigHandler.Generic.downKey.getKeybind().setCallback(new ScrollDown());
			ConfigHandler.Generic.leftKey.getKeybind().setCallback(new ScrollLeft());
			ConfigHandler.Generic.rightKey.getKeybind().setCallback(new ScrollRight());
			ConfigHandler.Generic.previewLength.setValueChangeCallback(config -> ConfigHandler.Generic.previewCursorPosition.setMaxValue(config.getIntegerValue()));
			ConfigHandler.Generic.previewCursorPosition.setValueChangeCallback(config -> ConfigHandler.Rendering.cursorPosition = ConfigHandler.Rendering.paused ? ConfigHandler.Rendering.cursorPosition : config.getIntegerValue());

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

		private static class TogglePreviewVisible implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				ConfigHandler.Rendering.visible = !ConfigHandler.Rendering.visible;
				return true;
			}

		}

		private static class ScrollUp implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				ConfigHandler.Rendering.currentLine = Math.max(0, ConfigHandler.Rendering.currentLine - 1);
				return true;
			}

		}

		private static class ScrollDown implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				ConfigHandler.Rendering.currentLine = Math.max(0, Math.min(MeterManager.get(MinecraftClient.getInstance()).METERS.size() - ConfigHandler.Rendering.previewHeight.getIntegerValue(), ConfigHandler.Rendering.currentLine + 1));
				return true;
			}

		}

		private static class ScrollRight implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				if (ConfigHandler.Rendering.paused) {
					ConfigHandler.Rendering.cursorPosition -= 1;
					if (ConfigHandler.Rendering.cursorPosition < 1) {
						ConfigHandler.Rendering.cursorPosition = 1;
						ConfigHandler.Rendering.scrollPosition = Math.max(ConfigHandler.Rendering.scrollPosition - 1, 0);
					}
				}
				return true;
			}

		}

		private static class ScrollLeft implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				if (ConfigHandler.Rendering.paused) {
					ConfigHandler.Rendering.cursorPosition += 1;
					if (ConfigHandler.Rendering.cursorPosition > ConfigHandler.Generic.previewLength.getIntegerValue()) {
						ConfigHandler.Rendering.cursorPosition = ConfigHandler.Generic.previewLength.getIntegerValue();
						ConfigHandler.Rendering.scrollPosition = Math.min(ConfigHandler.Rendering.scrollPosition + 1, ConfigHandler.Generic.maxHistory.getIntegerValue() - ConfigHandler.Generic.previewLength.getIntegerValue());
					}
				}
				return true;
			}

		}

		private static class PausePreview implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				ConfigHandler.Rendering.paused = !ConfigHandler.Rendering.paused;
				ConfigHandler.Rendering.cursorPosition = ConfigHandler.Generic.previewCursorPosition.getIntegerValue();
				ConfigHandler.Rendering.scrollPosition = 0;
				ConfigHandler.Rendering.pauseTick = MinecraftClient.getInstance().world.getTime();
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
					} catch (NullPointerException e) {
						Names.LOGGER.info("ignoring {} in RsmmInitializer$ToggleMeter.onKeyAction", e.getMessage());
					} catch (NoSuchObjectException e) {
						try {
							Meter meter = new Meter(
											blockHit.getBlockPos(),
											MinecraftClient.getInstance().world.getRegistryKey(),
											MeterManager.get(MinecraftClient.getInstance()).getNextId()
							).setColor(
											Command.Color.getNextColor().color
							);
							meter.setMeterable((Meterable) (MinecraftClient.getInstance().world.getBlockState(meter.position).getBlock()));
							MeterManager.get(MinecraftClient.getInstance()).addMeter(meter);
							buffer.writeEnumConstant(MeterManager.Action.add);
							buffer.writeIdentifier(meter.id);
							buffer.writeBlockPos(meter.position);
							buffer.writeIdentifier(meter.dimension.getValue());
							buffer.writeInt(meter.color);
							buffer.writeText(meter.name);
						} catch (CommandSyntaxException ignored) {
						}
					}
					ClientSidePacketRegistry.INSTANCE.sendToServer(Names.METER_CHANNEL, buffer);
				}
				return true;
			}

		}

	}

	private static class Packets {
		public static class MeterPacketHandler {
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

		public static class MeterC2SPacketHandler extends MeterPacketHandler {
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

		public static class MeterS2CPacketHandler extends MeterPacketHandler {
			private static final Meter clearMeter = new Meter(
							new BlockPos(-2147483648, -2147483648, -2147483648),
							null,
							new Identifier("remove", "all")
			);

			public void onPacketReceived (PacketContext context, PacketByteBuf buffer) {
				//			Names.LOGGER.info(buffer.array());
				MeterManager.Action action = buffer.readEnumConstant(MeterManager.Action.class);
				final Meter meter;
				switch (action) {
					case add -> {
						meter = readMeter(buffer);
						meter.setMeterable((Meterable) (MinecraftClient.getInstance().world.getBlockState(meter.position).getBlock()));
					}
					case name -> meter = MeterManager.get(MinecraftClient.getInstance()).METERS.get(buffer.readIdentifier()).setName(buffer.readText());
					case color -> meter = MeterManager.get(MinecraftClient.getInstance()).METERS.get(buffer.readIdentifier()).setColor(buffer.readInt());
					case remove -> meter = MeterManager.get(MinecraftClient.getInstance()).METERS.get(buffer.readIdentifier());
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

		public static class EventPacketHandler {
			public void onPacketReceived (PacketContext context, PacketByteBuf buffer) {
				MeterEvent event = MeterEvent.readEvent(buffer);
				BlockPos nevPos;
				if (event.event == MeterEvent.Event.moved) {
					nevPos = buffer.readBlockPos();
				} else {
					nevPos = null;
				}
				//				Names.LOGGER.info("meter event \"{}\" for id {} on tick {} №{} in phase {};", event.event, event.meterId, event.time.tick, event.time.index, event.time.phase);
				context.getTaskQueue().execute(
								() -> {
									MeterManager.get(MinecraftClient.getInstance()).METERS.get(event.meterId).events.add(event);
									if (event.event == MeterEvent.Event.moved) {
										MeterManager.get(MinecraftClient.getInstance()).METERS.get(event.meterId).position = nevPos;
									}
								}
				);
			}

		}

	}

}
