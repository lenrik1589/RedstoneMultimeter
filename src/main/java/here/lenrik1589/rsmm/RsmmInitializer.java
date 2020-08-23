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
import fi.dy.masa.malilib.interfaces.IRenderer;
import here.lenrik1589.rsmm.config.ConfigHandler;
import here.lenrik1589.rsmm.meter.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class RsmmInitializer implements ModInitializer {

	@Override
	public void onInitialize () {
		CommandRegistrationCallback.EVENT.register(Command::register);
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

}
