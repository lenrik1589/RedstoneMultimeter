package here.lenrik1589.rsmm.meter;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import here.lenrik1589.rsmm.Names;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.rmi.NoSuchObjectException;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class Command {
	private static final DynamicCommandExceptionType METER_EXISTS = new DynamicCommandExceptionType(a -> new TranslatableText("rsmm.error.meter_exists", a));
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
																						Command::suggestIdentifiers
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
																		Command::suggestIdentifiers
														)
										)
						).then(
										literal(
														"list"
										).executes(
														context -> MeterManager.get(context.getSource().getMinecraftServer()).listMeters(context)
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
																		Command::suggestIdentifiers
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
										Command::suggestIdentifiers
						)
		);
	}

	static CompletableFuture<Suggestions> suggestIdentifiers (CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
		MeterManager manager = MeterManager.get(context.getSource().getMinecraftServer());
		Set<Identifier> candidates = manager.METERS.keySet();
		String string = builder.getRemaining().toLowerCase(Locale.ROOT);
		try {
			BlockHitResult hit = (BlockHitResult) context.getSource().getPlayer().rayTrace(32, 1, false);
			Identifier id = manager.getMeterId(hit.getBlockPos(), context.getSource().getWorld().getRegistryKey());
			candidates = Set.of(id);
		} catch (CommandSyntaxException | NoSuchObjectException ignored) {
		}
		CommandSource.forEachMatching(candidates, string, identifier -> identifier, identifier -> builder.suggest(identifier.toString()));
		builder.suggest("all");
		return builder.buildFuture();
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
		CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(Names.METER_CHANNEL, buffer);
		source.getMinecraftServer().getPlayerManager().sendToAll(packet);
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
		if(MeterManager.get(source.getMinecraftServer()).addMeter(meter)) {
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			buffer.writeEnumConstant(MeterManager.Action.add);
			writeMeter(buffer, meter);
			source.getMinecraftServer().getPlayerManager().sendToAll(new CustomPayloadS2CPacket(Names.METER_CHANNEL, buffer));
//			source.getMinecraftServer().getPlayerManager().getPlayerList().forEach(
//							player -> ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, Names.METER_CHANNEL, buffer)
//			);
		}else {
			throw METER_EXISTS.create(meter.id);
		}
		return 0;
	}

	public static int addMeter (ServerCommandSource source, Identifier id, BlockPos pos, Text name) throws CommandSyntaxException {
		Meter meter = new Meter(
						pos,
						source.getWorld().getRegistryKey(),
						id,
						name
		).setColor(Color.getNextColor().color);
		if(MeterManager.get(source.getMinecraftServer()).addMeter(meter)) {
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			buffer.writeEnumConstant(MeterManager.Action.add);
			writeMeter(buffer, meter);
			source.getMinecraftServer().getPlayerManager().sendToAll(new CustomPayloadS2CPacket(Names.METER_CHANNEL, buffer));
//			source.getMinecraftServer().getPlayerManager().getPlayerList().forEach(
//							player -> ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, Names.METER_CHANNEL, buffer)
//			);
		}else{
			throw METER_EXISTS.create(meter.id);
		}
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

	public static void writeMeter (PacketByteBuf buffer, Meter meter) {
		buffer.writeIdentifier(meter.id);
		buffer.writeBlockPos(meter.position);
		buffer.writeIdentifier(meter.dimension.getValue());
		buffer.writeInt(meter.color);
		buffer.writeText(meter.name);
	}

}
