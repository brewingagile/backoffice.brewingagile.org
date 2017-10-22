package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import fj.data.Collectors;
import fj.data.List;
import fj.data.Option;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.AccountsSql;
import org.brewingagile.backoffice.db.operations.BundlesSql;
import org.brewingagile.backoffice.rest.json.FromJson;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;

import static argo.jdom.JsonNodeFactories.*;
import static org.brewingagile.backoffice.db.operations.BundlesSql.Bucket;
import static org.brewingagile.backoffice.rest.json.ToJson.nullable;

@Path("accounts")
@NeverCache
public class AccountsJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final AccountsSql accountsSql;

	public AccountsJaxRs(
		DataSource dataSource,
		AuthService authService,
		AccountsSql accountsSql
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.accountsSql = accountsSql;
	}

	//	curl -u admin:password "http://localhost:9080/gui/accounts/"
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			List<Account> accounts = accountsSql.all(c).toList();
			return Response.ok(ArgoUtils.format(array(accounts.map(ToJson::account)))).build();
		}
	}

	private static JsonRootNode json(Bucket b) {
		return object(
			field("bucket" ,string(b.bucket)),
			field("conference", number(b.conference)),
			field("workshop1", number(b.workshop1)),
			field("workshop2", number(b.workshop2)),
			field("deal", nullable(b.deal, x ->
				object(field("price", number(x.price)))
			))
		);
	}


	private static List<Bucket> unJson(String json) throws InvalidSyntaxException {
		return ArgoUtils.parse(json)
			.getArrayNode()
			.stream()
			.map(AccountsJaxRs::unbucket)
			.collect(Collectors.toList());
	}

	private static Bucket unbucket(JsonNode node) {
		return new Bucket(
			node.getStringValue("bucket"),
			new BigInteger(node.getNumberValue("conference")).intValue(),
			new BigInteger(node.getNumberValue("workshop1")).intValue(),
			new BigInteger(node.getNumberValue("workshop2")).intValue(),
			Option.fromNull(FromJson.getNullableNode(node, "deal")).map(x -> new BundlesSql.Deal(
				new BigDecimal(x.getNumberValue("price"))
			))
		);
	}
}
