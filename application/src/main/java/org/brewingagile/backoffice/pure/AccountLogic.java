package org.brewingagile.backoffice.pure;

import fj.*;
import fj.data.List;
import fj.data.Set;
import fj.data.TreeMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.brewingagile.backoffice.types.AccountPackage;
import org.brewingagile.backoffice.types.TicketName;

import java.math.BigDecimal;
import java.math.BigInteger;

public class AccountLogic {
	@EqualsAndHashCode
	@ToString
	public static final class Line {
		public final String description;
		public final BigInteger qty;
		public final BigDecimal price;

		public Line(String description, BigInteger qty, BigDecimal price) {
			this.description = description;
			this.qty = qty;
			this.price = price;
		}
	}

	@EqualsAndHashCode
	@ToString
	public static final class AccountStatement {
		public final List<Line> lines;

		public AccountStatement(List<Line> lines) {
			this.lines = lines;
		}
	}

	public static AccountStatement accountStatement(List<AccountPackage> packages, List<P2<TicketName, BigInteger>> signupsIn, TreeMap<TicketName, BigDecimal> prices) {
		TreeMap<TicketName, BigInteger> creditedTickets = List.join(packages.map(x -> x.tickets))
			.groupBy(x -> x._1(), x -> x._2(), Monoid.bigintAdditionMonoid, TicketName.Ord);

		TreeMap<TicketName, BigInteger> debitedTickets = signupsIn.groupBy(x -> x._1(), x -> x._2(), Monoid.bigintAdditionMonoid, TicketName.Ord);

		Set<TicketName> union = Set.<TicketName>union()
			.f(Set.iterableSet(TicketName.Ord, creditedTickets.keys()))
			.f(Set.iterableSet(TicketName.Ord, debitedTickets.keys()));

		List<P3<TicketName, BigInteger, BigInteger>> ticketLines = union.toList().map(x -> P.p(
			x,
			creditedTickets.get(x).orSome(BigInteger.ZERO),
			debitedTickets.get(x).orSome(BigInteger.ZERO)
		));

		List<Line> totalTickets = ticketLines.map(x -> new Line(
			x._1().ticketName,
			x._3().subtract(x._2()).max(BigInteger.ZERO),
			unvat(prices.get(x._1()).some())
		));
		List<Line> packageLines = packages.map(x -> new Line(x.description, BigInteger.ONE, x.price));

		List<Line> lines = List.join(List.list(
			totalTickets,
			packageLines
		)).filter(x -> !x.qty.equals(BigInteger.ZERO));

		return new AccountStatement( lines );
	}

	public static Total logic(List<AccountPackage> packages, List<P2<TicketName, BigInteger>> signupsIn, TreeMap<TicketName, BigDecimal> prices) {
		Monoid<BigDecimal> add = Monoid.bigdecimalAdditionMonoid;
		BigDecimal packagesAmountExVat = packages.map(x -> x.price).foldLeft(add.sum(), add.zero());

		TreeMap<TicketName, BigInteger> creditedTickets = List.join(packages.map(x -> x.tickets))
			.groupBy(x -> x._1(), x -> x._2(), Monoid.bigintAdditionMonoid, TicketName.Ord);

		TreeMap<TicketName, BigInteger> debitedTickets = signupsIn.groupBy(x -> x._1(), x -> x._2(), Monoid.bigintAdditionMonoid, TicketName.Ord);

		Set<TicketName> union = Set.<TicketName>union()
			.f(Set.iterableSet(TicketName.Ord, creditedTickets.keys()))
			.f(Set.iterableSet(TicketName.Ord, debitedTickets.keys()));

		List<P3<TicketName, BigInteger, BigInteger>> map = union.toList().map(x -> P.p(
			x,
			creditedTickets.get(x).orSome(BigInteger.ZERO),
			debitedTickets.get(x).orSome(BigInteger.ZERO)
		));

		List<P2<TicketName, TicketTotal>> totalTickets = map.map(x -> {
			BigInteger signups = x._3();
			BigInteger ticketNeedOverPackages = signups.subtract(x._2()).max(BigInteger.ZERO);
			BigInteger missingSignups = x._2().subtract(x._3()).max(BigInteger.ZERO);
			BigInteger totalReserved = signups.max(x._2());
			return P.p(x._1(), new TicketTotal(ticketNeedOverPackages, signups, missingSignups, totalReserved));
		});

		BigDecimal extraTicketsAmountExVat = totalTickets.map(x -> {
			BigDecimal bdNeed = new BigDecimal(x._2().signupsNotPartOfPackage);
			BigDecimal price = unvat(prices.get(x._1()).some());
			return bdNeed.multiply(price);
		}).foldLeft(add.sum(), add.zero());

		return new Total(
			packagesAmountExVat.add(extraTicketsAmountExVat),
			totalTickets,
			packagesAmountExVat,
			extraTicketsAmountExVat
		);
	}

	private static BigDecimal unvat(BigDecimal some) {
		return some.multiply(BigDecimal.valueOf(8, 1));
	}

	public static final class TicketTotal {
		public final BigInteger signupsNotPartOfPackage;
		public final BigInteger signups;
		public final BigInteger missingSignups;
		public final BigInteger totalReserved;

		public TicketTotal(BigInteger signupsNotPartOfPackage, BigInteger signups, BigInteger missingSignups, BigInteger totalReserved) {
			this.signupsNotPartOfPackage = signupsNotPartOfPackage;
			this.signups = signups;
			this.missingSignups = missingSignups;
			this.totalReserved = totalReserved;
		}
	}

	public static final class Total {
		public final BigDecimal totalAmountExVat;
		public final List<P2<TicketName, TicketTotal>> tickets;
		public final BigDecimal packagesAmountExVat;
		public final BigDecimal extraTicketsAmountExVat;

		public Total(
			BigDecimal totalAmountExVat,
			List<P2<TicketName, TicketTotal>> tickets,
			BigDecimal packagesAmountExVat,
			BigDecimal extraTicketsAmountExVat
		) {
			this.totalAmountExVat = totalAmountExVat;
			this.tickets = tickets;
			this.packagesAmountExVat = packagesAmountExVat;
			this.extraTicketsAmountExVat = extraTicketsAmountExVat;
		}
	}
}
