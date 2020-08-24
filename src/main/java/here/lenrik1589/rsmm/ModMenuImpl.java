package here.lenrik1589.rsmm;

import here.lenrik1589.rsmm.config.RsmmConfigGui;
import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;

public class ModMenuImpl implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return (screen) -> {
            RsmmConfigGui gui = new RsmmConfigGui();
            gui.setParent(screen);
            return gui;
        };
    }
}
