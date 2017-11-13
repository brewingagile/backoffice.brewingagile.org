package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import fj.Monoid;
import fj.P2;
import fj.P3;
import fj.data.Collectors;
import fj.data.List;
import fj.data.TreeMap;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.AccountsSql;
import org.brewingagile.backoffice.db.operations.BudgetSql;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.pure.AccountIO;
import org.brewingagile.backoffice.pure.AccountLogic;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.TicketName;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;

import static argo.jdom.JsonNodeFactories.*;
import static org.brewingagile.backoffice.types.TicketName.ticketName;

@Path("budget")
@NeverCache
public class BudgetJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final BudgetSql budgetSql;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final AccountIO accountIO;
	private final TicketsSql ticketsSql;

	public BudgetJaxRs(
		DataSource dataSource,
		AuthService authService,
		BudgetSql budgetSql,
		RegistrationsSqlMapper registrationsSqlMapper,
		AccountIO accountIO,
		TicketsSql ticketsSql
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.budgetSql = budgetSql;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.accountIO = accountIO;
		this.ticketsSql = ticketsSql;
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
	private static BigDecimal unvat(BigDecimal some) {
		return some.multiply(BigDecimal.valueOf(8, 1));
	}

	@GET
	@Path("report")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReport(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);

		try (Connection c = dataSource.getConnection()) {
			TreeMap<TicketName, BigDecimal> ticketPrices = ticketsSql.all(c)
				.groupBy(x -> x.ticket, x -> x.price).map(x -> unvat(x.head()));

			TreeMap<TicketName, BigInteger> ticketSalesMap = accountIO.ticketSales(c)
				.groupBy(x -> x._1(), x -> x._2().total, Monoid.bigintAdditionMonoid, TicketName.Ord);

			List<P3<Account, AccountsSql.AccountData, AccountLogic.Total>> allAccountTotals = accountIO.allAccountTotals(c);
			List<P2<TicketName, BigInteger>> individualSales = registrationsSqlMapper.individuals2(c);

			int conference = ticketSalesMap.get(ticketName("conference")).some().intValue();
			int workshop1 = ticketSalesMap.get(ticketName("workshop1")).some().intValue();
			int workshop2 = ticketSalesMap.get(ticketName("workshop2")).some().intValue();

			List<BudgetItem> costsWorkshop1 = List.list(
				t("Venue: Workshop1", Math.max(workshop1 + 2, 27), 595) // speaker + 1 of us
			);
			List<BudgetItem> costsWorkshop2 = List.list(
				t("Venue: Workshop2", Math.max(workshop2 + 1 + 5, 27), 495) // speaker + 5 of us.
			);
			List<BudgetItem> costsConference = List.list(
				t("Venue: Friday Conference", Math.max(conference, 135), 250),
				t("Beer", conference, 68),
				t("Venue: Saturday", Math.max(conference / 2, 63), 395),
				t("Speaker Reimbursements", 2, 10000),
				t("Speaker: Workshop 1", 1, BigDecimal.valueOf(workshop1).multiply(ticketPrices.get(ticketName("workshop1")).some()).subtract(sum(costsWorkshop1))),
				t("Speaker: Workshop 2", 1, BigDecimal.valueOf(workshop2).multiply(ticketPrices.get(ticketName("workshop2")).some()).subtract(sum(costsWorkshop2))),
				t("Video Production", 1, 20000),
				t("Audio Technician", 1, 3600),
				t("Supplies for Open Spaces", 1, 1000),
				t("Lanyards", 1, 529 + 325)
			);

			List<BudgetItem> individualRevenues = individualSales.map(x -> t(x._1().ticketName + ": Separata biljetter", x._2().intValue(), ticketPrices.get(x._1()).some().intValue()));
			List<BudgetItem> accountRevenues = allAccountTotals.map(x -> t("Account: " + x._1().value, 1, x._3().totalAmountExVat));


			List<BudgetItem> revenues = List.Buffer.<BudgetItem>empty()
				.append(individualRevenues)
				.append(accountRevenues)
				.toList();

			List<BudgetItem> costs = List.Buffer.<BudgetItem>empty()
				.append(costsWorkshop1)
				.append(costsWorkshop2)
				.append(costsConference)
				.toList();

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