package functional;

import java.util.Objects;

public final class Tuple3<T1, T2, T3> {
	public final T1 _1;
	public final T2 _2;
	public final T3 _3;

	private Tuple3(T1 t1, T2 t2, T3 t3) {
		this._1 = Objects.requireNonNull(t1);
		this._2 = Objects.requireNonNull(t2);
		this._3 = Objects.requireNonNull(t3);
	}

	public T1 fst() { return _1; }
	public T2 snd() { return _2; }

	public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 t1, T2 t2, T3 t3) {
		return new Tuple3<>(t1, t2, t3);
	}

	@Override
	public String toString() {
		return "Tuple3 [_1=" + _1 + ", _2=" + _2 + ", _3=" + _3 + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Tuple3 tuple3 = (Tuple3) o;

		if (!_1.equals(tuple3._1)) return false;
		if (!_2.equals(tuple3._2)) return false;
		if (!_3.equals(tuple3._3)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _1.hashCode();
		result = 31 * result + _2.hashCode();
		result = 31 * result + _3.hashCode();
		return result;
	}
}
