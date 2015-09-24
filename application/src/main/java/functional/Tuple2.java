package functional;


import java.util.Objects;

public final class Tuple2<T1, T2> {
	public final T1 _1;
	public final T2 _2;

	private Tuple2(T1 t1, T2 t2) {
		this._1 = Objects.requireNonNull(t1);
		this._2 = Objects.requireNonNull(t2);
	}

	public T1 fst() { return _1; }
	public T2 snd() { return _2; }

	public static <T1, T2> Tuple2<T1, T2> of(T1 t1, T2 t2) {
		return new Tuple2<>(t1, t2);
	}

	@Override
	public String toString() {
		return "Tuple2 [_1=" + _1 + ", _2=" + _2 + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Tuple2 tuple2 = (Tuple2) o;

		if (!_1.equals(tuple2._1)) return false;
		if (!_2.equals(tuple2._2)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _1.hashCode();
		result = 31 * result + _2.hashCode();
		return result;
	}
}
