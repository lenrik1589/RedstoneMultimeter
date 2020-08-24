package here.lenrik1589.rsmm;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Names {
	public static final String ModId = "rsmm";
	public static final Identifier METER_CHANNEL = new Identifier(ModId, "meter");
	public static final Identifier EVENT_CHANNEL = new Identifier(ModId, "event");
	public static final List<Identifier> CHANNELS = ImmutableList.of(
					EVENT_CHANNEL,
					METER_CHANNEL
	);
	public static final Logger LOGGER = LogManager.getLogger(ModId);
}
