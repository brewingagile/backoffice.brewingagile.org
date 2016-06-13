package org.brewingagile.backoffice.db.operations;

import fj.data.List;
import fj.function.Try1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BucketsSqlMapper {
	public final static class Bucket {
		public final String bucket;
		public final int conference;
		public final int workshop1;
		public final int workshop2;

		public Bucket(
			String bucket,
			int conference,
			int workshop1,
			int workshop2
		) {
			this.bucket = bucket;
			this.conference = conference;
			this.workshop1 = workshop1;
			this.workshop2 = workshop2;
		}
	}

	public List<Bucket> all(Connection c) throws SQLException {
		String sql = "SELECT * FROM bucket ORDER BY bucket;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps, rs -> new Bucket(
				rs.getString("bucket"),
				rs.getInt("conference"),
				rs.getInt("workshop1"),
				rs.getInt("workshop2")
			));
		}
	}

	public void replace(Connection c, List<Bucket> bs) throws SQLException {
		deferAll(c); delete(c); for (Bucket b : bs) insert(c, b);
	}

	private void deferAll(Connection c) throws SQLException {
		c.createStatement().execute("SET CONSTRAINTS ALL DEFERRED;");
	}

	private void delete(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM bucket;")) {
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
		public final BucketsSqlMapper.Bucket bucket;
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
			"\tbucket,\n" +
			"\tcount(rt_conference.registration_id) as conference,\n" +
			"\tcount(rt_workshop1.registration_id) as workshop1,\n" +
			"\tcount(rt_workshop2.registration_id) as workshop2\n" +
			"FROM bucket \n" +
			"LEFT JOIN registration_bucket rb USING (bucket)\n" +
			"LEFT JOIN registration_ticket rt_conference ON (rt_conference.registration_id = rb.registration_id AND rt_conference.ticket = 'conference') \n" +
			"LEFT JOIN registration_ticket rt_workshop1 ON (rt_workshop1.registration_id = rb.registration_id AND rt_workshop1.ticket = 'workshop1') \n" +
			"LEFT JOIN registration_ticket rt_workshop2 ON (rt_workshop2.registration_id = rb.registration_id AND rt_workshop2.ticket = 'workshop2') \n" +
			"GROUP BY bucket ORDER BY bucket";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			Try1<ResultSet, Bucket, SQLException> f = rs -> new Bucket(
				rs.getString("bucket"),
				rs.getInt("conference"),
				rs.getInt("workshop1"),
				rs.getInt("workshop2")
			);
			return SqlOps.list(ps, rs -> new BucketSummary(
				f.f(rs),
				rs.getInt("conference"),
				rs.getInt("workshop1"),
				rs.getInt("workshop2")
			));
		}
	}
}