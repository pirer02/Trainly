package com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos;
import android.text.InputFilter;
import android.text.Spanned;

public class InputFilterPeso implements InputFilter {
    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {

        String newVal = dest.toString().substring(0, dstart) + source + dest.toString().substring(dend);

        // Eliminar si es solo un punto
        if (newVal.equals(".")) return "";

        if (!newVal.matches("^\\d{0,3}(\\.\\d{0,2})?$")) return "";

        try {
            float value = Float.parseFloat(newVal);
            if (value < 0 || value > 999.99f) return "";
        } catch (NumberFormatException e) {
            return "";
        }

        return null;
    }
}
