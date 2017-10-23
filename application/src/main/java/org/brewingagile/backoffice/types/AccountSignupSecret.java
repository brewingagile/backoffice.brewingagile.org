package org.brewingagile.backoffice.types;

import fj.data.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class AccountSignupSecret {
	public final UUID value;

	private AccountSignupSecret(UUID value) {
		this.value = Objects.requireNonNull(value);
	}

	public static AccountSignupSecret accountSignupSecret(UUID value) {
		return new AccountSignupSecret(value);
	}

	public static Option<AccountSignupSecret> parse(String x) {
		try {
			return Option.some(UUID.fromString(x)).map(AccountSignupSecret::accountSignupSecret);
		} catch (IllegalArgumentException e) {
			return Option.none();
		}
	}
}
