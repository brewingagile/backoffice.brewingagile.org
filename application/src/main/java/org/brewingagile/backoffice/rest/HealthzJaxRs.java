package org.brewingagile.backoffice.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("")
public class HealthzJaxRs {
	@GET
	public Response get() {
		return Response.ok().build();
	}
}
