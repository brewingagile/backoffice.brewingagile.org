package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonNode;
import okhttp3.*;
import org.brewingagile.backoffice.utils.ArgoUtils;

import java.io.IOException;

import static argo.jdom.JsonNodeFactories.*;

public class SlackBotHook {
	private final OkHttpClient client;
	private final HttpUrl hookUrl;
	private final String botUsername;
	private final String channel;

	public SlackBotHook(OkHttpClient client, HttpUrl hookUrl, String botUsername, String channel) {
		this.client = client;
		this.hookUrl = hookUrl;
		this.botUsername = botUsername;
		this.channel = channel;
	}

	public void post(String text) throws IOException {
		JsonNode request9 = object(
			field("text", string(text)),
			field("channel", string(channel)), //@username, #channel
			field("username", string(botUsername)),
			field("icon_emoji", string(":monkey_face:"))
		);

		Request request = new Request.Builder()
			.url(hookUrl)
			.addHeader("Accept", "application/json")
			.cacheControl(CacheControl.FORCE_NETWORK)
			.post(RequestBody.create(MediaType.parse("application/json"), ArgoUtils.format(request9)))
			.build();

		try (Response execute = client.newCall(request).execute()) {
			if (!execute.isSuccessful() || execute.isRedirect())
				throw new IOException("Call to " + hookUrl + " failed unexpectedly: " + execute);
		}
	}
}
