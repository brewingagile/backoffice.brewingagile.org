package org.brewingagile.backoffice.auth;

import java.io.IOException;
import javax.servlet.*;

import org.brewingagile.backoffice.application.Configuration;
import org.brewingagile.backoffice.application.Main;

import com.hencjo.summer.security.*;
import com.hencjo.summer.security.api.*;
import static com.hencjo.summer.security.api.Summer.*;

public class AuthenticationFilter extends AbstractFilter {
	private final Configuration configuration = Main.getConfiguration();
	
	private final SummerLogger logger = Loggers.noop();
	private final Authenticator authenticator = (username, password) -> (configuration.guiAdminUsername.equals(username) && configuration.guiAdminPassword.equals(password));
	private final ServerSideSession session = new ServerSideSession("TOKEN");
	private final HttpBasicAuthenticator httpBasicAuthenticator = new HttpBasicAuthenticator(authenticator, "BA_BACKOFFICE");
	private final FormBasedLogin formBasedLogin = new FormBasedLogin(logger, authenticator, session.sessionWriter(), 
			"/j_spring_security_check", "/j_spring_security_logout", 
			"j_username", "j_password", 
			redirect("/login.html#?logout=true"), redirect("/login.html#?failure=true"), new RedirectToHashbang());

	private final SummerFilterDelegate filterDelegate = summer(logger,
			when(pathEquals("/form.html")).thenAllow(),
			when(pathBeginsWith("/form/")).thenAllow(),
			when(pathBeginsWith("/img/")).thenAllow(),
			when(pathBeginsWith("/js-lib/")).thenAllow(),
			when(pathBeginsWith("/lib/")).thenAllow(),
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
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		filterDelegate.doFilter(request, response, filterChain);
	}
}
