package com.dedsafio3.client;

/** Estado del Momento Reviil en el cliente: mientras dura, no se dibuja la barra de vida. */
public final class ReviilClientState {
	public static volatile boolean hideHealth;

	private ReviilClientState() {
	}
}
