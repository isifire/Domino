package servidor;

import java.io.IOException;

import comun.Ficha;
import lib.ChannelException;
import lib.CommServer;
import optional.Trace;

public class Servidor {

	private static void registrarOperaciones(CommServer com) {
		com.addFunction("unirsePartida",
				(o, x) -> ((Servicio)o).unirsePartida((String)x[0]));
		com.addFunction("todosPreparados",
				(o, x) -> ((Servicio)o).todosPreparados());
		com.addFunction("listaJugadores",
				(o, x) -> ((Servicio)o).listaJugadores());
		com.addAction("cogerFichas",
				(o, x) -> ((Servicio)o).cogerFichas(), true);
		com.addFunction("turno",
				(o, x) -> ((Servicio)o).turno());
		com.addFunction("fichasColocadas",
				(o, x) -> ((Servicio)o).fichasColocadas());
		com.addFunction("puntosAbiertos",
				(o, x) -> ((Servicio)o).puntosAbiertos());
		com.addAction("ponerFicha",
				(o, x) -> ((Servicio)o).ponerFicha((Ficha)x[0]));
		com.addAction("ponerFichaPorPuntos",
				(o, x) -> ((Servicio)o).ponerFicha((Ficha)x[0], (int)x[1]));
		com.addAction("pasar",
				(o, x) -> ((Servicio)o).pasar(), true);
		com.addFunction("misFichas",
				(o, x) -> ((Servicio)o).misFichas());
		com.addFunction("numeroFichasJugadores",
				(o, x) -> ((Servicio)o).numeroFichasJugadores());
		com.addFunction("fichasJugadores",
				(o, x) -> ((Servicio)o).fichasJugadores());
		com.addFunction("jugadoresUnidos",
				(o, x) -> ((Servicio)o).jugadoresUnidos());
		com.addAction("siguienteMano",
				(o, x) -> ((Servicio)o).siguienteMano(), true);
		com.addFunction("informePuntos",
				(o, x) -> ((Servicio)o).informePuntos());
	}
	
	public static void main(String[] args) {
		CommServer com;	// canal de comunicación del servidor
		int idCliente;	// identificador del cliente
		
		try {
			// crear el canal de comunicación del servidor
			com = new CommServer();
			
			// activar la traza en el servidor (opcional)
			Trace.activateTrace(com);
			
			// activar el registro de mensajes del servidor (opcional)
			com.activateMessageLog();
			
			// registrar operaciones del servicio
			registrarOperaciones(com);
								
			// ofrecer el servicio (queda a la escucha)
			while (true) {
				// espera por un cliente
				idCliente = com.waitForClient();			
				
				// conversación con el cliente en un hilo
				Trace.printf("-- Creando hilo para el cliente %d.\n",
						idCliente);
				new Thread(new Hilos(com, idCliente)).start();
				Trace.printf("-- Creado hilo para el cliente %d.\n",
						idCliente);
			}
		} catch (IOException | ChannelException e) {
			System.err.printf("Error: %s\n", e.getMessage());
			e.printStackTrace();
		}
		
	} // main

}
