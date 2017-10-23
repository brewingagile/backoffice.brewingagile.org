package org.brewingagile.backoffice.rest.json;

import argo.jdom.JsonNode;
import fj.F;
import fj.data.Option;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.AccountSecret;
import org.brewingagile.backoffice.types.AccountSignupSecret;
import org.brewingagile.backoffice.types.TicketName;

import static argo.jdom.JsonNodeFactories.nullNode;
import static argo.jdom.JsonNodeFactories.string;

public class ToJson {
	public static <A> JsonNode nullable(Option<A> oa, F<A,JsonNode> f) {
		return oa.map(f).orSome(nullNode());
	}
	public static JsonNode json(TicketName x) { return string(x.ticketName); }
	public static JsonNode account(Account x) { return string(x.value); }
	public static JsonNode ticketName(TicketName x) { return string(x.ticketName); }
	public static JsonNode accountSecret(AccountSecret x) { return string(x.value.toString()); }
	public static JsonNode accountSignupSecret(AccountSignupSecret x) { return string(x.value.toString()); }
}
