package org.brewingagile.backoffice.rest.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.brewingagile.backoffice.application.Application;
import org.brewingagile.backoffice.utils.GitPropertiesDescribeVersionNumberProvider;
import org.brewingagile.backoffice.utils.JsonReaderWriter;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

@Path("/versionnumber")
@NeverCache
public class VersionNumberRestService {
	private final JsonReaderWriter jsonReaderWriter;
	private final GitPropertiesDescribeVersionNumberProvider versionNumberProvider;	

	public VersionNumberRestService() {
		this.jsonReaderWriter = new JsonReaderWriter();
		this.versionNumberProvider = Application.INSTANCE.versionNumberProvider();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		return Response.ok(jsonReaderWriter.serialize(json(versionNumberProvider.softwareVersion()))).build();
	}

	private static ObjectNode json(String version) {
		return JsonNodeFactory.instance.objectNode().put("version", version);
	}
}
