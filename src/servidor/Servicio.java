package servidor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import comun.PartidaException;
import comun.AliasNoValidoException;
import comun.FaltanFichasException;
import comun.Ficha;
import comun.FichaNoValidaException;
import comun.FichasColocadas;
import comun.JuegoDomino;

import optional.Trace;

public class Servicio implements JuegoDomino {
	// Información Compartida
	// estados
	private static final int ESTADO_INICIAL = 0;
	private static final int JUGADOR_UNIDO = 1;
	private static final int JUGADOR_TOMA_FICHAS = 2;
	private static final int EN_JUEGO_SIN_TURNO = 3;
	private static final int EN_JUEGO_CON_TURNO = 4;
	private static final int FINAL_MANO = JuegoDomino.FINAL_MANO;
	private static final int FINAL_PARTIDA = JuegoDomino.FINAL_PARTIDA;
	
	private static volatile FichasDomino fichas = new FichasDomino();	// fichas del dominó y operaciones
	private static volatile int turno = -1;								// jugador con turno
	private static volatile int mano = -1;								// jugador mano
	private static volatile int jugadoresUnidos = 0;					// número de jugadores de la partida (unidos)
	private static volatile int jugadoresConFichas = 0;					// número de jugadores que han cogido
																		// fichas del pozo
	private static volatile int jugadoresPreparados = 0;				// Jugadores preparados para que comience
																		// la siguiente mano

	// alias de los jugadores de la partida almacenados según su orden
	private static volatile List<String> jugadores = new ArrayList<>(NUM_JUGADORES);
	// puntos de los jugadores de la partida almacenados según su orden
	private static volatile List<Integer> puntosJugadores = new ArrayList<>(NUM_JUGADORES);
	// puntos de los jugadores en la mano actual
	private static volatile List<Integer> puntosMano = new ArrayList<>(NUM_JUGADORES);
	// fichas de los jugadores de la partida almacenados según su orden
	private static volatile List<List<Ficha>> fichasJugadores = new ArrayList<>(NUM_JUGADORES);
	private static volatile boolean manoFinalizada = false;				// indica si una mano ha finalizado
	private static volatile boolean partidaFinalizada = false;			// indica si la partida ha finalizado
	
	// objetos para exclusión mutua
	private static volatile Object mutexTurno = new Object();
	private static volatile Object mutexJugadores = new Object();
	private static volatile Object mutexJugadoresUnidos = new Object();
	private static volatile Object mutexJugadoresPreparados = new Object();
	private static volatile Object mutexManoFinalizada = new Object();
	private static volatile Object mutexPartidaFinalizada = new Object();

	// información exclusiva de un OOS
	private int idCliente;			// identificador del cliente  (para este caso, sólo es
	                                // necesario si se hace uso de la traza)
	private String alias;			// alias de un jugador
	private int orden;				// posición que ocupa en la mesa
	private List<Ficha> misFichas;	// fichas del jugador
	private int estado;				// estado
	
	public Servicio(int idCliente) {
		this.idCliente = idCliente;
		this.alias = null;
		this.misFichas = null;
		this.estado = Servicio.ESTADO_INICIAL;
		
		// Inicializa las estructuras de datos compartidas. Pero solo lo hace 
		// uno de los clientes
		synchronized (mutexJugadores) {			
			if (Servicio.jugadores.isEmpty()) { 
				for (int k = 0; k < JuegoDomino.NUM_JUGADORES; k++) {
					Servicio.jugadores.add(null);
					Servicio.fichasJugadores.add(null);
					Servicio.puntosJugadores.add(0);
					Servicio.puntosMano.add(0);
				}
			}
		}
	}
	
	private int siguiente(int n) {
		return (n + 1) % Servicio.jugadoresUnidos;
	}
	
	@Override
	public int unirsePartida(String alias)
			throws AliasNoValidoException, PartidaException {
		int jUnidos;
		
		synchronized (Servicio.mutexJugadoresUnidos) {		
			if (Servicio.jugadoresUnidos == JuegoDomino.NUM_JUGADORES) {
				throw new PartidaException("la partida ya ha comenzado");
			}
		}

		if (alias == null || alias.isEmpty()) {
			throw new AliasNoValidoException("Alias nulo o vacío");
		}
		
		synchronized (Servicio.mutexJugadores) {		
			if (Servicio.jugadores.contains(alias)) {
				throw new AliasNoValidoException("Alias ya existente");
			}
		}

		this.alias = alias;
		
		// asignar posicición en la mesa a este jugador,
		// añadir a la lista de jugadores y cambiar el estado
		synchronized (Servicio.mutexJugadoresUnidos) {		
			this.orden = Servicio.jugadoresUnidos++;
			jUnidos = Servicio.jugadoresUnidos;
		}

		Servicio.jugadores.set(this.orden, this.alias);
		this.estado = Servicio.JUGADOR_UNIDO; // esperando oponentes para jugar la primera mano		

		// ¿Se acaba de unir el último jugador?
		// MUY IMPORTANTE HACER ESTO AQUÍ PARA QUE 
		// LO SIGUIENTE SE HAGA UNA ÚNICA VEZ
		if (jUnidos == JuegoDomino.NUM_JUGADORES) {
			// Se acaba de unir el último jugador
			// Inicializamos el turno inicial y volcamos 
			// las fichas al pozo
			Trace.println("Se han unido todos los jugadores");
			Trace.print("Volcando las fichas al pozo ... ");
			// preparar las fichas para el juego
			Servicio.fichas.volcarFichasAlPozo();
			Trace.println("hecho.");
			// jugador mano elegido al azar
			Trace.print("Eligiendo el primer jugador mano: ");
			Random r = new Random();
			Servicio.mano = r.nextInt(jUnidos);
			Trace.printf("%d (%s)\n", Servicio.mano, Servicio.jugadores.get(Servicio.mano));
			// turno para coger las fichas
			synchronized (Servicio.mutexTurno) {
				Servicio.turno = this.siguiente(Servicio.mano);
			}
		}			

		// Un jugador preparado más. Importante que esta sea la última operación
		// de esta función, sobre todo que esté después del if anterior
		synchronized (Servicio.mutexJugadoresPreparados) {		
			Servicio.jugadoresPreparados++;
		}
		
		Trace.printf(this.idCliente, "Jugador unido: %d (%s)\n", this.orden, this.alias);	
		return this.orden;
	}
	
	@Override
	public boolean todosPreparados() {
		synchronized (Servicio.mutexJugadoresPreparados) {
			if (Servicio.jugadoresPreparados == JuegoDomino.NUM_JUGADORES) {
				this.estado= Servicio.JUGADOR_TOMA_FICHAS;
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	@Override
	public void cogerFichas() {
		// coger fichas del pozo
		try {
			Trace.printf(this.idCliente,
					"Jugador %d cogiendo fichas del pozo ... ", this.orden);
			this.misFichas = Servicio.fichas.cogerDelPozo(JuegoDomino.FICHAS_AL_INICIO);
			Servicio.fichasJugadores.set(this.orden, this.misFichas);
			Trace.println("hecho.\n");
			Trace.printf(this.idCliente, "Fichas del jugador %d: %s\n", this.orden, this.misFichas);
		} catch (FaltanFichasException e) {
			e.printStackTrace();
		}
		
		// este jugador ya tiene sus fichas (está en juego) y ya no tiene el turno
		this.estado = Servicio.EN_JUEGO_SIN_TURNO;
		
		synchronized (Servicio.mutexTurno) {
			if (++Servicio.jugadoresConFichas == Servicio.jugadoresUnidos) { // todos los jugadores tienen sus fichas
				// Comienza la partida y el turno es para el jugador que tiene la mano!!
				Servicio.turno = Servicio.mano;
			} else {
				// Todavía estamos en la fase de robar fichas iniciales.
				// Pasamos el turno de robar fichas iniciales al siguiente jugador
				Servicio.turno = this.siguiente(Servicio.turno);
			}
		}
	}
		
	@Override
	public int turno() {
		// Comprueba si finalizó la partida
		synchronized (Servicio.mutexPartidaFinalizada) {
			if (Servicio.partidaFinalizada) {
				this.estado = Servicio.FINAL_PARTIDA;
				return Servicio.FINAL_PARTIDA;
			}
		}

		// Comprueba si finalizó la mano
		synchronized (Servicio.mutexManoFinalizada) {
			if (Servicio.manoFinalizada) {
				this.estado = Servicio.FINAL_MANO;
				return Servicio.FINAL_MANO;
			}
		}

		// ¿Tenemos el turno?
		synchronized (Servicio.mutexTurno) {
			if (this.orden != Servicio.turno) {
				// No tenemos el turno. El estado no cambia!!
				return Servicio.turno;
			}
		}
		
		// Tenemos el turno pero podemos estar en dos situaciones
		// 1  Turno para robar las fichas iniciales de la mesa (estado == JUGADOR_TOMA_FICHAS)
		//      (el estado no cambia --> Lo cambia cogerFichas() )
		// 2  Turno para jugar normalmente (estado == EN_JUEGO_SIN/CON_TURNO)
		//       (Habrá que cambiar al estado EN_JUEGO_CON_TURNO si estamos en EN_JUEGO_SIN_TURNO)
		if (this.estado == Servicio.EN_JUEGO_SIN_TURNO) {
			this.estado = Servicio.EN_JUEGO_CON_TURNO;
		}
		
		return this.orden;
	}
	
	@Override
	public Ficha puntosAbiertos() {		
		return Servicio.fichas.puntosAbiertos();
	}
	
	/**
	 * Retorna cierto si la partida ha finalizado.
	 * @return {@code true} si la partida ha finalizado
	 */
	private boolean ganadorDeLaMano() {
		// Cálculo del número de puntos por jugar de cada jugador
		// (puntos de las fichas no jugadas)
		List<Integer> puntosPorJugar = new ArrayList<>(Servicio.jugadoresUnidos);
		for (List<Ficha> lf: Servicio.fichasJugadores ) {
			if (lf.isEmpty()) {
				puntosPorJugar.add(0);
			} 
			else {
				puntosPorJugar.add(lf.stream().mapToInt(f -> f.puntosFicha()).sum());
			}
		}
		
		// De momento, todos los jugadores han obtenido 0 puntos en esta mano
		for (int k = 0; k < JuegoDomino.NUM_JUGADORES; k++) {
			Servicio.puntosMano.set(k, 0);
		}
		
		if (this.misFichas.isEmpty()) {  
			// JUEGO DOMINADO
			// El jugador del turno actual (ESTE jugador) ha Dominado el juego
			// (puso todas las fichas), por tanto gana esta mano.
			// El jugador recibe como puntos la suma de todos los puntos de las fichas
			// del resto de jugadores
			int p = puntosPorJugar.stream().mapToInt(n -> n).sum();
			Servicio.puntosMano.set(this.orden, p);
			// Acumulamos estos puntos en sus puntos totales
			Servicio.puntosJugadores.set(this.orden,
					                     Servicio.puntosJugadores.get(this.orden) + p);
			return Servicio.puntosJugadores.get(this.orden) >= JuegoDomino.PUNTOS_DE_PARTIDA;
		} 
		else { 
			// JUEGO CERRADO
			// Gana el jugador que tenga menos puntos en la suma de sus fichas
			// En caso de empate gana la mano o el que esté más cerca de la mano por la derecha
			int minimo = Collections.min(puntosPorJugar);
			
			// Buscamos, a partir del jugador que tiene la mano, quién tiene ese mínimo
			// De esta forma ya se resuelven posibles casos de empate
			int pos = Servicio.mano;
			while (puntosPorJugar.get(pos) != minimo) {
				pos = this.siguiente(pos);
			}
					
			// El jugador pos ha ganado
			// El jugador recibe como punto la suma de todos los puntos del resto de jugadores
			// Sumamos las de todos y luego le quitamos los que tiene este jugador
			int p = puntosPorJugar.stream().mapToInt(n -> n).sum() - puntosPorJugar.get(pos);
			Servicio.puntosMano.set(pos, p);
			// Acumulamos estos puntos en sus puntos totales
			Servicio.puntosJugadores.set(pos, Servicio.puntosJugadores.get(pos) + p);

			return Servicio.puntosJugadores.get(pos) >= JuegoDomino.PUNTOS_DE_PARTIDA;
		}
	}
	
	/**
	 * Cambio de turno tras colocar una ficha o pasar
	 */
	private void cambiarTurno() {
		boolean finalPartida;
		
		if (!this.misFichas.isEmpty() && !Servicio.fichas.juegoCerrado()) {
			// La mano actual no ha finalizado. Cambiamos el turno
			this.estado = Servicio.EN_JUEGO_SIN_TURNO;
			synchronized (Servicio.mutexTurno) {
				Servicio.turno = this.siguiente(Servicio.turno);
			}
		}
		else { 
			// La mano actual ha finalizado porque un jugador se ha quedado sin fichas 
			// o porque el juego está cerrado
			
			
			// Obtener el jugador que gana esta mano y actualizar su puntuación
			// también comprobaremos si ha finalizado o no la partida
			finalPartida = this.ganadorDeLaMano();
			
			// Actualizar la información compartida que indica el final de una mano
			// y, si fuera el caso, de la partida.
			// 
			// Hacer este cambio lo último, durante el turno de juego de un jugador
			// (el resto está a la espera) y sin haber cambiado su estado, garantiza
			// el acceso exclusivo a toda la información compartida que se ha actualizado
			// previamente (siempre y cuando, como debe ser, el acceso a las operaciones
			// críticas esté controlado según el estado)
			if (finalPartida) {
				this.estado = Servicio.FINAL_PARTIDA;
				synchronized (Servicio.mutexPartidaFinalizada) {
					Servicio.partidaFinalizada = true;
				}
			} 
			else {
				this.estado = Servicio.FINAL_MANO;
				// De momento, no está preparado ningún jugador para iniciar la siguiente mano
				synchronized (Servicio.mutexJugadoresPreparados) {		
					Servicio.jugadoresPreparados = 0;
				}				
			}
			
			synchronized (Servicio.mutexManoFinalizada) {
				Servicio.manoFinalizada =  true;
			}
		}
	}
	
	@Override
	public void ponerFicha(Ficha ficha) throws FichaNoValidaException {
		if (this.estado == Servicio.EN_JUEGO_CON_TURNO) {
			if (!this.misFichas.contains(ficha)) {
				throw new FichaNoValidaException();
			}
			Servicio.fichas.ponerFicha(ficha);
			this.misFichas.remove(ficha);
			this.cambiarTurno();
		}
	}

	@Override
	public void ponerFicha(Ficha ficha, int puntos) throws FichaNoValidaException {	
		if (this.estado == Servicio.EN_JUEGO_CON_TURNO) {
			if (!this.misFichas.contains(ficha)) {
				throw new FichaNoValidaException();
			}
			Servicio.fichas.ponerFicha(ficha, puntos);
			this.misFichas.remove(ficha);
			this.cambiarTurno();
		}
	}

	@Override
	public void pasar() {
		if (this.estado == Servicio.EN_JUEGO_CON_TURNO) {
			this.cambiarTurno();
		}
	}

	@Override
	public Collection<Ficha> misFichas() {
		return this.misFichas;
	}
	
	@Override
	public List<Integer> numeroFichasJugadores() {
		List<Integer> ret = new ArrayList<Integer>();
		for (int i=0; i < Servicio.jugadoresUnidos; i++) {
			if (Servicio.fichasJugadores.get(i) != null) {
				ret.add(Servicio.fichasJugadores.get(i).size());
			}
			else {
				ret.add(0);
			}
		}
		return ret;
	}
	
	@Override
	public List<List<Ficha>> fichasJugadores() {
		// Operación ignorada si no se finalizó la mano y/o la partida (POR MOTIVOS DE SEGURIDAD)
		if (this.estado == Servicio.FINAL_MANO || this.estado == Servicio.FINAL_PARTIDA) {
			return Servicio.fichasJugadores;
		}
		return null;
	}

	@Override
	public List<String> jugadoresUnidos() {
		return Servicio.jugadores;
	}
	
	public void siguienteMano() {
		// Por seguridad revisa que se llame a esta
		// operación cuando es necesario
		if (this.estado != Servicio.FINAL_MANO) {
			return;
		}
		
		// revertir la información exclusiva de cada OOS
		// al estado en que se unió a la partida
		this.misFichas.clear();
		this.misFichas = null;

		// este jugador se ha incorporado a la nueva
		// mano, le toca coger las fichas iniciales
		this.estado = Servicio.JUGADOR_UNIDO;

		// Un jugador preparado más.
		synchronized (Servicio.mutexJugadoresPreparados) {		
			Servicio.jugadoresPreparados++;

			// tras incorporarse todos los jugadores a la nueva mano,
			// revetir la información compartida al estado en que
			// todos los jugadores se unieron al juego
			if (Servicio.jugadoresPreparados == Servicio.jugadoresUnidos) {
				//Servicio.ganadoresMano = null;
				Trace.println("Todos los jugadores listos para comenzar una nueva mano.");
				Trace.print("Volcando las fichas al pozo ... ");
				// preparar las fichas para el juego
				Servicio.fichas.volcarFichasAlPozo();
				Trace.println("hecho.");
				Servicio.jugadoresConFichas = 0;
				Servicio.manoFinalizada = false;
				  // la mano pasa al jugador situado a su derecha
				Servicio.mano = this.siguiente(Servicio.mano);
				synchronized (Servicio.mutexTurno) {
					// turno para coger fichas
					Servicio.turno = this.siguiente(Servicio.mano); 
				}
			}
		}
	}

	@Override
	public synchronized void close() {
		Trace.println("¡EL CLIENTE " + this.idCliente + " SE HA DESCONECTADO!");
		Servicio.jugadoresUnidos--;
		if (Servicio.jugadoresUnidos == 0) {
			// todos los clientes se han desconectado
			Servicio.turno = -1;
			Servicio.mano = -1;
			Servicio.manoFinalizada = false;
			Servicio.partidaFinalizada = false;
			Servicio.jugadoresConFichas = 0;
			Servicio.jugadoresPreparados = 0;
			for (int k = 0; k < JuegoDomino.NUM_JUGADORES; k++) {
				Servicio.jugadores.set(k, null);
				Servicio.fichasJugadores.set(k, null);
				Servicio.puntosJugadores.set(k, 0);
				Servicio.puntosMano.set(k, 0);
			}
		}
	}

	@Override
	public List<String> listaJugadores() {
		List<String> l = new LinkedList<>();
		for (int k = 0; k < Servicio.jugadoresUnidos; k++) {
			l.add(Servicio.jugadores.get(k));
		}
		
		return l;
	}

	@Override
	public FichasColocadas fichasColocadas() {
		return Servicio.fichas.fichasJugadas();
	}

	@Override
	public List<List<Integer>> informePuntos() {
		List<List<Integer>> ret = new ArrayList<List<Integer>>(2);
		
		// Operación ignorada si no se finalizó la mano y/o la partida
		if (this.estado == Servicio.FINAL_MANO || this.estado == Servicio.FINAL_PARTIDA) {
			ret.add(Servicio.puntosMano);
			ret.add(Servicio.puntosJugadores);
			return ret;
		}
		
		return null;
	}
}
