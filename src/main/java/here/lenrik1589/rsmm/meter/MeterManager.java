package here.lenrik1589.rsmm.meter;

import java.rmi.NoSuchObjectException;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class MeterManager {
	public final LinkedHashMap<DimPos, Identifier> positionIdMap = new LinkedHashMap<>();
	public final LinkedHashMap<Identifier, Meter> METERS = new LinkedHashMap<>();

	public Identifier getMeterId (BlockPos pos, RegistryKey<World> dimension) throws NoSuchObjectException {
		Identifier id = positionIdMap.get(new DimPos(pos, dimension));
		if (id == null) {
			throw new NoSuchObjectException("maybe add it first?");
		} else {
			return id;
		}
	}

	public int listMeters (CommandContext<ServerCommandSource> context) {
		if (METERS.isEmpty()) {
			context.getSource().sendFeedback(new LiteralText("Meter list is empty."), false);
			return 1;
		}
		for (Object id : METERS.keySet().stream().sorted().toArray()) {
			Meter m = METERS.get(id);
			context.getSource().sendFeedback(new LiteralText(m.id.toString() + " : " + m.name.asString()), false);
		}
		return 0;
	}

	public enum Action {
		add,
		remove,
		name,
		color,
		clear
	}

	public Identifier getNextId () {
		if (METERS.isEmpty()) {
			return new Identifier("rsmm", "meter_0");
		} else {
			int maxIdNum = 0;
			boolean matched = false;
			for (Meter m : METERS.values()) {
				if (m.id.getNamespace().equals("rsmm")) {
					String input = m.id.getPath();
					Pattern pattern = Pattern.compile("^(?:meter_)(?<num>\\d+?)$");
					Matcher matcher = pattern.matcher(input);
					if (matcher.find()) {
						matched = true;
						int r = Integer.parseInt(matcher.group("num"));
						maxIdNum = Math.max(maxIdNum, r);
					}
				}
			}
			return new Identifier("rsmm", "meter_" + (maxIdNum + (matched ? 1 : 0)));
		}
	}

	public boolean addMeter (Meter meter) {
		if (METERS.containsKey(meter.id)) {
			return false;
		}
		METERS.put(meter.id, meter);
		positionIdMap.put(new DimPos(meter.position, meter.dimension), meter.id);

		return true;
	}

	public boolean removeMeter (Meter meter) {
		if (!METERS.containsKey(meter.id)) {
			return false;
		}
		METERS.remove(meter.id, meter);
		positionIdMap.remove(new DimPos(meter.position, meter.dimension), meter.id);

		return true;
	}

	public boolean clear () {
		if (METERS.isEmpty() || positionIdMap.isEmpty()) {
			return false;
		}
		METERS.clear();
		positionIdMap.clear();
		return true;
	}

	public Meter get(Identifier id){
		return METERS.get(id);
	}

	public static MeterManager get (MinecraftServer server) {
		return ((MeterI) server).getMeterManager();
	}

	public static MeterManager get (MinecraftClient client) {
		return ((MeterI) client).getMeterManager();
	}

}
