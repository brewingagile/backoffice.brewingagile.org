package org.brewingagile.backoffice.db.operations;

import com.google.common.collect.ImmutableList;
import fj.function.Try1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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

	public ImmutableList<Bucket> all(Connection c) throws SQLException {
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
		String sql = "SELECT " +
			"	sum(1) as conference, " +
			"	sum(CASE WHEN ticket = 'conference+workshop' THEN 1 ELSE 0 END) as workshop1, " +
			"	sum(CASE WHEN ticket = 'conference+workshop2' THEN 1 ELSE 0 END) as workshop2 " +
			"FROM registrations r " +
			"LEFT JOIN registration_bucket rb ON (r.id = rb.registration_id) " +
			"WHERE rb.bucket IS NULL;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.one(ps, rs -> new Individuals(
				rs.getInt("conference"),
				rs.getInt("workshop1"),
				rs.getInt("workshop2")
			)).get();
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

	public ImmutableList<BucketSummary> bundles(Connection c) throws SQLException {
		String sql = "SELECT * FROM bucket b " +
			"LEFT JOIN (" +
			"	SELECT rb.bucket," +
			"		sum(1) as actual_conference, " +
			"		sum(CASE WHEN ticket = 'conference+workshop' THEN 1 ELSE 0 END) as actual_workshop1, " +
			"		sum(CASE WHEN ticket = 'conference+workshop2' THEN 1 ELSE 0 END) as actual_workshop2 " +
			"	FROM registrations r " +
			"	JOIN registration_bucket rb ON (r.id = rb.registration_id) " +
			"	GROUP BY bucket" +
			") sub USING (bucket)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			Try1<ResultSet, Bucket, SQLException> f = rs -> new Bucket(
				rs.getString("bucket"),
				rs.getInt("conference"),
				rs.getInt("workshop1"),
				rs.getInt("workshop2")
			);
			return SqlOps.list(ps, rs -> new BucketSummary(
				f.f(rs),
				rs.getInt("actual_conference"),
				rs.getInt("actual_workshop1"),
				rs.getInt("actual_workshop2")
			));
		}
	}
}