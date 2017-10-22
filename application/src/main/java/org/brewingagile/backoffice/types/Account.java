package org.brewingagile.backoffice.types;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.brewingagile.backoffice.utils.Strings;

import java.util.Objects;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class Account {
	public final String value;

	private Account(String x) {
		this.value = Objects.requireNonNull(Strings.emptyToNull(x));
	}

	public static Account account(String s) {
		return new Account(s);
	}

	public static fj.Ord<Account> Ord = fj.Ord.ord(l -> r -> fj.Ord.stringOrd.compare(l.value, r.value));
}
