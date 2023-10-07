package comun;

/**
 * Como se indica en el enunciado, la cara superior de una <strong>ficha</strong>
 * está dividida en dos cuadrado iguales en los que aparece una
 * cifra de {@code 0} a {@code 6} (mediante puntos como en los dados).
 * 
 * <p>Aunque desde el punto de vista del juego ambos cuadrados son
 * indistinguibles, por conveniencia, tanto aquí como en la clase
 * FichasDomino nos referiremos a ambos cuadrados como A o B.</p>
 */
public interface Ficha {
	
	/**
	 * Retorna cierto si esta ficha es la ficha nula.
	 * @return {@code true} si esta ficha es la ficha nula.
	 */
	boolean esNula();
	
	/**
	 * Retorna los puntos de un cuadrado de esta ficha.
	 * @return los puntos del cuadrado A
	 */
	int puntosA();
	
	/**
	 * Retorna los puntos del cuadrado B de esta ficha.
	 * @return los puntos del cuadrado B
	 */
	int puntosB();


	/**
	 * Retorna el total de puntos de esta ficha.
	 * @return su número de puntos
	 */
	int puntosFicha();
	
	/**
	 * Intercambia los puntos de los cuadrados A y B
	 */
	void swap();
	
	/**
	 * Retorna cierto si alguno de los cuadrados de la ficha
	 * especificada tiene puntos en común con alguno de los
	 * cuadrados de esta ficha.
	 * @param f la ficha dada
	 * @return {@code true} si ambas fichas tienen puntos
	 * en común.
	 */
	boolean casan(Ficha f);
	
	/**
	 * Retorna cierto si la ficha especificada tiene el mismo
	 * par de puntos que esta ficha.
	 * @param f la ficha dada para comparar
	 * @return {@code true} si la ficha dada tiene el mismo
	 * par de puntos que esta ficha
	 */
	boolean equals(Ficha f);
	
}
