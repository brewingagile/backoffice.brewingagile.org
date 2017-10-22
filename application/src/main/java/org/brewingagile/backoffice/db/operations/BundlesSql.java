package org.brewingagile.backoffice.db.operations;

import fj.P;
import fj.P2;
import fj.data.List;
import fj.data.Option;
import fj.data.TreeMap;
import fj.function.Try1;
import org.brewingagile.backoffice.instances.PreparedStatements;
import org.brewingagile.backoffice.instances.ResultSets;
import org.brewingagile.backoffice.pure.AccountLogic;
import org.brewingagile.backoffice.types.TicketName;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;
import static org.brewingagile.backoffice.types.TicketName.ticketName;

public class BundlesSql {
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
		TreeMap<TicketName, BigInteger> map = individuals2(c).groupBy(x -> x._1(), x -> x._2()).map(x -> x.head());

		return new Individuals(
			(int)map.get(ticketName("conference")).orSome(BigInteger.ZERO).longValue(),
			(int)map.get(ticketName("workshop1")).orSome(BigInteger.ZERO).longValue(),
			(int)map.get(ticketName("workshop2")).orSome(BigInteger.ZERO).longValue()
		);
	}

	public List<P2<TicketName, BigInteger>> individuals2(Connection c) throws SQLException {
		String sql = "SELECT ticket, count(1) as c FROM registration " +
			"JOIN registration_ticket USING (registration_id) " +
			"LEFT JOIN registration_account USING (registration_id) " +
			"WHERE registration_account IS NULL " +
			"GROUP BY ticket;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps, rs -> P.p(
				ResultSets.ticketName(rs, "ticket"),
				rs.getBigDecimal("c").toBigInteger()
			));
		}
	}

	public static final class BucketSummary {
		public final BundlesSql.Bucket bucket;
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
			Try1<ResultSet, Bucket, SQLException> f = BundlesSql::rsBucket;
			return SqlOps.list(ps, rs -> new BucketSummary(
				f.f(rs),
				rs.getInt("actual_conference"),
				rs.getInt("actual_workshop1"),
				rs.getInt("actual_workshop2")
			));
		}
	}
}