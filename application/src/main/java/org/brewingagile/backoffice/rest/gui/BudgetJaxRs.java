package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import fj.data.Collectors;
import fj.data.List;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BundlesSql;
import org.brewingagile.backoffice.db.operations.BudgetSql;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.pure.BundleLogic;
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

import static argo.jdom.JsonNodeFactories.*;

@Path("budget")
@NeverCache
public class BudgetJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final BudgetSql budgetSql;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final BundlesSql bundlesSql;

	public BudgetJaxRs(
		DataSource dataSource,
		AuthService authService,
		BudgetSql budgetSql,
		RegistrationsSqlMapper registrationsSqlMapper,
		BundlesSql bundlesSql
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.budgetSql = budgetSql;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.bundlesSql = bundlesSql;
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

	public static final class BudgetItem {
		private final String description;
		private final int qty;
		private final BigDecimal amount;
		private final BigDecimal total;

		public BudgetItem(String description, int qty, BigDecimal amount, BigDecimal total) {
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

		try (Connection c = dataSource.getConnection()) {
			List<BundlesSql.BucketSummary> bundles = bundlesSql.bundles(c);
			BundlesSql.Individuals individuals = bundlesSql.individuals(c);
			BundleLogic.Total logic = BundleLogic.logic(bundles, individuals);

			List<BudgetItem> costsWorkshop1 = List.list(
				t("Workshop1: Mat F+L+F", logic.total.workshop1 + 2, 215), // speaker + 1 of us
				t("Workshop1: Lokal", 1, 4000)
			);
			List<BudgetItem> revenuesWorkshop1 = List.list(
				t("Workshop1: biljetter", logic.individuals.workshop1, 2800),
				t("Workshop1: bundles", bundles.filter(x -> x.bucket.deal.isNone()).map(x -> x.bucket.workshop1).foldLeft1((l,r) -> l + r), 2800)
			);

			List<BudgetItem> costsWorkshop2 = List.list(
				t("Workshop2: Mat F+L", logic.total.workshop2 + 1, 175), // speaker
				t("Workshop1: Lokal", 1, 2000)
			);
			List<BudgetItem> revenuesWorkshop2 = List.list(
				t("Workshop2: biljetter", logic.individuals.workshop2, 1400),
				t("Workshop2: bundles", bundles.filter(x -> x.bucket.deal.isNone()).map(x -> x.bucket.workshop2).foldLeft1((l,r) -> l + r), 1400)
			);

			List<BudgetItem> costs = List.Buffer.<BudgetItem>empty()
				.append(
					List.list(
						t("Torsdag: Frukost (organisatörer)", 11, 45),
						t("Torsdag: Middag (organisatörer)", 11, 148.50),

						t("Konferens: Lunch", 5 + 5 + 1, 175), // organisers, support, video
						t("Konferens: Fika", logic.total.conference, 45),
						t("Konferens: Middag", logic.total.conference, 148.50),
						t("Konferens: öl", logic.total.conference, 55),
						t("Konferens: Lokalhyra", 1, 14000),
						t("Konferens: PA", 1, 5350),
						t("Konferens: Lanyards", 1, 529 + 325),

						t("Konferens: Speaker: Vasco", 1, sum(revenuesWorkshop1).subtract(sum(costsWorkshop1))),
						t("Konferens: Speaker: Luis", 1, sum(revenuesWorkshop2).subtract(sum(costsWorkshop2))),
						t("Konferens: Speaker", 3, 10000),

						t("Open Spaces: Supplies", 1, 1000),
						t("Open Spaces: Brunch (50%)", logic.total.conference / 2, 117),
						t("Open Spaces: Fika (50%)", logic.total.conference / 2, 45) //assumption, half attendees on saturday,
					)
				)
				.append(costsWorkshop1)
				.append(costsWorkshop2)
				.toList();


			List<BudgetItem> revenues = List.Buffer.<BudgetItem>empty()
				.append(
					bundles.filter(x -> x.bucket.deal.isSome()).map(x -> t("Bundle: " + x.bucket.bucket, 1, x.bucket.deal.some().price))
				)
				.append(
					List.list(
						t("Konferens: biljetter", logic.individuals.conference, 960),
						t("Konferens: bundles", bundles.filter(x -> x.bucket.deal.isNone()).map(x -> x.bucket.conference).foldLeft1((l,r) -> l + r), 960)
					)
				)
				.append(revenuesWorkshop1)
				.append(revenuesWorkshop2)
				.toList();

			// + Sponsorer + Separate Invoice - Bundles (workshopar, konferensbiljetter)
			// Ingen middag, öl eller brunch ännu.

			BigDecimal allRevenue = sum(revenues);
			BigDecimal allCosts = sum(costs);
			BigDecimal profit = allRevenue.subtract(allCosts);

			return Response.ok(ArgoUtils.format(json(revenues, costs, allRevenue, allCosts, profit))).build();
		}
	}

	private static BigDecimal sum(List<BudgetItem> revenue) {
		return revenue.foldLeft((BigDecimal l, BudgetItem r) -> l.add(r.total), BigDecimal.ZERO);
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

	private static BudgetItem t(String description, int qty, double price) {
		return t(description, qty, new BigDecimal(price));
	}

	private static BudgetItem t(String description, int qty, BigDecimal price) {
		return new BudgetItem(description, qty, price, price.multiply(new BigDecimal(qty)));
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