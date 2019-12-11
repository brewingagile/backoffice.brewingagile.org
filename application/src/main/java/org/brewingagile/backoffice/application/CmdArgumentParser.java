package org.brewingagile.backoffice.application;

import fj.data.Option;

import java.io.IOException;

public class CmdArgumentParser {
	public static final class CmdArguments {
		public final String configPropertiesFile;
		public final String secretPropertiesFile;
		public final Option<String> devOverlayDirectory;

		public CmdArguments(
			String configPropertiesFile,
			String secretPropertiesFile,
			Option<String> devOverlayDirectory
		) {
			this.configPropertiesFile = configPropertiesFile;
			this.secretPropertiesFile = secretPropertiesFile;
			this.devOverlayDirectory = devOverlayDirectory;
		}
	}

	public static CmdArguments parse(String[] args) throws IOException {
		Option<String> stringStringEither = getSwitchArgument("config-file", args);
		Option<String> s2 = getSwitchArgument("secret-file", args);
		Option<String> dev = getSwitchArgument("dev", args);
		if (stringStringEither.isNone()) throw new IOException("Missing mandatory argument --config-file=");
		if (stringStringEither.isNone()) throw new IOException("Missing mandatory argument --secret-file=");
		return new CmdArguments(stringStringEither.some(), s2.some(), dev);
	}

	private static Option<String> getSwitchArgument(String c, String[] args) {
		for (String arg : args) {
			String prefix = "--" + c + "=";
			if (arg.startsWith(prefix)) {
				return Option.some(arg.substring(prefix.length()));
			}
		}
		return Option.none();
	}
}