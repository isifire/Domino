package cliente;

import java.awt.EventQueue;

import java.io.IOException;
import java.net.UnknownHostException;
import comun.AliasNoValidoException;
import comun.Ficha;
import comun.FichaNoValidaException;
import comun.FichasColocadas;
import comun.JuegoDomino;
import comun.PartidaException;
import lib.ChannelException;
import lib.CommClient;
import lib.ProtocolMessages;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ClienteGraficoDomino {
	private static final int DEMORA = 500;	
	private static final int DEMORA_AUTOCONEXION = 10000;

	// Campos utilizados por la UI para 
	// indicar las acciones que debe realizar el cliente
	// El cliente esperará (wait) hasta que la UI dejé 
	// la información necesaria (y el UI hará entonces un notify)
	// Estas variables son compartidas con el hilo de la UI
	volatile String dirServer;
	volatile String alias;
	volatile boolean unirsePartida;
	volatile boolean siguienteMano;
	volatile int fichaElegida, mitadElegida;
	volatile boolean pasarTurno;
	
	// Objetos utilizados para realizar la sincronización
	// y la exclusión mutua de las variables anteriores
	volatile Object syncUnirsePartida;
	volatile Object syncAccionTurno;
	volatile Object syncSiguienteMano;
	
	// Campos necesarios para el cliente
	private CommClient com;
	private List<String> jugadores;
	private List<Integer> puntuaciones;
	int orden;
	private int turno;
	
	/******************************************************************
	 *  Los campos siguientes son para actualizar la UI (parte gráfica)
	 *  NO MODIFICAR --> ESTOS CAMPOS NO ENTRAN PARA EL EXAMEN
	 *******************************************************************/
	// Almacena el objeto UI
	// Se inicializa desde la UI
	UIDomino UI;
	
	// Guarda el turno en que se actualizó por última vez la UI
	// Sirve para no refrescar la UI innecesariamente
	private int turnoAnterior;

	// Campos utilizados para asegurarse de que la UI está ya creada
	// De no ser así se hace un wait() hasta que así sea
	volatile boolean creadoUI;
	volatile Object syncCreadoUI;

	// Campos compartidos utilizados para indicar a la UI el turno actual
	// (no se utiliza el campo turno para no complicar la exclusión mutua)
	volatile int turnoUI;
	volatile Object mutexTurnoUI;

	// Campos utilizados por el cliente
	// para suministrar información a la UI
	private List<InfoJugadorPanel> infoJugadores;
	private List<FichasJugadorPanel> infoFichasJugadores;
	
	
	// CONSTRUCTOR
	public ClienteGraficoDomino() {
		this.com = null;
		this.dirServer = "";
		this.alias = "";
		this.orden = -1;
		this.turno = -1;
		this.unirsePartida = false;
		this.syncUnirsePartida = new Object();
		this.syncAccionTurno = new Object();
		this.syncSiguienteMano = new Object();
		this.fichaElegida = -1;
		this.mitadElegida = -1;
		this.pasarTurno = false;
		this.siguienteMano = false;
		this.jugadores = null;
		this.infoJugadores = new ArrayList<InfoJugadorPanel>();
		this.infoFichasJugadores = new ArrayList<FichasJugadorPanel>();
		this.puntuaciones = new ArrayList<Integer>();
		for (int i=0; i<JuegoDomino.NUM_JUGADORES; i++) {
			this.infoJugadores.add(null);
			this.infoFichasJugadores.add(null);
			this.puntuaciones.add(0);
		}
		this.turnoAnterior = -1;
		this.creadoUI = false;
		this.syncCreadoUI = new Object();
		this.turnoUI = -1;
		this.mutexTurnoUI = new Object();
	}
	
	@SuppressWarnings("unchecked")
	private void mostrarJugadores() {		
		try {
			// crear mensaje a enviar
			ProtocolMessages peticion = new ProtocolMessages("listaJugadores");
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
			// esperar respuesta
			ProtocolMessages respuesta;
			respuesta = com.waitReply();
			this.jugadores = (List<String>)com.processReply(respuesta);				
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en mostrarJugadores: " + e.getMessage() + "!");
			return;
		}
		
		
	    // Actualiza los nombres de los jugadores en la UI
		// y su posición en la mesa
		// NO MODIFICAR LA SIGUIENTE LINEA
		this.actualizaJugadoresUI();
	}

	@SuppressWarnings("unchecked")
	private void mostrarNumeroFichasJugadores() {
		List<Integer> numFichas;
		try {
			// crear mensaje a enviar
			ProtocolMessages peticion = new ProtocolMessages("numeroFichasJugadores");
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
			// esperar respuesta
			ProtocolMessages respuesta;
			respuesta = com.waitReply();
			numFichas = (List<Integer>)com.processReply(respuesta);				
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en mostrarNumeroFichasJugadores: " + e.getMessage() + "!");
			return;
		}
				
	    // Actualiza el número de fichas en la UI
		// NO MODIFICAR LA SIGUIENTE LINEA
		this.actualizaFichasUI(numFichas);
	}
	
	@SuppressWarnings("unchecked")
	private void mostrarMisFichas() {
		Collection<Ficha> fichasPorColocar;
		
		try {
			// crear mensaje a enviar
			ProtocolMessages peticion = new ProtocolMessages("misFichas");
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
			// esperar respuesta
			ProtocolMessages respuesta;
			respuesta = com.waitReply();
			fichasPorColocar = (Collection<Ficha>)com.processReply(respuesta);
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en mostrarMisFichas: " + e.getMessage() + "!");
			return;
		}
		
	    // Actualiza las fichas de este jugador en la UI
		// NO MODIFICAR LA SIGUIENTE LINEA
		this.UI.fichasJugador.setFichasJugador(fichasPorColocar);
	}
	
	private void mostrarFichasJugadas() {
		FichasColocadas colocadas = null;
		
		try {
			// crear mensaje a enviar
			ProtocolMessages peticion = new ProtocolMessages("fichasColocadas");
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
			// esperar respuesta
			ProtocolMessages respuesta;
			respuesta = com.waitReply();
			colocadas = (FichasColocadas)com.processReply(respuesta);	
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en mostrarFichasJugadas: " + e.getMessage() + "!");
			return;
		}
		
		// Muestra en la UI todas las fichas sobre la mesa
		// NO MODIFICAR LA SIGUIENTE LINEA
		this.actualizaMesaUI(colocadas);
	}
	
	@SuppressWarnings("unchecked")
	private void informeDePuntos() {
		List<List<Ficha>> fichasJugadores = null;
		List<List<Integer>> informe = null;
		List<Integer> puntosMano = null;
		
		// Obtiene las fichas de todos los jugadores
		try {
			// crear mensaje a enviar
			ProtocolMessages peticion = new ProtocolMessages("fichasJugadores");
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
			// esperar respuesta
			ProtocolMessages respuesta;
			respuesta = com.waitReply();
			fichasJugadores = (List<List<Ficha>>)com.processReply(respuesta);
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en informeDePuntos: " + e.getMessage() + "!");
		}
		
		// Obtiene los puntos de esta mano y los puntos totales
		try {
			// crear mensaje a enviar
			ProtocolMessages peticion = new ProtocolMessages("informePuntos");
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
			// esperar respuesta
			ProtocolMessages respuesta;
			respuesta = com.waitReply();
			informe = (List<List<Integer>>)com.processReply(respuesta);
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en informeDePuntos: " + e.getMessage() + "!");
		}
		
		// Obtiene quien gano la manó y los puntos obtenidos
		if (informe != null) {
			puntosMano = informe.get(0);
			this.puntuaciones = informe.get(1);
		}		
		else {
			this.UI.panelMesa.setTexto("¡¡HA HABIDO UN PROBLEMA CON EL SERVIDOR Y NO PUEDO DAR INFORME DE PUNTOS!!");
			return;
		}
		
	
		// Informa de quién ganó la mano e indica sus puntos en la UI
		// NO MODIFICAR LA SIGUIENTE LINEA
		this.actualizaGanadorManoUI(puntosMano);
		
		// Revela las fichas que les quedaron al resto de jugadores
		// E indica la suma de puntos de las fichas no jugadas de cada jugador (en la UI)
		// NO MODIFICAR LA SIGUIENTE LINEA
		this.actualizaFichasJugadoresUI(fichasJugadores);		
	}

	int unirse() {
		int orden = -1;
		
		if (this.com == null) {
			// Todavía no se conectó al servidor. Lo hace ahora
			try {
				// establecer la comunicación con el servidor
				// crear el canal de comunicación y establecer la
				// conexión con el servicio
				this.com = new CommClient(this.dirServer, 5000);
				//this.com = new CommClient();
				// activa el registro de mensajes del cliente
				com.activateMessageLog();  // opcional			
			} catch (UnknownHostException e) {
				this.UI.panelMesa.setTexto("Servidor desconocido " + e.getMessage());
				return orden;
			} catch (IOException | ChannelException e) {
				this.UI.panelMesa.setTexto("¡No he podido conectarme al servidor! " + e.getMessage());
				return orden;
			}
		}

		// Ahora intenta unirse a la partida
		// crear mensaje a enviar
		ProtocolMessages peticion = new ProtocolMessages("unirsePartida", this.alias);
		
		try {
			// enviar mensaje
			com.sendEvent(peticion);
			// esperar por la respuesta
			ProtocolMessages respuesta = com.waitReply();
			// procesar respuesta o excepción
			orden = (int)com.processReply(respuesta);
		} catch (PartidaException e) {
			this.UI.panelMesa.setTexto("La partida ya ha comenzado. No puedes unirte");
			orden = -1;
		} catch (AliasNoValidoException e) {
			this.UI.panelMesa.setTexto("Tu alias " + this.alias + "ya existe en esta partida. Ponte otro");
			orden = -1;
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡No he podido unirme a la partida! " + e.getMessage());
			orden = -1;
		}
		
		if (orden != -1) {									
			this.UI.panelMesa.setTexto("Te has unido correctamente. Esperando al resto de jugadores...");
		}
		
		return orden;
	}
	
	private boolean manoIniciada() {
		boolean ret = false;
		
		// crear mensaje a enviar
		ProtocolMessages peticion = new ProtocolMessages("todosPreparados");
		try {
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
			// esperar por la respuesta
			ProtocolMessages respuesta = com.waitReply();
			// procesar respuesta o excepción
			ret = (boolean) com.processReply(respuesta);
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en manoIniciada: " + e.getMessage() + "!");
		}

		return ret;		
	}
	
	private int turno() {
		int turno = -1;
		
		// crear mensaje a enviar
		ProtocolMessages peticion = new ProtocolMessages("turno");
		try {
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
			// esperar por la respuesta
			ProtocolMessages respuesta = com.waitReply();
			// procesar respuesta o excepción
			turno = (int) com.processReply(respuesta);
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en turno: " + e.getMessage() + "!");
		}

		return turno;
	}
	
	private void cogerFichas() {
		// crear mensaje a enviar
		ProtocolMessages peticion = new ProtocolMessages("cogerFichas");
		try {
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en cogerFichas: " + e.getMessage() + "!");
		}
	}
	
	private Ficha puntosAbiertos() {
		Ficha pa = null;
		do {
			try {
				// crear mensaje a enviar
				ProtocolMessages peticion = new ProtocolMessages("puntosAbiertos");
				// enviar mensaje de solicitud al servidor
				com.sendEvent(peticion);
				// esperar respuesta
				ProtocolMessages respuesta;
				respuesta = com.waitReply();
				pa = (Ficha)com.processReply(respuesta);
			} catch (Exception e) {
				this.UI.panelMesa.setTexto("¡Error en puntosAbiertos: " + e.getMessage() + "!");
			}
		} while (pa == null);
		
		return pa;
	}
	
	void ponerFicha(int numFicha, int numMitad) {
		Ficha elegida, pa;
		Iterator<Ficha> itr;
		int pos;
		
		// Comprueba si tenemos el turno
		if (this.turno() != this.orden) {
			// No tenemos el turno
			return;
		}
		
		// Obtiene la ficha elegida
		itr = this.UI.fichasJugador.getFichasJugador().iterator();
		pos = 0;
		do {
			elegida = itr.next();
			pos++;
		} while (pos <= numFicha);
		
		// Comprueba si se puede poner la ficha
		pa = this.puntosAbiertos();
		if (elegida.casan(pa)) {
			poner(elegida, pa, numMitad);
		} 
		else {
			this.UI.panelMesa.setTexto("¡No es posible poner esa ficha!");
		}
	}

	private void poner(Ficha ficha, Ficha pa, int numMitad) {
		int puntos = -1;

		// Comprueba si debe indicarse por qué mitad casar
		if (ficha.equals(pa) && ficha.puntosA() != ficha.puntosB()) {
			if (numMitad == 0) {
				puntos = ficha.puntosA();
			}
			else {
				puntos = ficha.puntosB();
			}
		}
		
		try {
			ProtocolMessages peticion;
			// crear mensaje a enviar
			if (puntos == -1) {
				peticion = new ProtocolMessages("ponerFicha", ficha);
			} else {
				peticion = new ProtocolMessages("ponerFichaPorPuntos", ficha, puntos);				
			}
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
			// esperar respuesta
			ProtocolMessages respuesta;
			respuesta = com.waitReply();
			com.processReply(respuesta);
		} catch (FichaNoValidaException e) {
			this.UI.panelMesa.setTexto("¡Ficha no válida! " + e.getMessage());
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en poner: " + e.getMessage() + "!");
		}
	}

	void pasar() {
		// Comprueba si tenemos el turno
		if (this.turno() != this.orden) {
			// No tenemos el turno
			return;
		}
		
		try {
			// crear mensaje a enviar
			ProtocolMessages peticion = new ProtocolMessages("pasar");
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en pasar: " + e.getMessage() + "!");
		}
	}

	void nuevaMano() {
		try {
			// crear mensaje a enviar
			ProtocolMessages peticion = new ProtocolMessages("siguienteMano");
			// enviar mensaje de solicitud al servidor
			com.sendEvent(peticion);
		} catch (Exception e) {
			this.UI.panelMesa.setTexto("¡Error en nuevaMano: " + e.getMessage() + "!");
			return;
		}
		this.UI.panelMesa.setTexto("Esperando al resto de jugadores...");
	}

	// METODO PRINCIPAL DEL CLIENTE
	// DONDE SE DESARROLLA TODA LA DINÁMICA DEL JUEGO
	private void run(String args[]) {
		boolean comenzar;
		int ficha_elegida, mitad_elegida; 
		boolean pasar_turno;

		// Espera a que la UI esté creada
		// (Necesario porque se accede a objetos de la UI y si aun no 
		// están creaados se producirá un error)
		synchronized(this.syncCreadoUI) {
			while (!this.creadoUI) {
				try {
					this.syncCreadoUI.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		// Si hay argumentos deben ser obligatoriamente estos:
		// <dir_ip> <alias>
		// Permite lanzar el cliente sin tener que rellenar a mano
		// esa información en la UI
		if (args.length > 0) {
			this.dirServer = args[0];
			this.alias = args[1];
		}
		
		// Espera a que el jugador se una a una partida
		// ¡OJO! Se puede conectar al servidor pero no necesariamente a la partida
		// En cuanto termina la partida actual se podrá unir (si el usuario le da a unirse a tiempo...)
		while (this.orden == -1) {
			// Abro un bloque synchronized para garantizar la exclusión mutua
			// de this.syncUnirsePartida y para realizar la sincronización (hacer el wait)
			synchronized (this.syncUnirsePartida) {				
				// Si hay argumentos se realiza una AUTOCONEXIÓN con el servidor
				if (args.length > 0) {
					this.unirsePartida = true;				
				}
	    		// ¿Han dado ya al botón de unirse o hay AUTOCONEXIÓN?
				// Solo debo hacer wait si el usuario no pulsó el botón en la UI (y no hay AUTOCONEXION)
				// Además usamos un bucle while por si el wait() lanza una excepción repetir el proceso
				while (!this.unirsePartida) {
					// No. Esperamos a que el usuario le de al botón
					try {
						this.syncUnirsePartida.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				// Hay AUTOCONEXIÓN o le dieron al botón.
				// Reseteamos la variable (para que no siga diciendo que el botón está pulsado)
				this.unirsePartida = false;
			} // Finaliza la sección crítica
			// Intento unirme a la partida. Puede fallar...			
			this.orden = this.unirse();
			
    		// Si hubo fallo al unirse y hay AUTOCONEXIÓN aplica una demora grande
			// (para que no esté reintentado constantemente)
			if (this.orden == -1 && args.length > 0 ) {
	    		try {
					Thread.sleep(ClienteGraficoDomino.DEMORA_AUTOCONEXION);
				} catch (InterruptedException e) {
				}
			}
		}

		// Inicializa elementos de la UI
		this.inicializarUI();
		
		// Espera a que el resto de jugadores se conecten
		do {
			comenzar = this.manoIniciada();
			this.mostrarJugadores();
    		try {
				Thread.sleep(ClienteGraficoDomino.DEMORA);
			} catch (InterruptedException e) {
			}
		} while (!comenzar);
		
		// Resetea el mensaje de la UI
		this.UI.panelMesa.setTexto("");
		
		// Bucle principal del juego
		this.turno = this.turno();
		while (this.turno != JuegoDomino.FINAL_PARTIDA) {
			// Esperamos a nuestro turno de coger fichas
			while (this.turno != this.orden) {
				// Mientras esperamos nuestro turno refrescamos
				// el UI (parte gráfica) para ver cómo van cogiendo fichas
				// el resto de jugadores
				this.actualizarUI(true);
				this.turno = this.turno();
				// Este bucle no tendrá demora para que se refresque muy rapido la UI
				// y se vean bien los cambios
			}
			
			// Coger fichas iniciales
			this.cogerFichas();
			// Refresca la UI para que se vea la acción realizada
			this.actualizarUI(true);
			
			// Esperamos a que todos los jugadores cojan sus fichas iniciales
			// y tengamos el turno
			this.turno = this.turno();
			while (this.turno != this.orden) {
				this.actualizarUI(true);
				this.turno = this.turno();
				// Este bucle no tendrá demora para que se refresque muy rapido la UI
				// y se vean bien los cambios
			}

			// Bucle para jugar una mano
			while (this.turno != JuegoDomino.FINAL_MANO && this.turno != JuegoDomino.FINAL_PARTIDA) {
				// Refrescamos la UI para que se vea el transcurso del juego
				this.actualizarUI(false);
				
				// Comprueba si es mi turno
				if (this.turno == this.orden) {
					// Abro un bloque synchronized para garantizar la exclusión mutua
					// de this.fichaElegida, this.mitadElegida y this.pasarTurno
					// y para realizar la sincronización (hacer el wait)
					synchronized (this.syncAccionTurno) {						
						// ¿Se ha elegido una ficha desde la UI o se ha pasado el turno?
						// Solo debo hacer wait si el usuario todavía no elegió la ficha en la UI
						// o no le dio al botón de pasar
						// Además usamos un bucle while por si el wait() lanza una excepción repetir el proceso
						while ((this.fichaElegida < 0 || this.mitadElegida < 0) && !this.pasarTurno) {
							// No. Espero a que el usuario indique lo que hacer en la UI
							try {
								this.syncAccionTurno.wait();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						// Guardamos la información en variables locales auxiliares para simplificar la exclusión mutua
						ficha_elegida = this.fichaElegida;
						mitad_elegida = this.mitadElegida;
						pasar_turno = this.pasarTurno;						
					} // Finaliza la sección crítica						
					// ¿Qué eligió: una ficha o pasar?
					if (ficha_elegida >= 0 && mitad_elegida >= 0) {
						// Se pone la ficha elegida
						this.ponerFicha(ficha_elegida, mitad_elegida);
					}
					else {
						// ¿He pasado turno desde la UI?
						if (pasar_turno) {
							this.pasar();
						}
					}
					// Actualizamos cuanto antes la variable turno para que la UI no
					// permita más acciones a este jugador
					this.turno = this.turno();
					
					// Refresca la UI para que se vea la acción realizada
					this.actualizarUI(true);
					
					// Reseteamos las acciones del usuario en la UI para que
					// no permanezcan para el siguiente turno.
					// (Importante hacerlo lo más tarde posible para que queden bien reseteadas)
					// Abro un bloque synchronized para garantizar la exclusión mutua
					// de this.fichaElegida, this.mitadElegida y this.pasarTurno
					synchronized (this.syncAccionTurno) {
						this.fichaElegida = -1;
						this.mitadElegida = -1;
						this.pasarTurno = false;
					}					
				} // fin de mi turno
				// Demora para no cargar la CPU
				try {
					Thread.sleep(ClienteGraficoDomino.DEMORA);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Obtenemos el turno actual
				this.turno = this.turno();
			}
			
			// HA FINALIZADO UNA MANO
			// Refresca el UI para que refleje bien la situación final
			this.actualizarUI(true);
			
			// Oculta el botón de PASAR (en la UI)
			this.UI.btnPasar.setVisible(false);
			
			
			// En este punto ha terminado la mano y, a lo mejor, la partida
			// Se enseñan las fichas que tiene cada jugador y los puntos
			// obtenidos por cada uno. 
			// Además se actualizan los puntajes totales
			informeDePuntos();
			
			// Si no ha terminado la partida se espera a que todos
			// estén preparados para la siguiente mano
			if (this.turno == JuegoDomino.FINAL_MANO) {
				// Espero a que el usuario pulse el botón de siguiente mano
				// Muestra el botón de siguiente mano (en la UI)
				this.UI.btnSiguienteMano.setVisible(true);
				// Abro un bloque synchronized para garantizar la exclusión mutua
				// de this.siguienteMano y para realizar la sincronización (hacer el wait)
				synchronized (this.syncSiguienteMano) {					
					// Sólo debo hacer wait() si el usuario no pulsó todavía el botón
					// Además usamos un bucle while por si el wait() lanza una excepción repetir el proceso
					while (!this.siguienteMano) {
						try {
							this.syncSiguienteMano.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					// Se oculta el botón de la UI
					this.UI.btnSiguienteMano.setVisible(false);
					// Reseteamos la variable (para que no siga diciendo que el botón está pulsado)
					this.siguienteMano = false;
				} // Fin de la sección crítica
				this.nuevaMano();
				
				// Bucle de espera a que todos los jugadores
				// estén preparados para jugar la siguiente mano (si no acabó la partida)
				// Durante este bucle no se actualiza la UI para que se pueda
				// ver el informe de puntos del fin de la mano con tranquilidad
				if (this.turno == JuegoDomino.FINAL_MANO) {
					do {
						comenzar = this.manoIniciada();
						// Demora para no cargar la CPU
			    		try {
							Thread.sleep(ClienteGraficoDomino.DEMORA);
						} catch (InterruptedException e) {
						}
					} while (!comenzar);
					// Restituye la información habitual durante el juego en la UI
					this.restaurarUI();
				}
			}
		}
		
		// Finalizó la partida. Indicamos quién ha sido el glorioso ganador en la UI
		this.muestraGanador();
		
		// cierra el canal de comunicación y
		// desconecta el cliente
		this.com.disconnect();
	}

	public static void main(String[] args) {
		ClienteGraficoDomino cliente = new ClienteGraficoDomino();
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					// La UI corre en un hilo aparte para
					// no ralentizarse ni ralentizar el cliente
					// Recibe el objeto OOS para que ambos
					// se puedan comunicar
					UIDomino window = new UIDomino(cliente);
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
		});
		
		// Método principal del cliente donde
		// se desarrolla toda la gestión del juego
		cliente.run(args);
	}
	
	/**************************************************************
	 * 
	 *  Los métodos privados a continuación son para mantener el UI
	 *  (parte gráfica) 
	 *  NO MODIFICAR A PARTIR DE AQUÍ ---> NO ENTRA PARA EL EXAMEN
	 *  
	 ***************************************************************/
	// Método interno que inicializa la UI
	// y campos necesarios para la comunicación con la UI
	private void inicializarUI() {
		int pos;
		
		// Oculta los controles de la UI que ya no son necesarios
		this.UI.lblServidor.setVisible(false);
		this.UI.textFieldServidor.setVisible(false);
		this.UI.btnUnirse.setVisible(false);
		this.UI.lblAlias.setVisible(false);
		this.UI.textFieldAlias.setVisible(false);
		
		// Muestra el botón de PASAR de la UI
		this.UI.btnPasar.setVisible(true);
		
		// Añade en las listas de UIs a ESTE jugador
		pos = this.orden;
		this.infoJugadores.set(pos, this.UI.infoJugador);
		this.infoFichasJugadores.set(pos, this.UI.fichasJugador);
	}
	
	// Método interno que actualiza la UI durante una mano
	// Se llama cada cierto tiempo
	private void actualizarUI(boolean forzarActualizacion) {
		// Actualiza la información del turno para la UI
		synchronized (this.mutexTurnoUI) {
			this.turnoUI = this.turno;
		}
		
		// ¿Hay que borrar un mensaje?
		if (this.UI.panelMesa.getTexto() != null && !this.UI.panelMesa.getTexto().equals("")) {
			if (this.UI.panelMesa.getCuentaAtras() > 0) {
				this.UI.panelMesa.decrementaCuentaAtras(); 
			}
			else {
				this.UI.panelMesa.setTexto("");
			}
		}
		
		// El resto de acciones que faltan solo se hacen si ha cambiado el turno
		// o si se está forzando la actualización de la UI
		if (this.turno == this.turnoAnterior && !forzarActualizacion) {
			return;
		}
		else {
			this.turnoAnterior = this.turno;
		}
		
		// Actualiza las fichas de cada jugador
		this.mostrarNumeroFichasJugadores();
		this.mostrarMisFichas();
		for (int i=0; i<this.infoJugadores.size(); i++) {
			if (this.infoJugadores.get(i) != null) {
				this.infoJugadores.get(i).repaint();
				this.infoFichasJugadores.get(i).repaint();
			}
		}
		
		// Actualiza las fichas en la mesa
		this.mostrarFichasJugadas();
		this.UI.panelMesa.repaint();
		
		// Actualiza quien tiene el turno
		for (int i=0; i<this.infoJugadores.size(); i++) {
			if (i == this.turno) {
				this.infoJugadores.get(i).setTexto2("TIENE EL TURNO");
			}
			else {
				if (this.infoJugadores.get(i) != null) {
					this.infoJugadores.get(i).setTexto2("");
				}
			}
		}		
	}

	// Método interno para actualizar los jugadores en la UI
	private void actualizaJugadoresUI() {
		int pos;
		int nJugadores;
		
		// Obtiene el número actual de jugadores
		nJugadores = this.jugadores.size();
		
		// Inicializa las listas de UIs que permiten 
		// mapear jugadores a elementos de la UI que muestran información
		pos = this.orden;
		pos = (pos + 1) % nJugadores;
		switch (nJugadores) {
			case 2:
				// Dos jugadores
				this.infoJugadores.set(pos, this.UI.infoJugadorNorte);
				this.infoFichasJugadores.set(pos, this.UI.fichasJugadorNorte);
				break;
			case 3:
				// Tres jugadores
				this.infoJugadores.set(pos, this.UI.infoJugadorEste);
				this.infoFichasJugadores.set(pos, this.UI.fichasJugadorEste);
				pos = (pos + 1) % nJugadores;
				this.infoJugadores.set(pos, this.UI.infoJugadorNorte);
				this.infoFichasJugadores.set(pos, this.UI.fichasJugadorNorte);
				break;
			case 4:
				// Cuatro jugadores
				this.infoJugadores.set(pos, this.UI.infoJugadorEste);
				this.infoFichasJugadores.set(pos, this.UI.fichasJugadorEste);
				pos = (pos + 1) % nJugadores;
				this.infoJugadores.set(pos, this.UI.infoJugadorNorte);
				this.infoFichasJugadores.set(pos, this.UI.fichasJugadorNorte);
				pos = (pos + 1) % nJugadores;
				this.infoJugadores.set(pos, this.UI.infoJugadorOeste);
				this.infoFichasJugadores.set(pos, this.UI.fichasJugadorOeste);
				break;
		}
		
		// Muestra los nombres de los jugadores en la UI+
		for (int i=0; i<this.jugadores.size(); i++) {
			this.infoJugadores.get(i).setTexto1(this.jugadores.get(i) + " (" + this.puntuaciones.get(i) +  " puntos)");
		}		
	}

	// Método interno para actualizar las fichas de los contrincantes en la UI
	private void actualizaFichasUI(List<Integer> numFichas) {
		// Solo se pueden ver el número de fichas de los contrincantes
		for (int i=0; i<numFichas.size(); i++) {
			if (i != this.orden) {
				this.infoFichasJugadores.get(i).setNumeroFichasJugador(numFichas.get(i));
			}
		}		
	}
	
	// Método interno para actualizar las fichas colocadas en la mesa (en la UI)
	private void actualizaMesaUI(FichasColocadas colocadas) {
		if (colocadas != null && !colocadas.isEmpty()) {
			this.UI.panelMesa.setFichaInicial(colocadas.first());
			this.UI.panelMesa.setParteIzquierda(colocadas.left());
			this.UI.panelMesa.setParteDerecha(colocadas.right());			
		}
		else {
			this.UI.panelMesa.setFichaInicial(null);
			this.UI.panelMesa.setParteIzquierda(null);
			this.UI.panelMesa.setParteDerecha(null);
		}		
	}
	
	// Método interno para mostrar el ganador de la mano en la UI
	private void actualizaGanadorManoUI(List<Integer> puntosMano) {
		int i;
		
		if (puntosMano != null) {			
			for (i=0; i<puntosMano.size(); i++) {
				if (puntosMano.get(i) > 0) {
					this.UI.panelMesa.setTexto("¡¡" + this.jugadores.get(i) + " GANÓ ESTA MANO Y OBTIENE " + puntosMano.get(i) + " PUNTOS!!");
				}
			}
		}
	}

	// Método interno para mostrar las fichas de todos los jugadores cuando acaba una mano
	private void actualizaFichasJugadoresUI(List<List<Ficha>> fichasJugadores) {
		int i;
		int suma;
		
		if (fichasJugadores != null) {
			for (i=0; i<fichasJugadores.size(); i++) {
				this.infoFichasJugadores.get(i).setFichasJugador(fichasJugadores.get(i));
				this.infoFichasJugadores.get(i).setOculto(false);
				this.infoFichasJugadores.get(i).repaint();
				suma = fichasJugadores.get(i).stream().mapToInt(f -> f.puntosFicha()).sum();
				this.infoJugadores.get(i).setTexto1(this.jugadores.get(i) + " (" + this.puntuaciones.get(i) +  " puntos)");
				this.infoJugadores.get(i).setTexto2("Tus fichas no jugadas suman " + suma + " puntos");
			}			
		}
		
	}

	// Método interno que restituye la información de la UI 
	// después de haberse mostrado un informe detallado de puntos al final de una mano
	private void restaurarUI() {		
		// Oculta el botón de siguiente mano y muestra el de PASAR
		this.UI.btnSiguienteMano.setVisible(false);
		this.UI.btnPasar.setVisible(true);	
		
		// Restituye la información de los jugadores
		for (int i=0; i<this.infoFichasJugadores.size(); i++) {
			if (this.orden != i) {
				this.infoFichasJugadores.get(i).setOculto(true);
			}
			this.infoFichasJugadores.get(i).setFichasJugador(null);
			this.infoFichasJugadores.get(i).setNumeroFichasJugador(0);			
			this.infoFichasJugadores.get(i).repaint();
			this.infoJugadores.get(i).setTexto2("");
		}
		
		// Actualiza las fichas en la mesa
		this.mostrarFichasJugadas();
		this.UI.panelMesa.repaint();
	}
	
	// Metodo interno que muestra en la UI el ganador de todo el juego
	private void muestraGanador() {
		for (int i=0; i<this.puntuaciones.size(); i++) {
			if (this.puntuaciones.get(i) > JuegoDomino.PUNTOS_DE_PARTIDA) {
				this.UI.panelMesa.setTexto("¡¡" + this.jugadores.get(i) + " ES EL FLAMANTE GANADOR MUNDIAL CON " + this.puntuaciones.get(i) + " PUNTOS!!");
				this.infoJugadores.get(i).setTexto2("¡GANADOR! ¡GANADOR! ¡GANADOR! ¡GANADOR!");
			}
		}
	}
	
	
}
