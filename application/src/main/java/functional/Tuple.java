package functional;

public class Tuple {
	public static <T1,T2> Tuple2<T1,T2> of(T1 t1, T2 t2) { return Tuple2.of(t1, t2); }
	public static <T1,T2,T3> Tuple3<T1,T2,T3> of(T1 t1, T2 t2, T3 t3) { return Tuple3.of(t1, t2, t3); }
	public static <T1,T2,T3,T4> Tuple4<T1,T2,T3,T4> of(T1 t1, T2 t2, T3 t3, T4 t4) { return Tuple4.of(t1, t2, t3, t4); }
}
