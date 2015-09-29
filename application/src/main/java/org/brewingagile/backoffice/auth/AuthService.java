package org.brewingagile.backoffice.auth;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.hencjo.summer.security.SummerAuthenticatedUser;
import fj.data.Option;

public class AuthService {
	private final SummerAuthenticatedUser summerAuthenticatedUser;
	
	public AuthService(SummerAuthenticatedUser summerAuthenticatedUser) {
		this.summerAuthenticatedUser = summerAuthenticatedUser;
	}
	
	public String guardAuthenticatedUser(HttpServletRequest request) {
		String string = summerAuthenticatedUser.get(request);
		Option<String> fromNullable = Option.fromNull(string);
		if (!fromNullable.isSome()) throw new WebApplicationException("Not authorized", Response.Status.UNAUTHORIZED);
		return fromNullable.some();
	}
}
