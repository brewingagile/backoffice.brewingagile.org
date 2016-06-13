package org.brewingagile.backoffice.application;

import fj.P2;
import fj.data.Either;
import fj.data.List;
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

	private static Either<String, String> positionArgument(String name, String[] args, int position) {
		if (args.length == 0) return Either.left(name);
		return Either.right(args[0]);
	}

	private static Option<String> getSwitchArgument(String key, String[] args) {
		String argKey = "--" + key;
		List<String> list = List.list(args);
		return list.zip(list.tail())
			.filter(k -> argKey.equals(k._1()))
			.map(P2.__2())
			.headOption();
	}
}