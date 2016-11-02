package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonNode;
import fj.F;
import fj.data.Option;
import org.brewingagile.backoffice.db.operations.TicketsSql;

import static argo.jdom.JsonNodeFactories.nullNode;
import static argo.jdom.JsonNodeFactories.string;

public class ToJson {
	public static <A> JsonNode nullable(Option<A> oa, F<A,JsonNode> f) {
		return oa.map(f).orSome(nullNode());
	}
	public static JsonNode json(TicketsSql.TicketName x) { return string(x.ticketName);	}
}
