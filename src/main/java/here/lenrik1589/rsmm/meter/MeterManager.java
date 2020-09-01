package here.lenrik1589.rsmm.meter;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import here.lenrik1589.rsmm.config.ConfigHandler;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.booleans.BooleanLists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.rmi.NoSuchObjectException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MeterManager {
	public final LinkedHashMap<Identifier, Meter> METERS = new LinkedHashMap<>();
	private static final DynamicCommandExceptionType METER_EXISTS = new DynamicCommandExceptionType(a -> new TranslatableText("rsmm.error.meter_exists", a));

	public int cursorPosition = ConfigHandler.Generic.previewCursorPosition.getIntegerValue();
	public LinkedHashMap<Identifier, BooleanList> cachedPreviewHistory = new LinkedHashMap<>();
	public LinkedHashMap<Identifier, BooleanList> meterStateHistory = new LinkedHashMap<>();

	public Identifier getMeterId (BlockPos pos, RegistryKey<World> dimension) throws NoSuchObjectException {
		for (Map.Entry<Identifier, Meter> entry : METERS.entrySet()) {
			Meter m = entry.getValue();
			if (m.dimension == dimension && m.position.equals(pos)) {
				return m.id;
			}
		}
		throw new NoSuchObjectException("maybe add it first?");
	}

	public int listMeters (CommandContext<ServerCommandSource> context) {
		if(METERS.isEmpty()){
			context.getSource().sendFeedback(new LiteralText("Meter list is empty."), false);
			return 1;
		}
		for (Object id : METERS.keySet().stream().sorted().toArray()) {
			Meter m = METERS.get(id);
			context.getSource().sendFeedback(new LiteralText(m.id.toString() + " : " + m.name.asString()), false);
		}
		return 0;
	}

	public Meter getMeter (BlockPos pos, RegistryKey<World> dimesion) throws NoSuchObjectException {
		return METERS.get(getMeterId(pos, dimesion));
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

	public void addMeter (Meter meter) throws CommandSyntaxException {
		if (METERS.containsKey(meter.id))
			throw METER_EXISTS.create(meter.id.toString());
		METERS.put(meter.id, meter);
		meterStateHistory.put(meter.id, BooleanLists.EMPTY_LIST);

	}

	public String listMeters () {
		StringBuilder list = new StringBuilder();
		for (Object id : METERS.keySet().stream().sorted().toArray()) {
			Meter m = METERS.get(id);
			list.append(m.id.getPath()).append(" : ").append(m.name.asString()).append('\n');
		}
		return list.toString().replaceAll("\\n$", "");
	}

	public static int clear (CommandContext<ServerCommandSource> context) {
		((MeterI) context.getSource().getMinecraftServer()).getMeterManager().clear();
		return 0;
	}

	public void clear () {
		METERS.clear();
	}

	public static MeterManager get (Object server) {
		MinecraftServer actualServer = (MinecraftServer) server;
		return ((MeterI) actualServer).getMeterManager();
	}

	public static MeterManager get (MinecraftClient client) {
		return ((MeterI) client).getMeterManager();
	}

}
