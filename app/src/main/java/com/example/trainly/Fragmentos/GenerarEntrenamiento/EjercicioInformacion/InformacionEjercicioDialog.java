package com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.trainly.R;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class InformacionEjercicioDialog extends DialogFragment {

    private static final String ARG_NOMBRE = "nombre";
    private static final String ARG_DESCRIPCION = "descripcion";
    private static final String ARG_VIDEO_URL = "video";

    public static InformacionEjercicioDialog nuevaInstancia(String nombre, String descripcion) {
        return nuevaInstancia(nombre, descripcion, null);
    }

    public static InformacionEjercicioDialog nuevaInstancia(String nombre, String descripcion, @Nullable String enlaceVideo) {
        InformacionEjercicioDialog fragment = new InformacionEjercicioDialog();
        Bundle args = new Bundle();
        args.putString(ARG_NOMBRE, nombre);
        args.putString(ARG_DESCRIPCION, descripcion);
        args.putString(ARG_VIDEO_URL, enlaceVideo);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_informacion_ejercicio, container, false);

        String nombre = getArguments().getString(ARG_NOMBRE);
        String descripcion = getArguments().getString(ARG_DESCRIPCION);
        String videoUrl = getArguments().getString(ARG_VIDEO_URL);

        TextView tvNombre = view.findViewById(R.id.tvNombreEjercicio);
        TextView tvDescripcion = view.findViewById(R.id.tvDescripcionEjercicio);
        YouTubePlayerView youTubePlayerView = view.findViewById(R.id.youtubePlayerView);

        tvNombre.setText(nombre);
        tvDescripcion.setText(descripcion);

        if (videoUrl != null && !videoUrl.isEmpty()) {
            getLifecycle().addObserver(youTubePlayerView);
            String videoId = extraerVideoId(videoUrl);
            if (videoId != null) {
                youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                    @Override
                    public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                        youTubePlayer.loadVideo(videoId, 0);
                    }
                });
            }
        } else {
            youTubePlayerView.setVisibility(View.GONE);
        }

        return view;
    }

    private String extraerVideoId(String url) {
        try {
            if (url.contains("youtu.be/")) {
                return url.substring(url.lastIndexOf("/") + 1);
            } else if (url.contains("v=")) {
                String[] parts = url.split("v=");
                String idPart = parts[1];
                int ampIndex = idPart.indexOf("&");
                return ampIndex != -1 ? idPart.substring(0, ampIndex) : idPart;
            } else if (url.contains("/shorts/")) {
                return url.substring(url.lastIndexOf("/") + 1).split("\\?")[0];
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
