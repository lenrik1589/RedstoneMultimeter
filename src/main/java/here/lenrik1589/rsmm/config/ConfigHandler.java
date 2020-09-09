package here.lenrik1589.rsmm.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import here.lenrik1589.rsmm.Names;
import here.lenrik1589.rsmm.meter.MeterManager;
import here.lenrik1589.rsmm.meter.Render;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public class ConfigHandler implements IConfigHandler {
	private static final String CONFIG_FILE_NAME = Names.ModId + ".json";

	public static class Rendering {
		public static       int           currentLine           = 0;
		public static       int           cursorPosition        = 0;
		public static       int           scrollPosition        = 0;
		public static       long          pauseTick             = 0;
		public static       boolean       visible               = true;
		public static       boolean       paused                = false;
		public static final IntegerConfig tickWidth             = new IntegerConfig("Tick Width",          3, 3, 10, false, "How wide is each tick in preview.");
		public static final ConfigColor   selectedTick          = new ConfigColor(  "Selected Tick Color", "0xfff0f8ff", "Outline color for currently selected tick.");
		public static final ConfigColor   outlineColor          = new ConfigColor(  "Grid Outline Color",  "0xff2c2b2b", "Background for bright meters");
		public static final ConfigColor   backDark              = new ConfigColor(  "Dark Background",     "0xff222121", "Background for bright meters");
		public static final IntegerConfig previewHeight         = new IntegerConfig("Preview Height",      0, 0, 100, false, "");
		public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(
						tickWidth,
						selectedTick,
						outlineColor,
						backDark,
						previewHeight
		);
	}

	public static class Generic {
		public static final ConfigHotkey  meterKey               = new ConfigHotkey( "Meter Key",               "M",                                              "Toggle meter placement.");
		public static final ConfigHotkey  pauseKey               = new ConfigHotkey( "pause Key",               "P",                                              "Pause Preview autoscroll.");
		public static final ConfigHotkey  previewKey             = new ConfigHotkey( "Preview Key",             "H",                                              "Hide preview.");
		public static final ConfigHotkey  leftKey                = new ConfigHotkey( "Scroll Left Key",         "LEFT",                                           "Scroll preview to the left.");
		public static final ConfigHotkey  rightKey               = new ConfigHotkey( "Scroll Right Key",        "RIGHT",                                          "Scroll preview to the right.");
		public static final ConfigHotkey  upKey                  = new ConfigHotkey( "Scroll Up Key",           "UP",                                             "Scroll preview up.");
		public static final ConfigHotkey  downKey                = new ConfigHotkey( "Scroll Down Key",         "DOWN",                                           "Scroll preview down.");
		public static final ConfigHotkey  openConfig             = new ConfigHotkey( "Open Config GUI",         "N,C",                                            "Open the in-game malilib config GUI");
		public static final IntegerConfig maxHistory             = new IntegerConfig("Max History Length",      100000, 100, 1000000,                 "Maximum amount of ticks that will be stored in preview.\n72000 ticks are equal to one hour.");
		public static final IntegerConfig previewLength          = new IntegerConfig("Preview Length",          60,     1,   100,     false,"How many ticks to show in preview.");
		public static final IntegerConfig previewCursorPosition  = new IntegerConfig("Preview Cursor Position", 8,      1,   20,      false,"Default cursor position in preview mode.");
		public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(
						meterKey,
						pauseKey,
						previewKey,
						leftKey,
						rightKey,
						upKey,
						downKey,
						openConfig,
						maxHistory,
						previewLength,
						previewCursorPosition
		);
	}

	public static void loadFromFile () {
		File configFile = new File(FileUtils.getConfigDirectory(), CONFIG_FILE_NAME);

		if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
			JsonElement element = JsonUtils.parseJsonFile(configFile);

			if (element != null && element.isJsonObject()) {
				JsonObject root = element.getAsJsonObject();
				ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
				ConfigUtils.readConfigBase(root, "Rendering", Rendering.OPTIONS);
			}
		}
		if(!Rendering.paused)
			Rendering.cursorPosition = Generic.previewCursorPosition.getIntegerValue();
	}

	public static void saveToFile () {
		File dir = FileUtils.getConfigDirectory();

		if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
			JsonObject root = new JsonObject();

			ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
			ConfigUtils.writeConfigBase(root, "Rendering", Rendering.OPTIONS);

			JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
		}
	}

	@Override
	public void onConfigsChanged () {
		Rendering.currentLine = Rendering.previewHeight.getIntegerValue() == 0? 0 : Rendering.currentLine;
		Generic.previewCursorPosition.setMaxValue(Generic.previewLength.getIntegerValue());
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
