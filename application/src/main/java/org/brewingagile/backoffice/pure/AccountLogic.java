package org.brewingagile.backoffice.pure;

import fj.*;
import fj.data.List;
import fj.data.Set;
import fj.data.TreeMap;
import org.brewingagile.backoffice.types.AccountPackage;
import org.brewingagile.backoffice.types.TicketName;

import java.math.BigDecimal;
import java.math.BigInteger;

public class AccountLogic {
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

		List<P4<TicketName, BigInteger, BigInteger, BigInteger>> totalTickets = map.map(x -> {
			BigInteger signups = x._3();
			BigInteger ticketNeedOverPackages = signups.subtract(x._2()).max(BigInteger.ZERO);
			BigInteger missingSignups = x._2().subtract(x._3()).max(BigInteger.ZERO);
			return P.p(x._1(), ticketNeedOverPackages, signups, missingSignups);
		});

		BigDecimal extraTicketsAmountExVat = totalTickets.map(x -> {
			BigDecimal bdNeed = new BigDecimal(x._2());
			BigDecimal price = unvat(prices.get(x._1()).some());
			return bdNeed.multiply(price);
		}).foldLeft(add.sum(), add.zero());

		return new Total(
			packagesAmountExVat.add(extraTicketsAmountExVat),
			totalTickets
		);
	}

	private static BigDecimal unvat(BigDecimal some) {
		return some.multiply(BigDecimal.valueOf(8, 1));
	}

	public static final class Total {
		public final BigDecimal totalAmountExVat;
		//need, signups, missingSignups
		public final List<P4<TicketName, BigInteger, BigInteger, BigInteger>> tickets;

		public Total(
			BigDecimal totalAmountExVat,
			List<P4<TicketName, BigInteger, BigInteger, BigInteger>> tickets
		) {
			this.totalAmountExVat = totalAmountExVat;
			this.tickets = tickets;
		}
	}
}
