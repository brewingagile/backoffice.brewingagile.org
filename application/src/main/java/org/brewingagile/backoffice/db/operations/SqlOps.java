package org.brewingagile.backoffice.db.operations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import fj.data.List;
import fj.data.Option;
import fj.function.Try1;

public class SqlOps {
	public static <T> Option<T> one(PreparedStatement ps, Try1<ResultSet, T, SQLException> f) throws SQLException {
		try (ResultSet r = ps.executeQuery()) {
			if (!r.next()) return Option.none();
			return Option.some(f.f(r));
		}
	}

	public static <T> List<T> list(PreparedStatement ps, Try1<ResultSet, T, SQLException> f) throws SQLException {
		try (ResultSet r = ps.executeQuery()) {
			List.Buffer<T> invoices = List.Buffer.empty();
			while (r.next()) {
				invoices.snoc(f.f(r));
			}
			return invoices.toList();
		}
	}
}