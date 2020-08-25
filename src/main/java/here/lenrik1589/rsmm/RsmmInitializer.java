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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Arrays;

public class RsmmInitializer implements ModInitializer {

	MeterPacketHandler MPH = new MeterPacketHandler();
	EventPacketHandler EPH = new EventPacketHandler();

	@Override
	public void onInitialize () {
		CommandRegistrationCallback.EVENT.register(Command::register);
		InitializationHandler.getInstance().registerInitializationHandler(new Initializer());
		ClientSidePacketRegistry.INSTANCE.register(
						Names.METER_CHANNEL,
						MPH::onPacketReceived
		);
		ServerSidePacketRegistry.INSTANCE.register(
						Names.METER_CHANNEL,
						MPH::onPacketReceived
		);
		ClientSidePacketRegistry.INSTANCE.register(
						Names.EVENT_CHANNEL,
						EPH::onPacketReceived
		);
		ServerSidePacketRegistry.INSTANCE.register(
						Names.EVENT_CHANNEL,
						EPH::onPacketReceived
		);
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
				HitResult pointing = MinecraftClient.getInstance().crosshairTarget;
				if (!(pointing == null) && pointing.getType() == HitResult.Type.BLOCK) {
					BlockHitResult blockHit = (BlockHitResult) pointing;
					try {
						MeterManager.get(MinecraftClient.getInstance()).addMeter(
										new Meter(
														blockHit.getBlockPos(),
														MinecraftClient.getInstance().world.getRegistryKey(),
														MeterManager.get(MinecraftClient.getInstance()).getNextId()
										).setColor(
														Command.Color.getNextColor().color
										)
						);
					} catch (CommandSyntaxException ignored) {
					}
				}
				return true;
			}

		}

	}

	private static class MeterPacketHandler {
		public void onPacketReceived (PacketContext context, PacketByteBuf buf) {
			MeterManager.Action action = buf.readEnumConstant(MeterManager.Action.class);
			Meter meter;
			switch (action) {
				case add -> {
					meter = readMeter(buf);
					MeterManager.get(MinecraftClient.getInstance()).METERS.put(meter.id, meter);
				}
				case name -> {
					meter = MeterManager.get(MinecraftClient.getInstance()).METERS.get(buf.readIdentifier());
					meter.name = buf.readText();
				}
				case color -> {
					meter = MeterManager.get(MinecraftClient.getInstance()).METERS.get(buf.readIdentifier());
					meter.color = buf.readInt();
				}
				case remove -> meter = MeterManager.get(MinecraftClient.getInstance()).METERS.remove(buf.readIdentifier());
				default -> throw new IllegalStateException("Unexpected value: " + action);
			}
			Names.LOGGER.info("Received packet for {}, with action {}", meter.id, action);
		}

		private static Meter readMeter (PacketByteBuf buffer) {
			Identifier id = buffer.readIdentifier();
			BlockPos position = buffer.readBlockPos();
			Identifier dimId = buffer.readIdentifier();
			RegistryKey<World> dimension = RegistryKey.of(Registry.DIMENSION, dimId);
			int color = buffer.readInt();
			Text name = buffer.readText();
			return new Meter(position, dimension, id, name).setColor(color);
		}

	}

	private static class EventPacketHandler {
		public void onPacketReceived (PacketContext context, PacketByteBuf buf) {
		}

	}

}
