package here.lenrik1589.rsmm.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import here.lenrik1589.rsmm.Names;

import java.io.File;

public class ConfigHandler implements IConfigHandler {
	private static final String CONFIG_FILE_NAME = Names.ModId + ".json";

	public static class Generic {
		public static final ConfigHotkey  meterKey   = new ConfigHotkey("meterKey",       "M",     "Toggle meter placement.");
		public static final ConfigHotkey  pauseKey   = new ConfigHotkey("pauseKey",       "P",     "Pause Preview autoscroll.");
		public static final ConfigHotkey  previewKey = new ConfigHotkey("previewKey",     "H",     "Hide preview.");
		public static final ConfigHotkey  leftKey    = new ConfigHotkey("leftKey",        "LEFT",  "Scroll preview to the left.");
		public static final ConfigHotkey  rightKey   = new ConfigHotkey("rightKey",       "RIGHT", "Scroll preview to the right.");
		public static final ConfigHotkey  openConfig = new ConfigHotkey("openGuiConfigs", "N,C",   "Open the in-game malilib config GUI");
		public static final ConfigInteger maxHistory = new ConfigInteger("maxHistory", 100000, 100, 1000000, "Maximum amount of ticks that will be stored in preview.\n72000 ticks are equal to one hour.");
		public static final ConfigInteger preview    = new ConfigInteger("Preview Length", 60,1,100, false,"How many ticks to show in preview");

		public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(
						meterKey,
						pauseKey,
						previewKey,
						leftKey,
						rightKey,
						openConfig,
						maxHistory,
						preview
		);
	}

	public static class Debug {
		public static final ConfigBoolean KEYBIND_DEBUG           = new ConfigBoolean("keybindDebugging", false, "When enabled, key presses and held keys are\nprinted to the game console (and the action bar, if enabled)");
		public static final ConfigBoolean KEYBIND_DEBUG_ACTIONBAR = new ConfigBoolean("keybindDebuggingIngame", true, "If enabled, then the messages from 'keybindDebugging'\nare also printed to the in-game action bar");

		public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(
						KEYBIND_DEBUG,
						KEYBIND_DEBUG_ACTIONBAR
		);
	}

	public static void loadFromFile () {
		File configFile = new File(FileUtils.getConfigDirectory(), CONFIG_FILE_NAME);

		if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
			JsonElement element = JsonUtils.parseJsonFile(configFile);

			if (element != null && element.isJsonObject()) {
				JsonObject root = element.getAsJsonObject();

				ConfigUtils.readConfigBase(root, "Generic", ConfigHandler.Generic.OPTIONS);
			}
		}
	}

	public static void saveToFile () {
		File dir = FileUtils.getConfigDirectory();

		if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
			JsonObject root = new JsonObject();

			ConfigUtils.writeConfigBase(root, "Generic", ConfigHandler.Generic.OPTIONS);

			JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
		}
	}

	@Override
	public void onConfigsChanged () {
		saveToFile();
		loadFromFile();
	}

	@Override
	public void load () {
		loadFromFile();
	}

	@Override
	public void save () {
		saveToFile();
	}

}
