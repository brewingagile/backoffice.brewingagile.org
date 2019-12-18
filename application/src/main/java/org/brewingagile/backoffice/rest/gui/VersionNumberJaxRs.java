package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonNode;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static argo.jdom.JsonNodeFactories.*;

@Path("/versionnumber")
@NeverCache
public class VersionNumberJaxRs {
	private final String version;

	public VersionNumberJaxRs(String version) {
		this.version = version;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		return Response.ok(ArgoUtils.format(json(version))).build();
	}

	private static JsonNode json(String version) {
		return object(field("version", string(version)));
	}
}
