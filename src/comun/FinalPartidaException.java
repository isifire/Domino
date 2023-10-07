package comun;

/**
 * Excepción para acción no permitida (ANP).
 */
public class FinalPartidaException extends Exception {
	private static final long serialVersionUID = -3803429672080979861L;

	public FinalPartidaException() {
		super();
	}
	
	public FinalPartidaException(String str) {
		super(str);
	}
}
