package com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.trainly.R;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class InformacionEjercicio extends DialogFragment {

    public interface OnVideoDialogListener {
        void onGuardarVideo(String enlace);

        void onCancelarVideo();
    }

    private OnVideoDialogListener listener;

    private String nombreEjercicio = "";
    private String descripcion = "";
    private String enlaceSugerido = "";
    private String enlacePredeterminado = "";

    private boolean realizandoEjercicio = false;

    public void setNombreEjercicio(String nombreEjercicio) {
        this.nombreEjercicio = nombreEjercicio;
    }

    public void setRealizandoEjercicio(boolean realizando) {
        this.realizandoEjercicio = realizando;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = (descripcion != null) ? descripcion : "";
    }


    public void setEnlaceExistente(String enlaceSugerido) {
        this.enlaceSugerido = enlaceSugerido;
    }

    public void setEnlacePredeterminado(String enlacePredeterminado) {
        this.enlacePredeterminado = enlacePredeterminado;
    }

    public void setOnVideoDialogListener(OnVideoDialogListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_informacion_ejercicio, container, false);

        TextView tvTitulo = view.findViewById(R.id.tvTituloEjercicio);
        TextView tvDescripcion = view.findViewById(R.id.tvDescripcionEjercicio);
        TextView tituloSugerido = view.findViewById(R.id.tituloSugerido);
        EditText etEnlace = view.findViewById(R.id.etEnlaceEjercicio);
        Button btnGuardar = view.findViewById(R.id.btnGuardarEnlace);
        Button btnSalir = view.findViewById(R.id.btnSalirEnlace);
        YouTubePlayerView youtubePlayerView = view.findViewById(R.id.youtubePlayerView2);

        // ✅ Mostrar título y descripción
        tvTitulo.setText(nombreEjercicio);
        tvDescripcion.setText(!TextUtils.isEmpty(descripcion) ? descripcion : "Descripción no disponible.");

        // ✅ Solo mostrar el enlace sugerido si no está vacío
        if (!TextUtils.isEmpty(descripcion)) {
            tvDescripcion.setText(descripcion);
        } else {
            tvDescripcion.setText("Descripción no disponible");
        }


        TextView tvEnlace = view.findViewById(R.id.etEnlaceEjercicio);
        if (realizandoEjercicio) {
            etEnlace.setVisibility(View.GONE);
            tvEnlace.setVisibility(View.VISIBLE);
            tvEnlace.setText("Video Por Defecto");
            tvEnlace.setTextSize(20);
            tvEnlace.setTypeface(null, android.graphics.Typeface.BOLD);
            btnGuardar.setVisibility(View.INVISIBLE);
        }


        if (!TextUtils.isEmpty(enlaceSugerido)) {
            etEnlace.setText(enlaceSugerido);
        } else {
            etEnlace.setText(""); // Limpio por defecto
        }


        String videoId = "";

        boolean videoEntrarSugerido=false;
        if (!TextUtils.isEmpty(enlacePredeterminado)) {
            videoId = extraerVideoId(enlacePredeterminado);
        }

        if (!TextUtils.isEmpty(enlaceSugerido)) {
            videoId = extraerVideoId(enlaceSugerido);
            videoEntrarSugerido=true;
        }

        if (realizandoEjercicio==true){
            btnSalir.setBackgroundColor(getResources().getColor(android.R.color.black));
            btnGuardar.setVisibility(View.GONE);
            etEnlace.setVisibility(View.GONE);
            if (videoEntrarSugerido==true){
                tituloSugerido.setText("Video Sugerido");
                tituloSugerido.setVisibility(View.VISIBLE);
            }else if(!enlacePredeterminado.isEmpty()){
                tituloSugerido.setText("Video Predeterminado");
                tituloSugerido.setVisibility(View.VISIBLE);
            }
        }


        // ✅ Reproducir el video predeterminado

        getLifecycle().addObserver(youtubePlayerView);

        Log.d("YouTubeDebug", "Video ID extraído: " + videoId);

        if (videoId != null) {
            youtubePlayerView.setVisibility(View.VISIBLE);
            String finalVideoId = videoId;
            youtubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    youTubePlayer.loadVideo(finalVideoId, 0);
                }
            });
        } else {
            youtubePlayerView.setVisibility(View.GONE);
        }


        // ✅ Guardar enlace sugerido si es válido
        btnGuardar.setOnClickListener(v -> {
            String nuevoEnlace = etEnlace.getText().toString().trim();

            if (TextUtils.isEmpty(nuevoEnlace)) {
                Toast.makeText(getContext(), "El enlace no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!esEnlaceYoutubeValido(nuevoEnlace)) {
                Toast.makeText(getContext(), "El enlace no es un video de YouTube válido", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listener != null) listener.onGuardarVideo(nuevoEnlace);
            dismiss();
        });

        btnSalir.setOnClickListener(v -> {
            if (listener != null) listener.onCancelarVideo();
            dismiss();
        });

        return view;
    }

    private boolean esEnlaceYoutubeValido(String url) {
        return url.matches("^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.+$");
    }

    private String extraerVideoId(String url) {
        try {
            if (url.contains("youtu.be/")) {
                return url.substring(url.lastIndexOf("/") + 1).split("\\?")[0];
            } else if (url.contains("youtube.com/watch")) {
                String[] split = url.split("\\?");
                if (split.length > 1) {
                    String[] params = split[1].split("&");
                    for (String param : params) {
                        if (param.startsWith("v=")) {
                            return param.substring(2);
                        }
                    }
                }
            } else if (url.contains("/shorts/")) {
                return url.substring(url.lastIndexOf("/") + 1).split("\\?")[0];
            }
        } catch (Exception e) {
            Log.e("YouTubeDebug", "Error al extraer video ID: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }
}
