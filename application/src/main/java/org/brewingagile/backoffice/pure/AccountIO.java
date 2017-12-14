package org.brewingagile.backoffice.pure;

import fj.*;
import fj.data.List;
import fj.data.Set;
import fj.data.TreeMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.brewingagile.backoffice.db.operations.AccountsSql;
import org.brewingagile.backoffice.db.operations.AccountsSql.AccountData;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.AccountPackage;
import org.brewingagile.backoffice.types.ParticipantName;
import org.brewingagile.backoffice.types.TicketName;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;

public class AccountIO {
	private final AccountsSql accountsSql;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final TicketsSql ticketsSql;

	public AccountIO(
		AccountsSql accountsSql,
		RegistrationsSqlMapper registrationsSqlMapper,
		TicketsSql ticketsSql
	) {
		this.accountsSql = accountsSql;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.ticketsSql = ticketsSql;
	}

	public AccountLogic.AccountStatement accountStatement(Connection c, Account account) throws SQLException {
		List<AccountPackage> packages = accountsSql.accountPackages(c, account);
		List<P2<TicketName, BigInteger>> signups = registrationsSqlMapper.inAccount(c, account)
			.groupBy(x -> x._2(), x -> BigInteger.ONE, Monoid.bigintAdditionMonoid, TicketName.Ord)
			.toList();
		TreeMap<TicketName, BigDecimal> tickets = ticketsSql.all(c).groupBy(x -> x.ticket, x -> x.price).map(x -> x.head());
		return AccountLogic.accountStatement(
			packages,
			signups,
			tickets
		);
	}

	public AccountLogic.AccountStatement2 accountStatement2(Connection c, Account account) throws SQLException {
		List<AccountPackage> packages = accountsSql.accountPackages(c, account);
		List<P2<ParticipantName, Set<TicketName>>> signups = registrationsSqlMapper.inAccount2(c, account);
		TreeMap<TicketName, BigDecimal> tickets = ticketsSql.all(c).groupBy(x -> x.ticket, x -> x.price).map(x -> x.head());
		return AccountLogic.accountStatement2(
			packages,
			signups,
			tickets
		);
	}

	public AccountLogic.Total total(Connection c, Account account) throws SQLException {
		List<AccountPackage> packages = accountsSql.accountPackages(c, account);
		List<P2<TicketName, BigInteger>> signups = registrationsSqlMapper.inAccount(c, account)
			.groupBy(x -> x._2(), x -> BigInteger.ONE, Monoid.bigintAdditionMonoid, TicketName.Ord)
			.toList();
		TreeMap<TicketName, BigDecimal> tickets = ticketsSql.all(c).groupBy(x -> x.ticket, x -> x.price).map(x -> x.head());
		return AccountLogic.logic(
			packages,
			signups,
			tickets
		);
	}

	public List<P3<Account, AccountData, AccountLogic.Total>> allAccountTotals(Connection c) throws SQLException {
		List<Account> accounts = accountsSql.all(c).toList();
		List.Buffer<P3<Account, AccountData, AccountLogic.Total>> buffer = List.Buffer.empty();
		for (Account account : accounts) {
			AccountData accountData = accountsSql.accountData(c, account);
			AccountLogic.Total total = total(c, account);
			buffer.snoc(P.p(account, accountData, total));
		}
		return buffer.toList();
	}

	public List<P2<TicketName, TicketSales>> ticketSales(Connection c) throws SQLException {
		List<P3<Account, AccountData, AccountLogic.Total>> p3s = allAccountTotals(c);
		List<P2<TicketName, AccountLogic.TicketTotal>> join = List.join(p3s.map(x -> x._3().tickets));
		TreeMap<TicketName, BigInteger> accounts = join.groupBy(x -> x._1(), x -> x._2().totalReserved, Monoid.bigintAdditionMonoid, TicketName.Ord);

		TreeMap<TicketName, BigInteger> individuals = registrationsSqlMapper.individuals2(c).groupBy(x -> x._1(), x -> x._2(), Monoid.bigintAdditionMonoid, TicketName.Ord);
		Set<TicketName> keys = Set.iterableSet(TicketName.Ord, List.join(List.list(accounts.keys(), individuals.keys())));
		return keys.toList().map(x -> {
			BigInteger a = accounts.get(x).orSome(BigInteger.ZERO);
			BigInteger i = individuals.get(x).orSome(BigInteger.ZERO);
			return P.p(x, new TicketSales(
					i,
					a,
					a.add(i)
				)
			);
		});
	}

	@EqualsAndHashCode
	@ToString
	public static final class TicketSales {
		public final BigInteger individuals;
		public final BigInteger accounts;
		public final BigInteger total;

		public TicketSales(BigInteger individuals, BigInteger accounts, BigInteger total) {
			this.individuals = individuals;
			this.accounts = accounts;
			this.total = total;
		}
	}
}
