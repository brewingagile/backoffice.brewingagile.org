package org.brewingagile.backoffice.integrations;

import com.hencjo.summer.security.utils.Charsets;
import fj.data.Either;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.brewingagile.backoffice.application.Configuration;
import org.brewingagile.backoffice.utils.Resources2;
import org.eclipse.jetty.util.IO;

import java.io.IOException;
import java.io.InputStream;

public class ConfirmationEmailSender {
	private final Configuration configuration;

	public ConfirmationEmailSender(Configuration configuration) {
		this.configuration = configuration;
	}

	public Either<String, String> email(String toEmail) {
		try {
			catchAll(toEmail);
			return Either.right("OK");
		} catch (Exception e) {
			e.printStackTrace();
			return Either.left(e.getMessage());
		}
	}

	private void catchAll(String toEmail) throws EmailException, IOException {
		String subject = "BrewingAgile 2017: Registration Received";

		HtmlEmail email = new HtmlEmail();
		email.setCharset("UTF-8");
		email.setHostName("smtp.gmail.com");
		email.setSSLOnConnect(true);
		email.setSslSmtpPort("465");
		email.setAuthentication(configuration.gmailUser, configuration.gmailPassword);
		email.addTo(toEmail);
		email.setFrom(configuration.emailFromEmail, configuration.emailFromName);
		email.setSubject(subject);

		email.setHtmlMsg(template("/email-templates/registration-confirmation/email-processed.html"));
		email.setTextMsg(template("/email-templates/registration-confirmation/email-processed.txt"));
		email.send();
	}

	private String template(String template) throws IOException {
		try (InputStream inputStream = Resources2.resourceAsStream(ConfirmationEmailSender.class, template)) {
			 return IO.toString(inputStream, Charsets.UTF8.name());
		}
	}
}
