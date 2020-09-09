package here.lenrik1589.rsmm.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import here.lenrik1589.rsmm.Names;

import java.util.Collections;
import java.util.List;

public class RsmmConfigGui extends GuiConfigsBase {
	private static ConfigGuiTab tab = ConfigGuiTab.GENERIC;

	public RsmmConfigGui () {
		super(10, 50, Names.ModId, null, "rsmm.gui.title.configs");
	}

	@Override
	public void initGui () {
		super.initGui();

		this.clearOptions();

		int x = 10;
		int y = 26;

		for (ConfigGuiTab tab : ConfigGuiTab.values()) {
			x += this.createButton(x, y, -1, tab) + 2;
		}
	}

	private int createButton (int x, int y, int width, ConfigGuiTab tab) {
		ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
		button.setEnabled(RsmmConfigGui.tab != tab);
		this.addButton(button, new ButtonListener(tab, this));

		return button.getWidth();
	}

	@Override
	protected int getConfigWidth () {
		ConfigGuiTab tab = RsmmConfigGui.tab;

		if (tab == ConfigGuiTab.GENERIC) {
			return 200;
		}

		return super.getConfigWidth();
	}

	@Override
	public List<ConfigOptionWrapper> getConfigs () {
		List<? extends IConfigBase> configs;
		ConfigGuiTab tab = RsmmConfigGui.tab;

		if (tab == ConfigGuiTab.GENERIC) {
			configs = ConfigHandler.Generic.OPTIONS;
		} else if (tab == ConfigGuiTab.RENDERING) {
			configs = ConfigHandler.Rendering.OPTIONS;
		} else if (tab == ConfigGuiTab.DEBUG){
			configs = ConfigHandler.Debug.OPTIONS;
		}else{
			return Collections.emptyList();
		}

		return ConfigOptionWrapper.createFor(configs);
	}

	private static class ButtonListener implements IButtonActionListener {
		private final RsmmConfigGui parent;
		private final ConfigGuiTab tab;

		public ButtonListener (ConfigGuiTab tab, RsmmConfigGui parent) {
			this.tab = tab;
			this.parent = parent;
		}

		@Override
		public void actionPerformedWithButton (ButtonBase button, int mouseButton) {
			RsmmConfigGui.tab = this.tab;

			this.parent.reCreateListWidget(); // apply the new config width
			this.parent.getListWidget().resetScrollbarPosition();
			this.parent.initGui();
		}

	}

	public enum ConfigGuiTab {
		GENERIC("rsmm.gui.generic"),
		RENDERING("rsmm.gui.rendering"),
		DEBUG("rsmm.gui.debug");

		private final String translationKey;

		ConfigGuiTab (String translationKey) {
			this.translationKey = translationKey;
		}

		public String getDisplayName () {
			return StringUtils.translate(this.translationKey);
		}
	}

}
