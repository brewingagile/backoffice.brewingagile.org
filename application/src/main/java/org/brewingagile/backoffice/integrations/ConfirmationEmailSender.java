package org.brewingagile.backoffice.integrations;

import com.hencjo.summer.security.utils.Charsets;
import fj.data.Array;
import fj.data.Either;
import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.brewingagile.backoffice.application.Configuration;
import org.brewingagile.backoffice.types.ParticipantEmail;
import org.brewingagile.backoffice.utils.Resources2;
import org.eclipse.jetty.util.IO;

import java.io.IOException;
import java.io.InputStream;

public class ConfirmationEmailSender {
	private final Configuration configuration;

	public ConfirmationEmailSender(Configuration configuration) {
		this.configuration = configuration;
	}

	public Either<String, String> email( ParticipantEmail toEmail, Array<Attachment> attachments ) {
		try {
			catchAll(toEmail.value, attachments);
			return Either.right("OK");
		} catch (Exception e) {
			e.printStackTrace();
			return Either.left(e.getMessage());
		}
	}

	public static final class Attachment {
		public final String name;
		public final String contentType;
		public final byte[] contents;

		public Attachment(String name, String contentType, byte[] contents) {
			this.name = name;
			this.contentType = contentType;
			this.contents = contents;
		}
	}

	private void catchAll(String toEmail, Array<Attachment> attachments) throws EmailException, IOException {
		String subject = "Brewing Agile 2020: Registration Received";

		Configuration.Smtp smtp = configuration.smtp;

		HtmlEmail email = new HtmlEmail();
		email.setCharset("UTF-8");
		email.setHostName(smtp.server);
		email.setSSLOnConnect(true);
		email.setSslSmtpPort(Integer.toString(smtp.sslPort));
		email.setAuthentication(smtp.username, smtp.password);
		email.addTo(toEmail);
		email.setFrom(configuration.emailFromEmail, configuration.emailFromName);
		email.setSubject(subject);

		email.setHtmlMsg(template("/email-templates/registration-confirmation/email-processed.html"));
		email.setTextMsg(template("/email-templates/registration-confirmation/email-processed.txt"));
		for (Attachment a : attachments) email.attach(new ByteArrayDataSource(a.contents, a.contentType), a.name, "", EmailAttachment.ATTACHMENT);
		email.send();
	}

	private String template(String template) throws IOException {
		try (InputStream inputStream = Resources2.resourceAsStream(ConfirmationEmailSender.class, template)) {
			 return IO.toString(inputStream, Charsets.UTF8.name());
		}
	}
}
