package here.lenrik1589.rsmm;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import here.lenrik1589.rsmm.config.ConfigHandler;

import java.util.List;

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

}
