package org.brewingagile.backoffice.auth;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import com.hencjo.summer.security.SummerAuthenticatedUser;

public class AuthService {
	private final SummerAuthenticatedUser summerAuthenticatedUser;
	
	public AuthService(SummerAuthenticatedUser summerAuthenticatedUser) {
		this.summerAuthenticatedUser = summerAuthenticatedUser;
	}
	
	public String guardAuthenticatedUser(HttpServletRequest request) {
		String string = summerAuthenticatedUser.get(request);
		Optional<String> fromNullable = Optional.fromNullable(string);
		if (!fromNullable.isPresent()) throw new WebApplicationException("Not authorized", Response.Status.UNAUTHORIZED);
		return fromNullable.get();
	}
}
