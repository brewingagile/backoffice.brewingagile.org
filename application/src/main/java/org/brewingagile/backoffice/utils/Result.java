package org.brewingagile.backoffice.utils;

public final class Result {
	public final String message;
	public final boolean success;
	public final Object data;
	
	private Result(String message, boolean success, Object data) {
		this.message = message;
		this.success = success;
		this.data = data;
	}
	
	public static Result success(String message) { return new Result(message, true, null); }
	public static Result success(String message, Object data) { return new Result(message, true, data); }
	public static Result failure(String message) { return new Result(message, false, null); }
	public static Result failure(String message, Object data) { return new Result(message, false, data); }
}