package here.lenrik1589.rsmm.meter;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.*;
import here.lenrik1589.rsmm.Names;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntColorArgument implements ArgumentType<Integer> {
	private static final SimpleCommandExceptionType MALFORMED_COLOR = new SimpleCommandExceptionType(new TranslatableText("rsmm.error.malformed_color"));
	private static final Dynamic2CommandExceptionType TOO_BIG_VALUE = new Dynamic2CommandExceptionType(IntColorArgument::getExceptionText);

	private static TranslatableText getExceptionText(Object value, Object key){
		return new TranslatableText("rsmm.error.too_big_value", value, key);
	}

	public static IntColorArgument color () {
		return new IntColorArgument();
	}

	public static Integer getColor (CommandContext<ServerCommandSource> context, String name) {
		return context.getArgument(name, Integer.class);
	}

	@Override
	public Integer parse (StringReader reader) throws CommandSyntaxException {
		int out;
		int argBeginning = reader.getCursor(); // The starting position of the cursor is at the beginning of the argument.
		boolean isArray = reader.canRead() && reader.peek() == '[';
		if (!reader.canRead()) {
			reader.skip();
		}
		while (reader.canRead() && reader.peek() != (isArray ? ']' : ' ')) {
			reader.skip();
		}
		if (isArray)
			reader.skip();
		String input = reader.getString().substring(argBeginning, reader.getCursor());
		if (isArray) {
			input = input.replaceAll("[\\[\\t\\n \\]]", "");
			Pattern pattern = Pattern.compile("^(?:(?:(?:(?:\"r\"|r):(?<r>\\d{1,3}),?)|(?:(?:\"g\"|g):(?<g>\\d{1,3}),?)|(?:(?:\"b\"|b):(?<b>\\d{1,3}),?)){3}|(?<sr>\\d{1,3}),(?<sg>\\d{1,3}),(?<sb>\\d{1,3}))$");
			Matcher matcher = pattern.matcher(input);
			if (matcher.find()) {
				int r = Integer.parseInt(matcher.group("r") == null ? matcher.group("sr") : matcher.group("r"));
				int g = Integer.parseInt(matcher.group("g") == null ? matcher.group("sg") : matcher.group("g"));
				int b = Integer.parseInt(matcher.group("b") == null ? matcher.group("sb") : matcher.group("b"));
				if (r > 255) {
					throw TOO_BIG_VALUE.create(r, "§c§lRed§r");
				} else if (g > 255) {
					throw TOO_BIG_VALUE.create(g, "§a§lGreen§r");
				} else if (b > 255) {
					throw TOO_BIG_VALUE.create(b, "§9§lBlue§r");
				}
				out = (r << 16) + (g << 8) + b;
			} else {
				throw MALFORMED_COLOR.create();
			}
		} else {
			try {
				out = Integer.parseInt(input.replace("0x", "").replace("#", ""), (input.startsWith("#") || input.startsWith("0x")) ? 16 : 10);
			} catch (NumberFormatException e) {
				throw MALFORMED_COLOR.create();
			}
		}
		return out;
	}

	public static final Collection<String> EXAMPLES = ImmutableList.of(
					"#FAB41C", // hex color
					"1425514", // plain int for nerds
					"[100, 255, 13]", // most adequate, array representation
					"[\"r\":194, \"g\":17, \"b\":136]" // wait, what, ahh, yes, 2xsaiko
	);

	@Override
	public Collection<String> getExamples () {
		return EXAMPLES;
	}

}
