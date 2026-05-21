package com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos;

import android.text.InputFilter;
import android.text.Spanned;

public class InputFilterReps implements InputFilter {
    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {

        String newVal = dest.toString().substring(0, dstart) + source + dest.toString().substring(dend);

        if (!newVal.matches("^\\d{0,5}$")) return "";

        try {
            int value = Integer.parseInt(newVal);
            if (value < 0 || value > 10000) return "";
        } catch (NumberFormatException e) {
            return "";
        }

        return null;
    }
}

