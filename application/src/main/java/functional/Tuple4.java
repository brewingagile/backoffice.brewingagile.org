package functional;

import java.util.Objects;

public final class Tuple4<T1, T2, T3, T4> {
	public final T1 _1;
	public final T2 _2;
	public final T3 _3;
	public final T4 _4;

	private Tuple4(T1 t1, T2 t2, T3 t3, T4 t4) {
		this._1 = Objects.requireNonNull(t1);
		this._2 = Objects.requireNonNull(t2);
		this._3 = Objects.requireNonNull(t3);
		this._4 = Objects.requireNonNull(t4);
	}

	public T1 fst() { return _1; }
	public T2 snd() { return _2; }

	public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(T1 t1, T2 t2, T3 t3, T4 t4) {
		return new Tuple4<>(t1, t2, t3, t4);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Tuple4 tuple4 = (Tuple4) o;

		if (!_1.equals(tuple4._1)) return false;
		if (!_2.equals(tuple4._2)) return false;
		if (!_3.equals(tuple4._3)) return false;
		if (!_4.equals(tuple4._4)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _1.hashCode();
		result = 31 * result + _2.hashCode();
		result = 31 * result + _3.hashCode();
		result = 31 * result + _4.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "Tuple4{" +
			"_1=" + _1 +
			", _2=" + _2 +
			", _3=" + _3 +
			", _4=" + _4 +
			'}';
	}
}
