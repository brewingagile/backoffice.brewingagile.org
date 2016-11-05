package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonRootNode;
import com.squareup.okhttp.*;
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
		JsonRootNode request9 = object(
			field("text", string(text)),
			field("channel", string(channel)), //@username, #channel
			field("username", string(botUsername)),
			field("icon_emoji", string(":monkey_face:"))
		);

		Request request = new Request.Builder()
			.url(hookUrl)
			.addHeader("Accept", "application/json")
			.cacheControl(CacheControl.FORCE_NETWORK)
			.post(RequestBody.create(com.squareup.okhttp.MediaType.parse("application/json"), ArgoUtils.format(request9)))
			.build();

		com.squareup.okhttp.Response execute = client.newCall(request).execute();
		if (!execute.isSuccessful() || execute.isRedirect())
			throw new IOException("Call to " + hookUrl + " failed unexpectedly: " + execute);
	}
}
