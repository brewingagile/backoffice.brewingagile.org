package org.brewingagile.backoffice.db.operations;

import fj.data.List;
import fj.data.Option;
import fj.function.Try1;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

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