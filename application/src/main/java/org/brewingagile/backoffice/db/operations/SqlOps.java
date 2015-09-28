package org.brewingagile.backoffice.db.operations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import fj.function.Try1;

public class SqlOps {
	public static <T> Optional<T> one(PreparedStatement ps, Try1<ResultSet, T, SQLException> f) throws SQLException {
		try (ResultSet r = ps.executeQuery()) {
			if (!r.next()) return Optional.absent();
			return Optional.of(f.f(r));
		}
	}

	public static <T> ImmutableList<T> list(PreparedStatement ps, Try1<ResultSet, T, SQLException> f) throws SQLException {
		try (ResultSet r = ps.executeQuery()) {
			ImmutableList.Builder<T> invoices = ImmutableList.builder();
			while (r.next()) {
				invoices.add(f.f(r));
			}
			return invoices.build();
		}
	}
}
