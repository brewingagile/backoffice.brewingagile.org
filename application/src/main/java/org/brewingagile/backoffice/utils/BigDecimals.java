package org.brewingagile.backoffice.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

public class BigDecimals {
	public static BigInteger inOre(BigDecimal totalTicketIncsVat) {
		return totalTicketIncsVat.multiply(BigDecimal.valueOf(100)).toBigInteger();
	}
}
