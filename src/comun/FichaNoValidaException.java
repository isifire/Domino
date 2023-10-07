package comun;

/**
 * Excepción para acción no permitida (ANP).
 */
public class FichaNoValidaException extends Exception {
	private static final long serialVersionUID = 8622028590496494809L;

	public FichaNoValidaException() {
		super();
	}
	
	public FichaNoValidaException(String str) {
		super(str);
	}
}
