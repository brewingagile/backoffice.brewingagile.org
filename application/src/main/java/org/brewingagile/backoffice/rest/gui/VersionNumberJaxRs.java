package org.brewingagile.backoffice.rest.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import argo.jdom.JsonRootNode;
import static argo.jdom.JsonNodeFactories.*;

import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.GitPropertiesDescribeVersionNumberProvider;

import org.brewingagile.backoffice.utils.jersey.NeverCache;

@Path("/versionnumber")
@NeverCache
public class VersionNumberJaxRs {
	private final GitPropertiesDescribeVersionNumberProvider versionNumberProvider;

	public VersionNumberJaxRs(GitPropertiesDescribeVersionNumberProvider versionNumberProvider) {
		this.versionNumberProvider = versionNumberProvider;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		return Response.ok(ArgoUtils.format(json(versionNumberProvider.softwareVersion()))).build();
	}

	private static JsonRootNode json(String version) {
		return object(field("version", string(version)));
	}
}
