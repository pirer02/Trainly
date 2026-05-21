package com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos;

import android.text.InputFilter;
import android.text.Spanned; /**
 * Clase auxiliar para limitar los valores numéricos.
 */
public class InputFilterMinMax implements InputFilter {
    private int min, max;
    public InputFilterMinMax(int min, int max) {
        this.min = min;
        this.max = max;
    }
    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        try {
            String newVal = dest.toString().substring(0, dstart)
                    + source.toString()
                    + dest.toString().substring(dend);
            if(newVal.isEmpty()){
                return null; // Permite borrar el contenido
            }
            int input = Integer.parseInt(newVal);
            if (input >= min && input <= max)
                return null;
        } catch (NumberFormatException nfe) { }
        return "";
    }
}
