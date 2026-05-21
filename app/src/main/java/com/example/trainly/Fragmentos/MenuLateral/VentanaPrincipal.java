package com.example.trainly.Fragmentos.MenuLateral;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.trainly.Actividades.MainActivity;
import com.example.trainly.Fragmentos.VentanaEstadisticas;
import com.example.trainly.Objeto.Usuario.MailSender;
import com.example.trainly.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VentanaPrincipal#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VentanaPrincipal extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public VentanaPrincipal() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment VentanaPrincipal.
     */
    // TODO: Rename and change types and number of parameters
    public static VentanaPrincipal newInstance(String param1, String param2) {
        VentanaPrincipal fragment = new VentanaPrincipal();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }


    private Button calendario;
    private Button estadisticas;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ventana_principal, container, false);


        calendario = view.findViewById(R.id.botonCalendario);
        estadisticas = view.findViewById(R.id.botonVentanaEstadisticas);



        calendario.setOnClickListener(v -> {
            FragmentTransaction transaction = requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction();

// ✨ Animación más llamativa con zoom + fade
            transaction.setCustomAnimations(
                    R.anim.fragment_zoom_in,  // Entrada
                    R.anim.fragment_zoom_out, // Salida
                    R.anim.fragment_zoom_in,  // Entrada inversa (al volver)
                    R.anim.fragment_zoom_out  // Salida inversa (al volver)
            );

            transaction.replace(R.id.fragmentoVacio, new VentanaCalendario());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        estadisticas.setOnClickListener(v -> {
            FragmentTransaction transaction = requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction();

// ✨ Animación más llamativa con zoom + fade
            transaction.setCustomAnimations(
                    R.anim.fragment_zoom_in,  // Entrada
                    R.anim.fragment_zoom_out, // Salida
                    R.anim.fragment_zoom_in,  // Entrada inversa (al volver)
                    R.anim.fragment_zoom_out  // Salida inversa (al volver)
            );

            transaction.replace(R.id.fragmentoVacio, new VentanaEstadisticas());
            transaction.addToBackStack(null);
            transaction.commit();

        });






        // Inflate the layout for this fragment
        return view;


    }

}