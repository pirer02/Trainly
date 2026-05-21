package com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos;

import android.text.InputFilter;
import android.text.Spanned;

public class DecimalDigitsInputFilter implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {

        String newValue = dest.toString().substring(0, dstart) + source + dest.toString().substring(dend);

        // Permitir solo números decimales válidos (como texto)
        if (!newValue.matches("^\\d{0,2}(\\.\\d{0,1})?$")) {
            return "";
        }

        try {
            if (newValue.equals(".") || newValue.equals("")) return null;

            float value = Float.parseFloat(newValue);

            // Solo permitir de 0.0 a 10.0 y valores medios (x.0 o x.5)
            if (value < 0 || value > 10) return "";
            float decimalPart = value - (int) value;
            if (decimalPart != 0f && decimalPart != 0.5f) return "";

        } catch (NumberFormatException e) {
            return "";
        }

        return null;
    }
}
