package com.example.trainly.Fragmentos.MenuLateral.Buzon;

// MessageAdapter.java
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trainly.R;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    public interface OnRespondListener {
        void onRespond(MessageSimple message);
    }
    public interface OnDeleteListener {
        void onDelete(MessageSimple message);
    }

    private final List<MessageSimple> messages;
    private final boolean incoming;
    private final OnRespondListener respondListener;
    private final OnDeleteListener deleteListener;

    /**
     * @param messages         lista de mensajes a mostrar
     * @param incoming         true si son mensajes recibidos; false si son enviados
     * @param respondListener  listener para "Responder" (solo si incoming=true)
     * @param deleteListener   listener para "Borrar" (solo si incoming=false)
     */
    public MessageAdapter(List<MessageSimple> messages,
                          boolean incoming,
                          OnRespondListener respondListener,
                          OnDeleteListener deleteListener) {
        this.messages         = messages;
        this.incoming         = incoming;
        this.respondListener  = respondListener;
        this.deleteListener   = deleteListener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos un layout común: item_message.xml
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mensaje, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        MessageSimple m = messages.get(pos);
        if (incoming) {
            h.tvUser.setText(m.getUsuarioRemitente());
            h.tvLabel.setText("De:");
            h.btnAction.setText("Responder");
            h.btnAction.setOnClickListener(v -> respondListener.onRespond(m));
        } else {
            h.tvUser.setText(m.getUsuarioReceptor());
            h.tvLabel.setText("Para:");
            h.btnAction.setText("Borrar");
            h.btnAction.setOnClickListener(v -> deleteListener.onDelete(m));
        }
        h.tvMensaje.setText(m.getMensaje());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLabel;    // "De:" o "Para:"
        TextView tvUser;     // nombre de usuario
        TextView tvMensaje;  // contenido
        Button   btnAction;  // "Responder" o "Borrar"

        VH(View itemView) {
            super(itemView);
            tvLabel   = itemView.findViewById(R.id.tvMessageLabel);
            tvUser    = itemView.findViewById(R.id.tvMessageUser);
            tvMensaje = itemView.findViewById(R.id.tvMessageContent);
            btnAction = itemView.findViewById(R.id.btnMessageAction);
        }
    }
}

