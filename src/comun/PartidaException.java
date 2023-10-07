package comun;

/**
 * Excepción para acción no permitida (ANP).
 */
public class PartidaException extends Exception {
	private static final long serialVersionUID = 8622028590496494809L;

	public PartidaException() {
		super();
	}
	
	public PartidaException(String str) {
		super(str);
	}
}
