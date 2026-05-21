package com.example.trainly.Fragmentos.MenuLateral.Seguimiento;

// UsuarioAdapter.java

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.trainly.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

// UsuarioAdapter.java

public class UsuarioAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_ADD  = 1;

    public interface OnUserClickListener {
        void onClick(UsuarioSimple usuario);
    }
    public interface OnAddClickListener {
        void onClick();
    }

    private final List<UsuarioSimple> lista;
    private final OnUserClickListener userListener;
    private final OnAddClickListener   addListener;

    public UsuarioAdapter(List<UsuarioSimple> lista,
                          OnUserClickListener userListener,
                          OnAddClickListener addListener) {
        this.lista        = lista;
        this.userListener = userListener;
        this.addListener  = addListener;
    }

    @Override
    public int getItemCount() {
        // siempre mostramos todos los usuarios + 1 ítem de "añadir"
        return lista.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        // si la posición es la última, es el ítem de "añadir"
        if (position == lista.size()) {
            return TYPE_ADD;
        } else {
            return TYPE_USER;
        }
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            View v = inf.inflate(R.layout.item_usuario_add, parent, false);
            return new AddVH(v);
        } else {
            View v = inf.inflate(R.layout.item_usuario, parent, false);
            return new UserVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        if (getItemViewType(pos) == TYPE_ADD) {
            ((AddVH) holder).btnAdd.setOnClickListener(v -> addListener.onClick());
        } else {
            UsuarioSimple u = lista.get(pos);
            UserVH vh = (UserVH) holder;
            vh.tvNombre.setText(u.getNombre());

            // 1) Carga por defecto mientras consultas Firestore
            Glide.with(vh.itemView)
                    .load(R.drawable.untitled)
                    .circleCrop()
                    .into(vh.btnFoto);

            // 2) Consulta Firestore por el usuario u.getNombre()
            FirebaseFirestore.getInstance()
                    .collection("Usuarios")
                    .whereEqualTo("nombreUsuario", u.getNombre())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            String fotoBase64 = snap.getDocuments()
                                    .get(0)
                                    .getString("fotoPerfil");
                            if (fotoBase64 != null && !fotoBase64.isEmpty()) {
                                // Decodificar Base64 a Bitmap
                                byte[] decoded = Base64.decode(fotoBase64, Base64.DEFAULT);
                                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                // 3) Sobrescribir la imagen en el ImageButton
                                Glide.with(vh.itemView)
                                        .load(bmp)
                                        .circleCrop()
                                        .into(vh.btnFoto);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // opcional: log de error
                    });

            vh.btnFoto.setOnClickListener(v -> userListener.onClick(u));
        }
    }


    // ViewHolder para el usuario
    static class UserVH extends RecyclerView.ViewHolder {
        ImageButton btnFoto;
        TextView    tvNombre;
        UserVH(View itemView) {
            super(itemView);
            btnFoto  = itemView.findViewById(R.id.btnFotoUsuario);
            tvNombre = itemView.findViewById(R.id.tvNombreUsuario);
            tvNombre.setTextColor(Color.WHITE);
        }
    }

    // ViewHolder para el botón "Añadir usuario"
    static class AddVH extends RecyclerView.ViewHolder {
        ImageButton btnAdd;
        AddVH(View itemView) {
            super(itemView);
            btnAdd = itemView.findViewById(R.id.btnAddUser);
        }
    }
}
