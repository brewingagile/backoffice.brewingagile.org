package org.brewingagile.backoffice.auth;

import java.io.IOException;
import javax.servlet.*;

import org.brewingagile.backoffice.application.Configuration;

import com.hencjo.summer.security.*;
import com.hencjo.summer.security.api.*;
import static com.hencjo.summer.security.api.Summer.*;

public class AuthenticationFilter extends AbstractFilter {
	private final SummerFilterDelegate filterDelegate;

	public AuthenticationFilter(Configuration configuration) {
		SummerLogger logger = Loggers.noop();
		Authenticator authenticator = (username, password) -> (configuration.guiAdminUsername.equals(username) && configuration.guiAdminPassword.equals(password));
		ServerSideSession session = new ServerSideSession("TOKEN");
		HttpBasicAuthenticator httpBasicAuthenticator = new HttpBasicAuthenticator(authenticator, "BA_BACKOFFICE");
		FormBasedLogin formBasedLogin = new FormBasedLogin(logger, authenticator, session.sessionWriter(),
			"/j_spring_security_check", "/j_spring_security_logout",
			"j_username", "j_password",
			redirect("/login.html#?logout=true"), redirect("/login.html#?failure=true"), new RedirectToHashbang());

		this.filterDelegate = summer(logger,
			when(pathEquals("/stripe.html")).thenAllow(),
			when(pathEquals("/form.html")).thenAllow(),
			when(pathBeginsWith("/form/")).thenAllow(),
			when(pathBeginsWith("/stripe/")).thenAllow(),
			when(pathBeginsWith("/img/")).thenAllow(),
			when(pathBeginsWith("/js-lib/")).thenAllow(),
			when(pathBeginsWith("/lib/")).thenAllow(),
			when(pathBeginsWith("/css/")).thenAllow(),
			when(pathBeginsWith("/links/")).thenAllow(),
			when(pathBeginsWith("/email/")).thenAllow(),
			when(pathBeginsWith("/api/")).thenAllow(),
			when(pathEquals("/gui/versionnumber")).thenAllow(),
			when(pathEquals("/login.html")).thenAllow(),
			when(pathEquals("/login.js")).thenAllow(),
			when(formBasedLogin.logoutRequest()).then(formBasedLogin.performLogoutRequest()),
			when(formBasedLogin.loginRequest()).then(formBasedLogin.performLoginRequest()),
			when(session.exists()).thenAllow(),
			when(httpBasicAuthenticator.authorizes()).thenAllow(),
			when(header("X-Requested-With").equals("XMLHttpRequest")).then(status(403)),
			otherwise().then(redirect("/login.html"))
		);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		filterDelegate.doFilter(request, response, filterChain);
	}
}
