package common;

public class Fail extends Error {
	private static final long serialVersionUID = 348016084749739018L;

	//Used for Error Code ONLY
	public Fail() {
		super();
	}
	public Fail(String reason) {
		super(reason);
	}

	public Fail(Throwable t) {
		super(t);
	}

	public static void abort(String reason) {
		try {
			throw new Fail(reason);
		} catch (Throwable e) {
			try {
				e.printStackTrace();
			} catch (Throwable e2) {}
			System.err.println("FAILED!");
			System.exit(1);
		}
	}

	public static void abort(Throwable reason) {
		try {
			throw new Fail(reason);
		} catch (Throwable e) {
			try {
				e.printStackTrace();
			} catch (Throwable e2) {}
			System.err.println("FAILED!");
			System.exit(1);
		}
	}
}

