package org.brewingagile.backoffice.application;

import fj.data.Either;
import fj.data.Option;

public class CmdArgumentParser {

	public static final class CmdArguments {
		public final String propertiesFile;
		public final Option<String> devOverlayDirectory;

		public CmdArguments(String propertiesFile, Option<String> devOverlayDirectory) {
			this.propertiesFile = propertiesFile;
			this.devOverlayDirectory = devOverlayDirectory;
		}
	}

	public static Either<String, CmdArguments> parse(String[] args) {
		return positionArgument("properties-file", args, 1)
			.right().map(r -> new CmdArguments(r, getSwitchArgument("dev", args)));
	}

	private static boolean contains(String args[], String find) {
		for (String arg : args) { if (find.equals(arg)) return true; }
		return false;
	}

	private static Either<String, String> positionArgument(String name, String[] args, int position) {
		if (args.length == 0) return Either.left(name);
		return Either.right(args[0]);
	}

	private static Option<String> getSwitchArgument(String c, String[] args) {
		for (String arg : args) { if (arg.startsWith("--"+c+" ")) return Option.some(arg.substring(2 + c.length())); }
		return Option.none();
	}
}