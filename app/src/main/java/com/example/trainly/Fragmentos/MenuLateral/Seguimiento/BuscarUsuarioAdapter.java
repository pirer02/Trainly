package com.example.trainly.Fragmentos.MenuLateral.Seguimiento;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.trainly.R;

import java.util.List;

public class BuscarUsuarioAdapter
        extends RecyclerView.Adapter<BuscarUsuarioAdapter.VH> {

    public interface OnEnviarClick { void onEnviar(UsuarioSimple u); }
    private final List<UsuarioSimple> lista;
    private final OnEnviarClick listener;

    public BuscarUsuarioAdapter(List<UsuarioSimple> lista, OnEnviarClick l) {
        this.lista = lista;
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_buscar_usuario, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        UsuarioSimple u = lista.get(i);
        h.tvNombre.setText(u.getNombre());
        // Carga foto (puedes usar Glide)
        Glide.with(h.itemView)
                .load(u.getFotoUrl()!=null ? Base64.decode(u.getFotoUrl(), Base64.DEFAULT) : R.drawable.untitled)
                .circleCrop()
                .into(h.imgFoto);

        h.btnEnviar.setOnClickListener(v -> listener.onEnviar(u));
    }
    @Override public int getItemCount() { return lista.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgFoto;
        TextView tvNombre;
        Button btnEnviar;
        VH(View v) {
            super(v);
            imgFoto = v.findViewById(R.id.imgFoto);
            tvNombre = v.findViewById(R.id.tvNombreUsuario);
            btnEnviar = v.findViewById(R.id.btnEnviar);
        }
    }
}
