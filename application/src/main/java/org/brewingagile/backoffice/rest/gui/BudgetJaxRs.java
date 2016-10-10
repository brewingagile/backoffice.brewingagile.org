package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import fj.data.Collectors;
import fj.data.List;
import functional.Tuple2;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BucketsSqlMapper;
import org.brewingagile.backoffice.db.operations.BudgetSql;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.UUID;

import static argo.jdom.JsonNodeFactories.*;

@Path("budget")
@NeverCache
public class BudgetJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final BudgetSql budgetSql;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final BucketsSqlMapper bucketsSqlMapper;

	public BudgetJaxRs(
		DataSource dataSource,
		AuthService authService,
		BudgetSql budgetSql,
		RegistrationsSqlMapper registrationsSqlMapper,
		BucketsSqlMapper bucketsSqlMapper
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.budgetSql = budgetSql;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.bucketsSqlMapper = bucketsSqlMapper;
	}

//		curl -u admin:password "http://localhost:9080/ba-backoffice/gui/budget/fixed-costs"
	@GET
	@Path("fixed-costs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFixedCosts(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			return Response.ok(ArgoUtils.format(
				array(budgetSql.fixedCosts(c).map(BudgetJaxRs::json))
			)).build();
		}
	}

	private static JsonRootNode json(BudgetSql.FixedCost x) {
		return object(
			field("cost" ,string(x.cost)),
			field("amount", number(x.amount))
		);
	}

	//	curl -u admin:password -X PUT -H "Content-Type: application/json" "http://localhost:9080/ba-backoffice/gui/budget/fixed-costs" --data '...'
	@PUT
	@Path("fixed-costs")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(@Context HttpServletRequest request, String json) throws Exception {
		authService.guardAuthenticatedUser(request);

		List<BudgetSql.FixedCost> bs = unJson(json);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			budgetSql.replace(c, bs);
			c.commit();
		}
		return Response.ok().build();
	}

	private enum BudgetItemType {
		COST, REVENUE
	}

	public static final class BudgetItem {
		private final BudgetItemType type;
		private final String description;
		private final int qty;
		private final BigDecimal amount;
		private final BigDecimal total;

		public BudgetItem(BudgetItemType type, String description, int qty, BigDecimal amount, BigDecimal total) {
			this.type = type;
			this.description = description;
			this.qty = qty;
			this.amount = amount;
			this.total = total;
		}
	}

	//	curl -u admin:password "http://localhost:9080/gui/budget/report" | jq .

	@GET
	@Path("report")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReport(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);

		List<Registration> registrations;
		try (Connection c = dataSource.getConnection()) {
			List<Tuple2<UUID, RegistrationsSqlMapper.RegistrationTuple>> all = registrationsSqlMapper.all(c);
			List.Buffer<Registration> empty = List.Buffer.empty();
			for (Tuple2<UUID, RegistrationsSqlMapper.RegistrationTuple> x : all)
				empty.snoc(registrationsSqlMapper.one(c, x._1).some());

			registrations = empty.toList();
		}

		try (Connection c = dataSource.getConnection()) {
			List<BucketsSqlMapper.BucketSummary> bundles = bucketsSqlMapper.bundles(c);
			BucketsSqlMapper.Individuals individuals = bucketsSqlMapper.individuals(c);
			BundleLogic.Total logic = BundleLogic.logic(bundles, individuals);

			List<BudgetItem> cost = List.list(
				t(BudgetItemType.COST, "Torsdag förmiddagsfika", logic.total.workshop1, 54),
				t(BudgetItemType.COST, "Torsdag lunch", logic.total.workshop1, 112.50),
				t(BudgetItemType.COST, "Torsdag förmiddagsfika", logic.total.workshop1, 45),
				t(BudgetItemType.COST, "Fredag förmiddagsfika", logic.total.workshop2, 54), // + organisers
				t(BudgetItemType.COST, "Fredag lunch", logic.total.workshop2, 112.50), // + organisers, speakers
				t(BudgetItemType.COST, "Fredag eftermiddagsfika", logic.total.conference, 45),
				t(BudgetItemType.COST, "Lördag fika", logic.total.conference / 2, 45), //assumption, half attendees on saturday,
				t(BudgetItemType.COST, "Lokalhyra", 1, 20000),
				t(BudgetItemType.COST, "PA", 1, 6000) //don't remember exactly...
			);

			List<BudgetItem> revenue = List.list(
				t(BudgetItemType.REVENUE, "Workshop1", logic.individuals.workshop1, 2800),
				t(BudgetItemType.REVENUE, "Workshop2", logic.individuals.workshop2, 1400),
				t(BudgetItemType.REVENUE, "Konferensbiljetter", logic.individuals.conference, 960)
			);


			// + Sponsorer / Bundles!!!

			// Workshops don't actually cost. Speakers get it all!


			BigDecimal allRevenue = revenue.foldLeft((BigDecimal l, BudgetItem r) -> l.add(r.total), BigDecimal.ZERO);
			BigDecimal allCosts = cost.foldLeft((BigDecimal l, BudgetItem r) -> l.add(r.total), BigDecimal.ZERO);
			BigDecimal profit = allRevenue.subtract(allCosts);

			return Response.ok(ArgoUtils.format(json(revenue, cost, allRevenue, allCosts, profit))).build();
		}
	}

	private static JsonRootNode json(
		List<BudgetItem> revenue,
		List<BudgetItem> cost,
		BigDecimal allRevenue,
		BigDecimal allCosts,
		BigDecimal profit
	) {
		return object(
			field("costs", array(cost.map(BudgetJaxRs::json))),
			field("revenues", array(revenue.map(BudgetJaxRs::json))),
			field("bottomLine", object(
				field("revenue", number(allRevenue)),
				field("costs", number(allCosts)),
				field("profit", number(profit))
			))
		);
	}

	private static JsonRootNode json(BudgetItem bi) {
		return object(
			field("description", string(bi.description)),
			field("qty", number(bi.qty)),
			field("amount", number(bi.amount)),
			field("total", number(bi.total))
		);
	}

	private static BudgetItem t(BudgetItemType cost, String description, int qty, double price) {
		return new BudgetItem(cost, description, qty, new BigDecimal(price), new BigDecimal(price).multiply(new BigDecimal(qty)));
	}

	private static List<BudgetSql.FixedCost> unJson(String json) throws InvalidSyntaxException {
		return ArgoUtils.parse(json)
			.getArrayNode()
			.stream()
			.map(BudgetJaxRs::fixedCostUnJson)
			.collect(Collectors.toList());
	}

	private static BudgetSql.FixedCost fixedCostUnJson(JsonNode node) {
		return new BudgetSql.FixedCost(
			node.getStringValue("cost"),
			new BigDecimal(node.getNumberValue("amount"))
		);
	}
}