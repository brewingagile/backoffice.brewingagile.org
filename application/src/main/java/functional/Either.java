package functional;

import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

public final class Either<L,A> {
	private final L left;
	private final A right;

	private Either(L left, A right) {
		this.left = left;
		this.right = right;
	}

	public static <L,A> Either<L,A> left(L value) {
		if (value == null) throw new IllegalArgumentException("value may not be null");
		return new Either<>(value, null);
	}

	public static <L,A> Either<L,A> right(A value) {
		if (value == null) throw new IllegalArgumentException("value may not be null");
		return new Either<>(null, value);
	}

	public L left() {
		if (!isLeft()) throw new IllegalStateException("I'm right. Right is: " + right);
		return left;
	}

	public A right() {
		if (!isRight()) throw new IllegalStateException("I'm left. Left is: " + left);
		return right;
	}

	public Optional<L> leftOptional() {
		return Optional.fromNullable(left);
	}

	public Optional<A> rightOptional() {
		return Optional.fromNullable(right);
	}

	public boolean isLeft() { return left != null; }
	public boolean isRight() { return right != null; }

	@Override
	public String toString() {
		return Objects.toStringHelper(Either.class).add("left", left).add("right", right).toString();
	}

	@SafeVarargs
	public static <R> List<R> rights(Either<?,R> ... eithers) {
		List<R> rights = new ArrayList<>();
		for (Either<?, R> either : eithers) {
			if (either.isLeft()) continue;
			rights.add(either.right());
		}
		return rights;
	}

	@SafeVarargs
	public static <L> List<L> lefts(Either<L,?> ... eithers) {
		List<L> lefts = new ArrayList<>();
		for (Either<L,?> either : eithers) {
			if (either.isRight()) continue;
			lefts.add(either.left());
		}
		return lefts;
	}

	public <B> Either<L,B> transform(Function<A, B> fn) {
		if (isRight()) return Either.right(fn.apply(this.right));
		return Either.left(this.left);
	}

	public <B> Either<L,B> bind(Function<A, Either<L,B>> fn) {
		if (isRight()) return fn.apply(this.right);
		return Either.left(this.left);
	}

	public Either<A, L> flip() {
		return new Either<>(right, left);
	}

	public <B> Either<B,A> transformLeft(Function<L, B> fn) {
		if (isLeft()) return Either.left(fn.apply(this.left));
		return Either.right(this.right);
	}

	public static <A, E extends Exception> A rightOrThrowLeft(Either<E, A> e) throws E {
		if (e.isLeft()) throw e.left;
		return e.right;
	}

	public static <L,R,B> B either(Either<L,R> e, Function<L,B> fl, Function<R,B> fr) {
		return (e.isLeft())?fl.apply(e.left):fr.apply(e.right);
	}

	public <B> B either(Function<L,B> fl, Function<A,B> fr) {
		return (this.isLeft())?fl.apply(this.left):fr.apply(this.right);
	}

	@Override
	public int hashCode() {
		if (isLeft()) return this.left.hashCode();
		return this.right.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Either<?, ?> other = (Either<?, ?>) obj;
		if (!java.util.Objects.equals(this.left, other.left)) {
			return false;
		}
		if (!java.util.Objects.equals(this.right, other.right)) {
			return false;
		}
		return true;
	}

	public A rightOr(A r) {
		if (!isRight()) return r;
		return right;
	}

	public static <T> T either(Either<T, T> e) {
		if (e.isLeft()) return e.left();
		return e.right();
	}
}