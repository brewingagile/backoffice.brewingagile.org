package org.brewingagile.backoffice.utils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import com.google.common.base.Optional;

public class Responses {

	public static ResponseBuilder from(JsonReaderWriter jsonReaderWriter, Optional<? extends Object> o) {
		if (!o.isPresent()) return Response.status(Status.NOT_FOUND);
		return Response.ok(jsonReaderWriter.serialize(o.get()));
	}

	public static Response response(Optional<? extends Object> o) {
		if (!o.isPresent()) return Response.status(Status.NOT_FOUND).build();
		return Response.ok(o.get()).build();
	}
	
	public static ResponseBuilder from(JsonReaderWriter jsonReaderWriter, Result r) {
		return Response.ok(jsonReaderWriter.serialize(r));
	}
}
