package here.lenrik1589.rsmm;

import here.lenrik1589.rsmm.config.ConfigHandler;
import here.lenrik1589.rsmm.config.RsmmConfigGui;
import here.lenrik1589.rsmm.meter.Command;
import here.lenrik1589.rsmm.meter.Meter;
import here.lenrik1589.rsmm.meter.MeterManager;
import here.lenrik1589.rsmm.meter.Meterable;

import java.rmi.NoSuchObjectException;
import java.util.List;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class RsmmInputHandler implements IKeybindProvider {
	private static final RsmmInputHandler INSTANCE = new RsmmInputHandler();

	private RsmmInputHandler () {
		super();
	}

	public static RsmmInputHandler getInstance () {
		return INSTANCE;
	}

	@Override
	public void addKeysToMap (IKeybindManager manager) {
		manager.addKeybindToMap(ConfigHandler.Generic.openConfig.getKeybind());
		manager.addKeybindToMap(ConfigHandler.Generic.meterKey.getKeybind());
		manager.addKeybindToMap(ConfigHandler.Generic.pauseKey.getKeybind());
		manager.addKeybindToMap(ConfigHandler.Generic.previewKey.getKeybind());
		manager.addKeybindToMap(ConfigHandler.Generic.leftKey.getKeybind());
		manager.addKeybindToMap(ConfigHandler.Generic.rightKey.getKeybind());
		manager.addKeybindToMap(ConfigHandler.Generic.upKey.getKeybind());
		manager.addKeybindToMap(ConfigHandler.Generic.downKey.getKeybind());
	}

	@Override
	public void addHotkeys (IKeybindManager manager) {
		List<? extends IHotkey> hotkeys = ImmutableList.of(
						ConfigHandler.Generic.openConfig,
						ConfigHandler.Generic.meterKey,
						ConfigHandler.Generic.pauseKey,
						ConfigHandler.Generic.previewKey,
						ConfigHandler.Generic.rightKey,
						ConfigHandler.Generic.leftKey,
						ConfigHandler.Generic.upKey,
						ConfigHandler.Generic.downKey
		);
		manager.addHotkeysForCategory(Names.ModId, "rsmm.key.category", hotkeys);
	}

	static class Inputs{
		static class OpenConfigGui implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				GuiBase.openGui(new RsmmConfigGui());
				return true;
			}

		}

		static class TogglePreviewVisible implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				ConfigHandler.Rendering.visible = !ConfigHandler.Rendering.visible;
				return true;
			}

		}

		static class ScrollUp implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				ConfigHandler.Rendering.currentLine = Math.max(0, ConfigHandler.Rendering.currentLine - 1);
				return true;
			}

		}

		static class ScrollDown implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				ConfigHandler.Rendering.currentLine = Math.max(0, Math.min(MeterManager.get(MinecraftClient.getInstance()).METERS.size() - ConfigHandler.Rendering.previewHeight.getIntegerValue(), ConfigHandler.Rendering.currentLine + 1));
				return true;
			}

		}

		static class ScrollRight implements IHotkeyCallback {
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

		static class ScrollLeft implements IHotkeyCallback {
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

		static class PausePreview implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				if(MinecraftClient.getInstance().world == null){
					return false;
				}
				ConfigHandler.Rendering.paused = !ConfigHandler.Rendering.paused;
				ConfigHandler.Rendering.cursorPosition = ConfigHandler.Generic.previewCursorPosition.getIntegerValue();
				ConfigHandler.Rendering.scrollPosition = 0;
				ConfigHandler.Rendering.pauseTick = MinecraftClient.getInstance().world.getTime();
				return true;
			}

		}

		static class ToggleMeter implements IHotkeyCallback {
			@Override
			public boolean onKeyAction (KeyAction action, IKeybind key) {
				if(MinecraftClient.getInstance().world == null || MinecraftClient.getInstance().player == null){
					return false;
				}
				//				MinecraftClient.getInstance().mouse.unlockCursor();
				HitResult pointing = MinecraftClient.getInstance().crosshairTarget;
				if (!(pointing == null) && pointing.getType() == HitResult.Type.BLOCK) {
					BlockHitResult blockHit = (BlockHitResult) pointing;
					PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
					try {
						BlockPos pos = blockHit.getBlockPos();
						Identifier id = MeterManager.get(MinecraftClient.getInstance()).getMeterId(pos, MinecraftClient.getInstance().player.world.getRegistryKey());
						MeterManager.get(MinecraftClient.getInstance()).removeMeter(MeterManager.get(MinecraftClient.getInstance()).METERS.get(id));
						buffer.writeEnumConstant(MeterManager.Action.remove);
						buffer.writeIdentifier(id);
					} catch (NullPointerException e) {
						Names.LOGGER.info("ignoring {} in RsmmInitializer$ToggleMeter.onKeyAction", e.getMessage());
					} catch (NoSuchObjectException e) {
						Meter meter = new Meter(
										blockHit.getBlockPos(),
										MinecraftClient.getInstance().world.getRegistryKey(),
										MeterManager.get(MinecraftClient.getInstance()).getNextId()
						).setColor(
										Command.Color.getNextColor().color
						).setMovable(
										!key.getKeys().contains(77)
						);
						meter.setMeterable((Meterable) (MinecraftClient.getInstance().world.getBlockState(meter.position).getBlock()));
						MeterManager.get(MinecraftClient.getInstance()).addMeter(meter);
						buffer.writeEnumConstant(MeterManager.Action.add);
						buffer.writeIdentifier(meter.id);
						buffer.writeBlockPos(meter.position);
						buffer.writeIdentifier(meter.dimension.getValue());
						buffer.writeInt(meter.color);
						buffer.writeText(meter.name);
					}
					ClientSidePacketRegistry.INSTANCE.sendToServer(Names.METER_CHANNEL, buffer);
				}
				return true;
			}

		}

	}

}
