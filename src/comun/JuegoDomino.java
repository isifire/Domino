 package comun;

import java.util.Collection;
import java.util.List;

/**
 * Interfaz de servicio para el juego del dominó.
 */
public interface JuegoDomino extends lib.DefaultService {
	/**
	 * Número de jugadores necesarios para jugar una partida.
	 */
	static final int NUM_JUGADORES = 4;

	/**
	 * Valor del turno al final de una mano.
	 */
	static final int FINAL_MANO = -5;
	
	/**
	 * Valor del turno al final de una partida.
	 */
	static final int FINAL_PARTIDA = -10;

	/**
	 * Número de fichas de cada jugador al inicio de la partida.
	 */
	static final int FICHAS_AL_INICIO = 7;
	
	/**
	 * Puntos que deben obtenerse o superarse para ganar la partida.
	 */
	static final int PUNTOS_DE_PARTIDA = 100;

	/**
	 * El jugador (usuario del programa cliente) con el alias
	 * especificado se une a la partida.
	 * 
	 * <p>La partida la crea el primer jugador que se une y los
	 * jugadores se disponen en la mesa en el mismo orden en que
	 * se unen a ésta según en el sentido contrario a las agujas
	 * del reloj.</p>
	 *
	 * @param alias el alias del jugador
	 * @return El número que le corresponde en la mesa
	 * @throws PartidaException si la partida ya ha comenzado
	 * @throws AliasNoValidoException si el alias proporcionado
	 * es nulo o vacío, o si ya se está utilizando
	 */
	int unirsePartida(String alias)
			throws AliasNoValidoException, PartidaException;
	
	
	/**
	 * La lista de alias de los jugadores según el orden que ocupan
	 * en la mesa de juego. Sólo tras unirse todos los jugadores.
	 * @return la lista de alias de los jugadores según el orden
	 * que ocupan en la mesa de juego
	 */
	List<String> listaJugadores();
	
	/**
	 * El jugador (usuario del programa cliente) llamará a esta
	 * función para saber si todos los jugadores están ya preparados para
	 * jugar la siguiente mano
	 * 
	 * <p>La partida comenzará cuando todos los jugadores estén preparados.</p>
	 *
	 * @return cierto si todos los jugadores se han unido. Falso en otro caso
	 */
	boolean todosPreparados();

	/**
	 * El jugador coge del pozo las fichas especificadas.
	 */
	void cogerFichas();
	
	/**
	 * Indica al jugador si tiene o no el turno de juego, o bien, una
	 * condición relevante del transcurso de la partida.
	 * 
	 * @return el número del jugador (su orden en la mesa) que tiene el turno
	 * de juego o un valor especial
	 */
	int turno();
	
	/**
	 * Retorna las fichas jugadas y colocadas en la mesa.
	 * @return las fichas jugadas y colocadas en la mesa
	 */
	FichasColocadas fichasColocadas();
	
	/**
	 * Retorna una ficha ficticia con los puntos abiertos (donde se
	 * puede colocar una nueva ficha) que hay en los extremos de las
	 * fichas colocadas, o {@code null} si no hay fichas colocadas
	 * en la mesa (al inicio del juego).
	 * @return una ficha que indica los puntos que se pueden jugar,
	 * {@code null} si no hay fichas colocadas.
	 */
	Ficha puntosAbiertos();
	
	/**
	 * Añade la ficha especificada al extremo que corresponda de las
	 * fichas ya colocadas.
	 * @param ficha la ficha a colocar
	 * @throws FichaNoValidaException si la ficha no se puede colocar
	 * o es una ficha ficticia.
	 */
	void ponerFicha(Ficha ficha) throws FichaNoValidaException;
	
	/**
	 * Pasa turno sin poner ficha.
	 */
	void pasar();

	/**
	 * Añade la ficha especificada al extremo de las fichas ya
	 * colocadas y que tiene los puntos dados.
	 * <p> Esta operación sólo es útil cuando la ficha se puede
	 * colocar en ambos extremos, no es un doble y el jugador quiere
	 * precisar los puntos a colocar (el cuadrado A o B de la ficha
	 * que se une al resto de fichas colocadas).</p>
	 * @param ficha la ficha a colocar
	 * @param puntos los puntos del extremo donde ha de colocarse
	 * @throws FichaNoValidaException si la ficha no se puede colocar
	 * o es una ficha ficticia
	 */
	void ponerFicha(Ficha ficha, int puntos)
			throws FichaNoValidaException;
	
	/**
	 * Retorna las fichas que este jugador tiene por colocar.
	 * @return las fichas que el jugador tiene por colocar
	 */
	Collection<Ficha> misFichas();

	/**
	 * Retorna el número de fichas que tiene cada jugador.
	 * @return Una lista con el número de fichas de cada jugador
	 */
	List<Integer> numeroFichasJugadores();

	/**
	 * Retorna las fichas de cada jugador. Solo si acabó la mano
	 * @return Una lista con la lista de fichas de cada jugador 
	 */
	List<List<Ficha>> fichasJugadores();
	
	/**
	 * Retorna los alias de los jugadores unidos a la partida.
	 * @return los alias de los jugadores unidos a la partida
	 */
	Collection<String> jugadoresUnidos();
	
	/**
	 * Permite jugar una nueva mano.
	 */
	void siguienteMano();
	
	/**
	 * Retorna la lista con los puntos de cada jugador en la mano actual
	 * y otra lista con los puntos totales de cada jugador en toda la partida
	 * Esta operación será ignorada si no se acabó una mano
	 * @return Una lista con las dos listas mencionadas anteriormente
	 */
	List<List<Integer>> informePuntos();
		
}
