package comun;

/**
 * Excepción para acción no permitida (ANP).
 */
public class FaltanFichasException extends Exception {
	private static final long serialVersionUID = 8622028590496494809L;

	public FaltanFichasException() {
		super();
	}
	
	public FaltanFichasException(String str) {
		super(str);
	}
}
