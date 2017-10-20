package org.brewingagile.backoffice.types;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.brewingagile.backoffice.utils.Strings;

import java.util.Objects;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class TicketName {
	public final String ticketName;

	private TicketName(String ticketName) {
		this.ticketName = Objects.requireNonNull(Strings.emptyToNull(ticketName));
	}

	public static TicketName ticketName(String s) {
		return new TicketName(s);
	}

	public static fj.Ord<TicketName> Ord = fj.Ord.ord(l -> r -> fj.Ord.stringOrd.compare(l.ticketName, r.ticketName));
}
