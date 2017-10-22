package org.brewingagile.backoffice.db.operations;

import fj.P;
import fj.P5;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import fj.function.Try1;
import org.brewingagile.backoffice.instances.PreparedStatements;
import org.brewingagile.backoffice.instances.ResultSets;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.AccountPackage;
import org.brewingagile.backoffice.types.TicketName;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

public class AccountsSql {
	public static final class AccountData {
		public final String billingRecipient;
		public final String billingAddress;

		public AccountData(String billingRecipient, String billingAddress) {
			this.billingRecipient = billingRecipient;
			this.billingAddress = billingAddress;
		}
	}


	public final static class Deal {
		public final BigDecimal price;

		public Deal(BigDecimal price) {
			this.price = price;
		}
	}

	public final static class Bucket {
		public final String bucket;
		public final int conference;
		public final int workshop1;
		public final int workshop2;
		public final Option<Deal> deal;

		public Bucket(
			String bucket,
			int conference,
			int workshop1,
			int workshop2,
			Option<Deal> deal
		) {
			this.bucket = bucket;
			this.conference = conference;
			this.workshop1 = workshop1;
			this.workshop2 = workshop2;
			this.deal = requireNonNull(deal);
		}
	}

	public Set<Account> all(Connection c) throws SQLException {
		String sql = "SELECT account FROM account;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.set(ps, Account.Ord, rs -> ResultSets.account(rs, "account"));
		}
	}

	public AccountData accountData(Connection c, Account account) throws SQLException {
		String sql = "SELECT * FROM account WHERE account = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, account);
			return SqlOps.one(ps, rs -> new AccountData(
				rs.getString("billing_recipient"),
				rs.getString("billing_address")
			)).some();
		}
	}

	public List<AccountPackage> packages(Connection c, Account account) throws SQLException {
		String sql = "SELECT * FROM account_package JOIN account_package_ticket USING (account, package_number) WHERE account = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, account);

			List<P5<Integer, String, BigDecimal, TicketName, BigInteger>> list = SqlOps.list(ps, rs -> P.p(
				rs.getInt("package_number"),
				rs.getString("description"),
				rs.getBigDecimal("price"),
				ResultSets.ticketName(rs, "ticket"),
				BigInteger.valueOf(rs.getLong("qty"))
			));

			return
				list.groupBy(x1 -> P.p(x1._1(), x1._2(), x1._3()), x1 -> P.p(x1._4(), x1._5()))
					.toList()
					.map(x -> new AccountPackage(
						x._1()._2(),
						x._1()._3(),
						x._2()
					));
		}
	}

	private static Bucket rsBucket(ResultSet rs) throws SQLException {
		return new Bucket(
			rs.getString("bucket"),
			rs.getInt("conference"),
			rs.getInt("workshop1"),
			rs.getInt("workshop2"),
			Option.fromNull(rs.getBigDecimal("price")).map(Deal::new)
		);
	}

	public void replace(Connection c, List<Bucket> bs) throws SQLException {
		SqlOps.deferAll(c); delete(c);
		for (Bucket b : bs) {
			insert(c, b);
			if (b.deal.isSome()) insert(c, b, b.deal.some());
		}
	}

	private void delete(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM bundle_deal; DELETE FROM bucket;")) {
			statement.executeUpdate();
		}
	}

	private void insert(Connection connection, Bucket bucket) throws SQLException {
		String sql = "INSERT INTO bucket (bucket, conference, workshop1, workshop2) VALUES (?,?,?,?);";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, bucket.bucket);
			statement.setInt(2, bucket.conference);
			statement.setInt(3, bucket.workshop1);
			statement.setInt(4, bucket.workshop2);
			statement.executeUpdate();
		}
	}

	private void insert(Connection connection, Bucket bucket, Deal deal) throws SQLException {
		String sql = "INSERT INTO bundle_deal (bundle, price) VALUES (?,?);";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, bucket.bucket);
			statement.setBigDecimal(2, deal.price);
			statement.executeUpdate();
		}
	}

	public static final class Individuals {
		public final int conference;
		public final int workshop1;
		public final int workshop2;

		public Individuals(int conference, int workshop1, int workshop2) {
			this.conference = conference;
			this.workshop1 = workshop1;
			this.workshop2 = workshop2;
		}
	}

	public Individuals individuals(Connection c) throws SQLException {
		String sql = "SELECT (\n" +
			"\tSELECT count(1) FROM registration_ticket rt\n" +
			"\tLEFT JOIN registration_bucket rb USING (registration_id) \n" +
			"\tWHERE ticket = 'conference' AND rb.bucket IS NULL\n" +
			") as conference,\n" +
			"(\n" +
			"\tSELECT count(1) FROM registration_ticket rt\n" +
			"\tLEFT JOIN registration_bucket rb USING (registration_id) \n" +
			"\tWHERE ticket = 'workshop1' AND rb.bucket IS NULL\n" +
			") as workshop1,\n" +
			"(\n" +
			"\tSELECT count(1) FROM registration_ticket rt\n" +
			"\tLEFT JOIN registration_bucket rb USING (registration_id) \n" +
			"\tWHERE ticket = 'workshop2' AND rb.bucket IS NULL\n" +
			") as workshop2 ";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.one(ps, rs -> new Individuals(
				rs.getInt("conference"),
				rs.getInt("workshop1"),
				rs.getInt("workshop2")
			)).some();
		}
	}

	public static final class BucketSummary {
		public final AccountsSql.Bucket bucket;
		public final int actualConference;
		public final int actualWorkshop1;
		public final int actualWorkshop2;

		public BucketSummary(Bucket bucket, int actualConference, int actualWorkshop1, int actualWorkshop2) {
			this.bucket = bucket;
			this.actualConference = actualConference;
			this.actualWorkshop1 = actualWorkshop1;
			this.actualWorkshop2 = actualWorkshop2;
		}
	}

	public List<BucketSummary> bundles(Connection c) throws SQLException {
		String sql = "SELECT\n" +
			"\tbucket, conference, workshop1, workshop2,\n" +
			"\tcount(rt_conference.registration_id) as actual_conference,\n" +
			"\tcount(rt_workshop1.registration_id) as actual_workshop1,\n" +
			"\tcount(rt_workshop2.registration_id) as actual_workshop2, \n" +
			"price " +
			"FROM bucket b \n" +
			"LEFT JOIN registration_bucket rb USING (bucket)\n" +
			"LEFT JOIN registration_ticket rt_conference ON (rt_conference.registration_id = rb.registration_id AND rt_conference.ticket = 'conference') \n" +
			"LEFT JOIN registration_ticket rt_workshop1 ON (rt_workshop1.registration_id = rb.registration_id AND rt_workshop1.ticket = 'workshop1') \n" +
			"LEFT JOIN registration_ticket rt_workshop2 ON (rt_workshop2.registration_id = rb.registration_id AND rt_workshop2.ticket = 'workshop2') \n" +
			"LEFT JOIN bundle_deal bd ON (b.bucket = bd.bundle) " +
			"GROUP BY bucket, price ORDER BY bucket";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			Try1<ResultSet, Bucket, SQLException> f = AccountsSql::rsBucket;
			return SqlOps.list(ps, rs -> new BucketSummary(
				f.f(rs),
				rs.getInt("actual_conference"),
				rs.getInt("actual_workshop1"),
				rs.getInt("actual_workshop2")
			));
		}
	}
}