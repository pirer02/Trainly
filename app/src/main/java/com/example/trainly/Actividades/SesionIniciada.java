package com.example.trainly.Actividades;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.trainly.Fragmentos.MenuLateral.Buzon.BuzonFragment;
import com.example.trainly.Fragmentos.MenuLateral.SeguimientoFragment;
import com.example.trainly.Fragmentos.MenuLateral.Ajustes.VentanaAjustes;
import com.example.trainly.Fragmentos.MenuLateral.VentanaCalendario;
import com.example.trainly.Fragmentos.MenuLateral.VentanaPrincipal;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

public class SesionIniciada extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private static TextView usuarioMenu;
    private static TextView gmailMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sesion_iniciada);

        Usuario usuario = Usuario.getInstancia();

        // Header del NavigationView
        NavigationView navigationView2 = findViewById(R.id.navigation_view);
        View headerView = navigationView2.getHeaderView(0);
        usuarioMenu = headerView.findViewById(R.id.usuarioMenu);
        usuarioMenu.setText(usuario.getNombreUsuario());
        gmailMenu = headerView.findViewById(R.id.correoMenu);
        gmailMenu.setText(usuario.getGmail());

        ImageView imagenPerfilMenu = headerView.findViewById(R.id.imageView);

        // — Ajuste de tamaño cuadrado para un círculo completo —
        int tamañoDp = 80;
        float density = getResources().getDisplayMetrics().density;
        int tamañoPx = Math.round(tamañoDp * density);
        ViewGroup.LayoutParams params = imagenPerfilMenu.getLayoutParams();
        params.width  = tamañoPx;
        params.height = tamañoPx;
        imagenPerfilMenu.setLayoutParams(params);

        // Escala la imagen entera dentro del view, sin recortar bordes
        imagenPerfilMenu.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        // ——————————————————————————————————————————

        // Opciones de Glide: primero centerInside para que la imagen entera quepa,
        // luego circleCrop para enmascarar en un círculo
        RequestOptions opciones = new RequestOptions()
                .centerInside()
                .circleCrop();

        // Carga de foto de perfil desde Firestore
        String userDocId = usuario.getIdUsuario();
        FirebaseFirestore.getInstance()
                .collection("Usuarios")
                .document(userDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String base64 = doc.getString("fotoPerfil");
                        if (base64 != null && !base64.isEmpty()) {
                            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            Glide.with(this)
                                    .load(bitmap)
                                    .apply(opciones)
                                    .into(imagenPerfilMenu);
                        } else {
                            Glide.with(this)
                                    .load(R.drawable.untitled)
                                    .apply(opciones)
                                    .into(imagenPerfilMenu);
                        }
                    } else {
                        Glide.with(this)
                                .load(R.drawable.untitled)
                                .apply(opciones)
                                .into(imagenPerfilMenu);
                    }
                })
                .addOnFailureListener(e ->
                        Glide.with(this)
                                .load(R.drawable.untitled)
                                .apply(opciones)
                                .into(imagenPerfilMenu)
                );

        // Configurar DrawerLayout y Toggle
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(item -> {
            if (SeguimientoFragment.globalPrevUser != null) {
                SeguimientoFragment.globalPrevUser.restore();
                SeguimientoFragment.globalPrevUser = null;
            }

            int id = item.getItemId();
            if (id == R.id.nav_home) {
                cargarFragmentoInicio(new VentanaPrincipal());
            }
            if (id == R.id.nav_seguimiento) {
                cargarFragmentoInicio(new SeguimientoFragment());
            }
            if (id == R.id.nav_buzon) {
                cargarFragmentoInicio(new BuzonFragment());
            }
            if (id == R.id.nav_slideshow) {
                cargarFragmentoInicio(new VentanaAjustes());
            }
            if (id == R.id.nav_logout) {
                Usuario.resetInstance();
                Intent intent = new Intent(SesionIniciada.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Fragmento principal por defecto
        cargarFragmentoInicio(new VentanaPrincipal());
    }

    private void cargarFragmentoInicio(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragmentoVacio, fragment);
        ft.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentoVacio);

        // 1) Si estamos en el calendario...
        if (current instanceof VentanaCalendario) {
            // 1.1) Restaurar snapshot
            if (SeguimientoFragment.globalPrevUser != null) {
                SeguimientoFragment.globalPrevUser.restore();
                SeguimientoFragment.globalPrevUser = null;
            }
            // 1.2) Volver al fragmento anterior (SeguimientoFragment)
            getSupportFragmentManager().popBackStack();
            return; // no hago nada más
        }

        // 2) El resto del comportamiento que ya tenías
        if (current instanceof VentanaPrincipal) {
            super.onBackPressed();
        } else {
            cargarFragmentoInicio(new VentanaPrincipal());
        }
    }

}
