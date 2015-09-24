package org.brewingagile.backoffice.db.operations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class Common {
	public static interface FunctionE<A,B,E extends Exception> {
		public B apply(A a) throws E;
	}

	public static <T> ImmutableList<T> list(PreparedStatement ps, FunctionE<ResultSet, T, SQLException> f) throws SQLException {
		try (ResultSet r = ps.executeQuery()) {
			ImmutableList.Builder<T> invoices = ImmutableList.builder();
			while (r.next()) {
				invoices.add(f.apply(r));
			}
			return invoices.build();
		}
	}

	public static <T> Optional<T> one(PreparedStatement ps, FunctionE<ResultSet, T, SQLException> f) throws SQLException {
		try (ResultSet r = ps.executeQuery()) {
			if (!r.next()) return Optional.absent();
			return Optional.of(f.apply(r));
		}
	}
}
