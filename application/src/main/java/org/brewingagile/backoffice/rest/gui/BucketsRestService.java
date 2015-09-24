package org.brewingagile.backoffice.rest.gui;

import com.fasterxml.jackson.core.type.TypeReference;
import org.brewingagile.backoffice.application.Application;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BucketsSqlMapper;
import org.brewingagile.backoffice.utils.JsonReaderWriter;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.util.List;

import static org.brewingagile.backoffice.db.operations.BucketsSqlMapper.Bucket;

@Path("buckets")
@NeverCache
public class BucketsRestService {
	private final JsonReaderWriter jsonReaderWriter;
	private final DataSource dataSource;
	private final AuthService authService;
	private final BucketsSqlMapper bucketsSqlMapper;

	public BucketsRestService(
		JsonReaderWriter jsonReaderWriter,
		DataSource dataSource,
		AuthService authService,
		BucketsSqlMapper bucketsSqlMapper
	) {
		this.jsonReaderWriter = jsonReaderWriter;
		this.dataSource = dataSource;
		this.authService = authService;
		this.bucketsSqlMapper = bucketsSqlMapper;
	}

	public BucketsRestService() {
		this.jsonReaderWriter = new JsonReaderWriter();
		this.dataSource = Application.INSTANCE.dataSource();
		this.authService = Application.INSTANCE.authService();
		this.bucketsSqlMapper = Application.INSTANCE.bucketsSqlMapper;
	}

	//	curl -u admin:password "http://localhost:9080/ba-backoffice/gui/buckets/"
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			return Response.ok(jsonReaderWriter.serialize(bucketsSqlMapper.all(c))).build();
		}
	}

	private static final TypeReference<List<Bucket>> mappingEntryTypeReference = new TypeReference<List<Bucket>>() {};

	//	curl -u admin:password -X PUT -H "Content-Type: application/json" "http://localhost:9080/ba-backoffice/gui/buckets/" --data '...'
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(@Context HttpServletRequest request, String json) throws Exception {
		authService.guardAuthenticatedUser(request);
		List<Bucket> bs = jsonReaderWriter.deserialize(json, mappingEntryTypeReference);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			bucketsSqlMapper.replace(c, bs);
			c.commit();
		}
		return Response.ok().build();
	}
}
