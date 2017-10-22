package org.brewingagile.backoffice.types;

import fj.P2;
import fj.data.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.BigInteger;

@EqualsAndHashCode
@ToString
public class AccountPackage {
	public final String description;
	public final BigDecimal price;
	public final List<P2<TicketName, BigInteger>> tickets;

	public AccountPackage(
		String description,
		BigDecimal price,
		List<P2<TicketName, BigInteger>> tickets
	) {
		this.description = description;
		this.price = price;
		this.tickets = tickets;
	}
}
