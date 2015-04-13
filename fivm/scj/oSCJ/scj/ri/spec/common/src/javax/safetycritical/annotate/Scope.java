package javax.safetycritical.annotate;

public @interface Scope {
	public static final String CALLER = "CALLER";
	public static final String IMMORTAL = "IMMORTAL";
	public static final String THIS = "THIS";
	public static final String UNKNOWN = "UNKNOWN";

	public String value();
}
