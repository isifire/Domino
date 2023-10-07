package comun;

/**
 * Excepción para acción no permitida (ANP).
 */
public class AliasNoValidoException extends Exception {
	private static final long serialVersionUID = 8622028590496494809L;

	public AliasNoValidoException() {
		super();
	}
	
	public AliasNoValidoException(String str) {
		super(str);
	}
}
