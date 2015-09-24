package org.brewingagile.backoffice.rest.gui;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.brewingagile.backoffice.application.Application;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.utils.JsonReaderWriter;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

@Path("/loggedin")
@NeverCache
public class LoggedInRestService {
	private final JsonReaderWriter jsonReaderWriter;	
	private final AuthService authService;

	public LoggedInRestService() {
		this.jsonReaderWriter = new JsonReaderWriter();
		this.authService = Application.INSTANCE.authService();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLoggedIn(@Context HttpServletRequest request) throws Exception {
		String user = authService.guardAuthenticatedUser(request);
		return Response.ok(jsonReaderWriter.serialize(loggedInInfo(user))).build();
	}

	private ObjectNode loggedInInfo(String username) {
		return JsonNodeFactory.instance.objectNode()
				.put("username", username);
	}
}
