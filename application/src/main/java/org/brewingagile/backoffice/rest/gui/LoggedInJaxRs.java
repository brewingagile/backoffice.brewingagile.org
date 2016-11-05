package org.brewingagile.backoffice.rest.gui;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import argo.jdom.JsonRootNode;
import org.brewingagile.backoffice.auth.AuthService;

import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import static argo.jdom.JsonNodeFactories.*;

@Path("/loggedin")
@NeverCache
public class LoggedInJaxRs {
	private final AuthService authService;

	public LoggedInJaxRs(AuthService authService) {
		this.authService = authService;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLoggedIn(@Context HttpServletRequest request) throws Exception {
		String user = authService.guardAuthenticatedUser(request);
		return Response.ok(ArgoUtils.format(loggedInInfo(user))).build();
	}

	private JsonRootNode loggedInInfo(String username) {
		return object(
			field("username", string(username))
		);
	}
}
