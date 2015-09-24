package org.brewingagile.backoffice.application;

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
	public final String mandrillEndpoint;
	public final String mandrillApikey;
	public final String mailchimpEndpoint;
	public final String mailchimpApikey;

	public Configuration(String dbName, String dbHost, int dbPort, String dbUsername, String dbPassword, 
			String guiAdminUsername, String guiAdminPassword, 
			String outvoiceInvoicesEndpoint, String outvoiceInvoicesApikey,
			String mandrillEndpoint, String mandrillApikey,
			String mailchimpEndpoint, String mailchimpApikey
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
		this.mandrillEndpoint = mandrillEndpoint;
		this.mandrillApikey = mandrillApikey;
		this.mailchimpEndpoint = mailchimpEndpoint;
		this.mailchimpApikey = mailchimpApikey;
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
			etc.string("mandrill.endpoint"),
			etc.string("mandrill.apikey"),
			etc.string("mailchimp.endpoint"),
			etc.string("mailchimp.apikey")
		);
	}
}
