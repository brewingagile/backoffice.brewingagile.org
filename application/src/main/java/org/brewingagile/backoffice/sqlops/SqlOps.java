package org.brewingagile.backoffice.sqlops;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class SqlOps {
	public static <T> List<T> list(PreparedStatement ps, Function<ResultSet, T> f) throws SQLException {
		try (ResultSet r = ps.executeQuery()) {
			List<T> invoices = Lists.newArrayList(); 
			while (r.next()) {
				invoices.add(f.apply(r));
			}
			return invoices;
		}
	}

	public static <T> Optional<T> one(PreparedStatement ps, Function<ResultSet, T> f) throws SQLException {
		try (ResultSet r = ps.executeQuery()) {
			if (!r.next()) return Optional.absent();
			return Optional.of(f.apply(r));
		}
	}

	public static <T> List<T> map(Connection c, Query q, ResultSetMapper<T> f) throws SQLException {
		try (PreparedStatement ps = q.apply(c)) {
			try (ResultSet r = ps.executeQuery()) {
				List<T> l = Lists.newArrayList(); 
				while (r.next()) { l.add(f.apply(r)); }
				return l;
			}
		}
	}
}
