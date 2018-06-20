package org.brewingagile.backoffice.application;

import okhttp3.HttpUrl;
import org.brewingagile.backoffice.integrations.MailchimpSubscribeClient;
import org.brewingagile.backoffice.types.StripePrivateKey;
import org.brewingagile.backoffice.types.StripePublishableKey;
import org.brewingagile.backoffice.utils.EtcPropertyFile;

public class Configuration {
	public final String dbName;
	public final String dbHost;
	public final int dbPort;
	public final String dbUsername;
	public final String dbPassword;
	public final String guiAdminUsername;
	public final String guiAdminPassword;
	public final String outvoiceInvoicesEndpoint;
	public final String outvoiceInvoicesApikey;
	public final String emailFromEmail;
	public final String emailFromName;
	public final String emailsConfirmationSubject;
	public final String gmailUser;
	public final String gmailPassword;
	public final String mailchimpEndpoint;
	public final String mailchimpApikey;
	public final HttpUrl slackBotHookUrl;
	public final String slackBotName;
	public final String slackBotChannel;
	public final StripePublishableKey stripePublishableKey;
	public final StripePrivateKey stripePrivateKey;
	public final MailchimpSubscribeClient.ListUniqueId mailchimpList;

	public Configuration(
		String dbName, String dbHost, int dbPort, String dbUsername, String dbPassword,
		String guiAdminUsername, String guiAdminPassword,
		String outvoiceInvoicesEndpoint, String outvoiceInvoicesApikey,
		String emailFromEmail, String emailFromName,
		String emailsConfirmationSubject,
		String gmailUser,
		String gmailPassword,
		String mailchimpEndpoint,
		String mailchimpApikey,
		HttpUrl slackBotHookUrl,
		String slackBotName,
		String slackBotChannel,
		StripePublishableKey stripePublishableKey,
		StripePrivateKey stripePrivateKey,
		MailchimpSubscribeClient.ListUniqueId mailchimpList
	) {
		this.dbName = dbName;
		this.dbHost = dbHost;
		this.dbPort = dbPort;
		this.dbUsername = dbUsername;
		this.dbPassword = dbPassword;
		this.guiAdminUsername = guiAdminUsername;
		this.guiAdminPassword = guiAdminPassword;
		this.outvoiceInvoicesEndpoint = outvoiceInvoicesEndpoint;
		this.outvoiceInvoicesApikey = outvoiceInvoicesApikey;
		this.emailFromEmail = emailFromEmail;
		this.emailFromName = emailFromName;
		this.emailsConfirmationSubject = emailsConfirmationSubject;
		this.gmailUser = gmailUser;
		this.gmailPassword = gmailPassword;
		this.mailchimpEndpoint = mailchimpEndpoint;
		this.mailchimpApikey = mailchimpApikey;
		this.slackBotHookUrl = slackBotHookUrl;
		this.slackBotName = slackBotName;
		this.slackBotChannel = slackBotChannel;
		this.stripePublishableKey = stripePublishableKey;
		this.stripePrivateKey = stripePrivateKey;
		this.mailchimpList = mailchimpList;
	}
	
	public static Configuration from(EtcPropertyFile etc) {
		return new Configuration(
			etc.string("db.name"),
			etc.string("db.host"),
			etc.integer("db.port"),
			etc.string("db.username"),
			etc.string("db.password"),
			etc.string("gui.admin.username"),
			etc.string("gui.admin.password"),
			etc.string("outvoice.invoices.endpoint"),
			etc.string("outvoice.invoices.apikey"),
			etc.string("email.from.email"),
			etc.string("email.from.name"),
			etc.string("emails.confirmation.subject"),
			etc.string("gmail.user"),
			etc.string("gmail.password"),
			etc.string("mailchimp.endpoint"),
			etc.string("mailchimp.apikey"),
			HttpUrl.parse(etc.string("slackbot.hookurl")),
			etc.string("slackbot.name"),
			etc.string("slackbot.channel"),
			StripePublishableKey.stripePublishableKey(etc.string("stripe.key.publishable")),
			StripePrivateKey.stripePrivateKey(etc.string("stripe.key.secret")),
			MailchimpSubscribeClient.ListUniqueId.listUniqueId(etc.string("mailchimp.list"))
		);
	}
}
