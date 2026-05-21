package com.example.trainly.Fragmentos.MenuLateral.Buzon;

// MessageSimple.java
public class MessageSimple {
    private final String id;
    private final String usuarioRemitente;
    private final String usuarioReceptor;
    private final String mensaje;

    public MessageSimple(String id, String usuarioRemitente, String usuarioReceptor, String mensaje) {
        this.id = id;
        this.usuarioRemitente = usuarioRemitente;
        this.usuarioReceptor  = usuarioReceptor;
        this.mensaje          = mensaje;
    }

    public String getId() { return id; }
    public String getUsuarioRemitente() { return usuarioRemitente; }
    public String getUsuarioReceptor()  { return usuarioReceptor; }
    public String getMensaje()         { return mensaje; }
}

