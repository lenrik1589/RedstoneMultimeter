package here.lenrik1589.rsmm;

import here.lenrik1589.rsmm.config.ConfigHandler;
import here.lenrik1589.rsmm.meter.*;
import here.lenrik1589.rsmm.time.TickTime;
import here.lenrik1589.rsmm.time.TickTimeGetter;

import java.util.Random;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class RsmmInitializer implements ModInitializer, ClientModInitializer {

	private static final Packets.S2CMeterPacketHandler CMPH = new Packets.S2CMeterPacketHandler();
	private static final Packets.C2SMeterPacketHandler SMPH = new Packets.C2SMeterPacketHandler();
	private static final Packets.EventPacketHandler EPH = new Packets.EventPacketHandler();
	private static final Identifier timeSyncId = new Identifier("", "");

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
		ServerTickEvents.START_SERVER_TICK.register((server) -> {
			((TickTimeGetter) server).getTime().tick = server.getOverworld().getTime();
			((TickTimeGetter) server).getTime().phase = TickTime.Phase.start;
			((TickTimeGetter) server).getTime().index = 0;
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			MeterEvent event = new MeterEvent(((TickTimeGetter) server).getTime(), timeSyncId, MeterEvent.Event.tickStart);
			event.writeEvent(buffer);
			server.getPlayerManager().sendToAll(new CustomPayloadS2CPacket(Names.EVENT_CHANNEL, buffer));
		});
	}

	public void onInitializeClient () {
		Random random = new Random();
		if (random.nextDouble() < 0.01) {
			String[] lulz = new String[]{
							"Don't forget to feed your entry points.",
							"This is not Wisconsin!"
			};
			Names.LOGGER.info(lulz[random.nextInt(lulz.length)]);
		}
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

			ConfigHandler.Generic.openConfig.getKeybind().setCallback(new RsmmInputHandler.Inputs.OpenConfigGui());
			ConfigHandler.Generic.pauseKey.getKeybind().setCallback(new RsmmInputHandler.Inputs.PausePreview());
			ConfigHandler.Generic.meterKey.getKeybind().setCallback(new RsmmInputHandler.Inputs.ToggleMeter());
			ConfigHandler.Generic.previewKey.getKeybind().setCallback(new RsmmInputHandler.Inputs.TogglePreviewVisible());
			ConfigHandler.Generic.upKey.getKeybind().setCallback(new RsmmInputHandler.Inputs.ScrollUp());
			ConfigHandler.Generic.downKey.getKeybind().setCallback(new RsmmInputHandler.Inputs.ScrollDown());
			ConfigHandler.Generic.leftKey.getKeybind().setCallback(new RsmmInputHandler.Inputs.ScrollLeft());
			ConfigHandler.Generic.rightKey.getKeybind().setCallback(new RsmmInputHandler.Inputs.ScrollRight());
			ConfigHandler.Generic.previewLength.setValueChangeCallback(config -> ConfigHandler.Generic.previewCursorPosition.setMaxValue(config.getIntegerValue()));
			ConfigHandler.Generic.previewCursorPosition.setValueChangeCallback(config -> ConfigHandler.Rendering.cursorPosition = ConfigHandler.Rendering.paused ? ConfigHandler.Rendering.cursorPosition : config.getIntegerValue());

			RenderEventHandler.getInstance().registerGameOverlayRenderer(Render.getInstance());
			RenderEventHandler.getInstance().registerWorldLastRenderer(Render.getInstance());

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

		public static class C2SMeterPacketHandler extends MeterPacketHandler {
			public void onPacketReceived (PacketContext context, PacketByteBuf buf) {
				MeterManager.Action action = buf.readEnumConstant(MeterManager.Action.class);
				MeterManager manager = MeterManager.get(context.getPlayer().getServer());
				Meter meter;
				switch (action) {
					case add -> {
						meter = readMeter(buf);
						manager.addMeter(meter);
					}
					case remove -> {
						meter = manager.METERS.get(buf.readIdentifier());
						manager.removeMeter(meter);
					}
					default -> throw new IllegalStateException("Unexpected value: " + action);
				}
			}

		}

		public static class S2CMeterPacketHandler extends MeterPacketHandler {
			private static final Meter clearMeter = new Meter(
							new BlockPos(-2147483648, -2147483648, -2147483648),
							null,
							new Identifier("remove", "all")
			);

			public void onPacketReceived (PacketContext context, PacketByteBuf buffer) {
				if (MinecraftClient.getInstance().world == null) {
					return;
				}
				//			Names.LOGGER.info(buffer.array());
				MeterManager.Action action = buffer.readEnumConstant(MeterManager.Action.class);
				final Meter meter;
				switch (action) {
					case add -> {
						meter = readMeter(buffer);
						meter.setMeterable((Meterable) (MinecraftClient.getInstance().world.getBlockState(meter.position).getBlock()));
					}
					case name -> meter = MeterManager.get(MinecraftClient.getInstance()).get(buffer.readIdentifier()).setName(buffer.readText());
					case color -> meter = MeterManager.get(MinecraftClient.getInstance()).get(buffer.readIdentifier()).setColor(buffer.readInt());
					case remove -> meter = MeterManager.get(MinecraftClient.getInstance()).get(buffer.readIdentifier());
					case clear -> meter = clearMeter;
					default -> throw new IllegalStateException("Unexpected value: " + action);
				}
				context.getTaskQueue().execute(
								() -> {
									switch (action) {
										case add -> MeterManager.get(MinecraftClient.getInstance()).addMeter(meter);
										case remove -> MeterManager.get(MinecraftClient.getInstance()).removeMeter(meter);
										case clear -> MeterManager.get(MinecraftClient.getInstance()).clear();
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
				Meter meter = MeterManager.get(MinecraftClient.getInstance()).METERS.get(event.meterId);
				//				Names.LOGGER.info("meter event \"{}\" for id {} on tick {} №{} in phase {};", event.event, event.meterId, event.time.tick, event.time.index, event.time.phase);
				context.getTaskQueue().execute(
								() -> {
									if (meter == null) {
										for (Meter m : MeterManager.get(MinecraftClient.getInstance()).METERS.values()) {
											m.eventStorage.addEvent(event);
										}

									} else {
										meter.eventStorage.addEvent(event);
										if (event.event == MeterEvent.Event.moved) {
											meter.position = nevPos;
										}
										Render.eventNumbers.put(
														event.time.tick,
														Render.eventNumbers.containsKey(event.time.tick) ? Render.eventNumbers.get(event.time.tick) + 1 : 1
										);
									}
								}
				);

			}

		}

	}

}
