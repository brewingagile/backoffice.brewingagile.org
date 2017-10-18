package org.brewingagile.backoffice.types;

import fj.data.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class AccountSecret {
	public final UUID value;

	private AccountSecret(UUID value) {
		this.value = Objects.requireNonNull(value);
	}

	public static AccountSecret accountSecret(UUID value) {
		return new AccountSecret(value);
	}

	public static Option<AccountSecret> parse(String x) {
		try {
			return Option.some(UUID.fromString(x)).map(AccountSecret::accountSecret);
		} catch (IllegalArgumentException e) {
			return Option.none();
		}
	}
}
