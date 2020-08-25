package here.lenrik1589.rsmm.meter;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import here.lenrik1589.rsmm.Names;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class Command {
	private static final Random random = new Random();
	//	public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (commandContext, suggestionsBuilder) -> CommandSource.suggestIdentifiers(
	//					((MeterManager)commandContext.getSource().getMinecraftServer()).getMeterIds,
	//					suggestionsBuilder
	//	);

	public enum Color {
		// Colors are taken from https://cssgradient.io
		PINK("pink", 0xFF007F), // Rose
		BLUE("blue", 0x007FFF), // Azure
		RED("red", 0xFF3800), // Coquelicot
		GREEN("green", 0x32CD32), // Lime Green
		YELLOW("yellow", 0xFFD700), // Gold
		NEON_GREEN("neon_green", 0x39FF14), // Neon Green
		PURPLE("purple", 0x4B0082), // Indigo
		WHITE("white", 0xF0F8FF); // Alice Blue
		private final String name;
		public final Integer color;

		Color (String name, Integer color) {
			this.name = name;
			this.color = color;
		}

		public static Color getNextColor () {
			return Color.values()[random.nextInt(Color.values().length)];
		}
	}

	public static void register (CommandDispatcher<ServerCommandSource> dispatcher, boolean ignored) {
		dispatcher.register(
						literal(
										"meter"
						).then(
										getDefaultColorArg().then(
														argument(
																		"color",
																		IntColorArgument.color()
														).suggests(
																		(commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(
																						IntColorArgument.EXAMPLES,
																						suggestionsBuilder
																		)
														).executes(
																		(commandContext) -> setColor(
																						commandContext.getSource(),
																						Iterators.getLast(
																										MeterManager.get(commandContext.getSource().getMinecraftServer()).METERS.entrySet().iterator()
																						).getValue(),
																						IntColorArgument.getColor(
																										commandContext,
																										"color"
																						)
																		)
														).then(
																		argument(
																						"id",
																						IdentifierArgumentType.identifier()
																		).executes(
																						(commandContext) -> setColor(
																										commandContext.getSource(),
																										MeterManager.get(commandContext.getSource().getMinecraftServer()).METERS.get(
																														IdentifierArgumentType.getIdentifier(
																																		commandContext,
																																		"id"
																														)
																										),
																										IntColorArgument.getColor(
																														commandContext,
																														"color"
																										)
																						)
																		).suggests(
																						(commandContext, suggestionsBuilder) -> CommandSource.suggestIdentifiers(
																										MeterManager.get(commandContext.getSource().getMinecraftServer()).METERS.keySet(),
																										suggestionsBuilder
																						)
																		)
														)
										)
						).then(
										literal(
														"name"
										).then(
														argument(
																		"id",
																		IdentifierArgumentType.identifier()
														).then(
																		argument(
																						"name",
																						TextArgumentType.text()
																		).executes(
																						context -> setMeterName(
																										context.getSource(),
																										IdentifierArgumentType.getIdentifier(
																														context,
																														"id"
																										),
																										TextArgumentType.getTextArgument(
																														context,
																														"name"
																										)
																						)
																		)
														).suggests(
																		(commandContext, suggestionsBuilder) -> CommandSource.suggestIdentifiers(
																						MeterManager.get(commandContext.getSource().getMinecraftServer()).METERS.keySet(),
																						suggestionsBuilder
																		)
														)
										)
						).then(
										literal(
														"list"
										).executes(
														context -> listMeters(
																		context.getSource()
														)
										)
						).then(
										literal(
														"remove"
										).then(
														literal(
																		"all"
														).executes(
																		Command::clear
														)
										).then(
														argument(
																		"id",
																		IdentifierArgumentType.identifier()
														).executes(
																		context -> removeMeter(
																						context.getSource(),
																						IdentifierArgumentType.getIdentifier(
																										context,
																										"id"
																						)
																		)
														).suggests(
																		(commandContext, suggestionsBuilder) -> CommandSource.suggestIdentifiers(MeterManager.get(commandContext.getSource().getMinecraftServer()).METERS.keySet(), suggestionsBuilder)
														)
										)
						).then(
										literal(
														"add"
										).then(
														argument(
																		"position",
																		BlockPosArgumentType.blockPos()
														).then(
																		argument(
																						"id",
																						IdentifierArgumentType.identifier()
																		).then(
																						argument(
																										"name",
																										TextArgumentType.text()
																						).executes(
																										context -> addMeter(
																														context.getSource(),
																														IdentifierArgumentType.getIdentifier(
																																		context,
																																		"id"
																														),
																														BlockPosArgumentType.getBlockPos(
																																		context,
																																		"position"
																														),
																														TextArgumentType.getTextArgument(
																																		context,
																																		"name"
																														)
																										)
																						)
																		).executes(
																						context -> addMeter(
																										context.getSource(),
																										IdentifierArgumentType.getIdentifier(
																														context,
																														"id"
																										),
																										BlockPosArgumentType.getBlockPos(
																														context,
																														"position"
																										)
																						)
																		)
														).executes(
																		context -> addMeter(
																						context.getSource(),
																						BlockPosArgumentType.getBlockPos(
																										context,
																										"position"
																						)
																		)
														)
										)
						)
		);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> getDefaultColorArg () {
		LiteralArgumentBuilder<ServerCommandSource> root = literal("color");
		for (Color color : Color.values()) {
			root.then(addColor(color.name));
		}
		return root;
	}

	private static LiteralArgumentBuilder<ServerCommandSource> addColor (String name) {
		return literal(
						name
		).executes(
						(commandContext) -> setColor(
										commandContext.getSource(),
										Iterators.getLast(
														MeterManager.get(commandContext.getSource().getMinecraftServer()).METERS.entrySet().iterator()
										).getValue(),
										Color.valueOf(name.toUpperCase()).color
						)
		).then(
						argument(
										"id",
										IdentifierArgumentType.identifier()
						).executes(
										(commandContext) -> setColor(
														commandContext.getSource(),
														MeterManager.get(commandContext.getSource().getMinecraftServer()).METERS.get(
																		IdentifierArgumentType.getIdentifier(
																						commandContext,
																						"id"
																		)
														),
														Color.valueOf(name.toUpperCase()).color
										)
						).suggests(
										(commandContext, suggestionsBuilder) -> CommandSource.suggestIdentifiers(MeterManager.get(commandContext.getSource().getMinecraftServer()).METERS.keySet(), suggestionsBuilder)
						)
		);
	}

	private static int listMeters (ServerCommandSource source) {
		source.sendFeedback(
						new LiteralText(
										MeterManager.get(source.getMinecraftServer()).listMeters()
						), false);
		return 0;
	}

	private static int removeMeter (ServerCommandSource source, Identifier id) {
		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		buffer.writeEnumConstant(MeterManager.Action.remove);
		buffer.writeIdentifier(id);
		MeterManager.get(source.getMinecraftServer()).METERS.remove(id);
		source.getMinecraftServer().getPlayerManager().getPlayerList().forEach(
						player -> ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, Names.METER_CHANNEL, buffer)
		);
		return 0;
	}

	private static int clear (CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		buffer.writeEnumConstant(MeterManager.Action.clear);
		MeterManager.get(source.getMinecraftServer()).METERS.clear();
		source.getMinecraftServer().getPlayerManager().getPlayerList().forEach(
						player -> ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, Names.METER_CHANNEL, buffer)
		);
		return 0;
	}


	private static int setMeterName (ServerCommandSource source, Identifier id, Text name) {
		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		buffer.writeEnumConstant(MeterManager.Action.name);
		buffer.writeIdentifier(id);
		buffer.writeText(name);
		source.getMinecraftServer().getPlayerManager().getPlayerList().forEach(
						player -> ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, Names.METER_CHANNEL, buffer)
		);
		MeterManager.get(source.getMinecraftServer()).METERS.get(id).name = name;
		return 0;
	}

	public static int addMeter (ServerCommandSource source, BlockPos pos) throws CommandSyntaxException {
		return addMeter(source, MeterManager.get(source.getMinecraftServer()).getNextId(), pos);
	}

	public static int addMeter (ServerCommandSource source, Identifier id, BlockPos pos) throws CommandSyntaxException {
		Meter meter = new Meter(
						pos,
						source.getWorld().getRegistryKey(),
						id
		).setColor(Color.getNextColor().color);
		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		buffer.writeEnumConstant(MeterManager.Action.add);
		writeMeter(buffer, meter);
		source.getMinecraftServer().getPlayerManager().getPlayerList().forEach(
						player -> ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, Names.METER_CHANNEL, buffer)
		);
		MeterManager.get(source.getMinecraftServer()).addMeter(meter);
		return 0;
	}

	public static int addMeter (ServerCommandSource source, Identifier id, BlockPos pos, Text name) throws CommandSyntaxException {
		Meter meter = new Meter(
						pos,
						source.getWorld().getRegistryKey(),
						id,
						name
		).setColor(Color.getNextColor().color);
		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		buffer.writeEnumConstant(MeterManager.Action.add);
		writeMeter(buffer, meter);
		source.getMinecraftServer().getPlayerManager().getPlayerList().forEach(
						player -> ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, Names.METER_CHANNEL, buffer)
		);
		MeterManager.get(source.getMinecraftServer()).addMeter(meter);
		return 0;
	}

	private static int setColor (ServerCommandSource source, Meter meter, Integer color) {
		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		buffer.writeEnumConstant(MeterManager.Action.color);
		buffer.writeIdentifier(meter.id);
		buffer.writeInt(color);
		source.getMinecraftServer().getPlayerManager().getPlayerList().forEach(
						player -> ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, Names.METER_CHANNEL, buffer)
		);
		meter.color = color;
		return 0;
	}

	private static void writeMeter (PacketByteBuf buffer, Meter meter) {
		buffer.writeIdentifier(meter.id);
		buffer.writeBlockPos(meter.position);
		buffer.writeIdentifier(meter.dimension.getValue());
		buffer.writeInt(meter.color);
		buffer.writeText(meter.name);
	}
}
