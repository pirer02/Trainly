package com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion;

import android.text.InputFilter;
import android.text.Spanned;

public class DecimalDigitsInputFilter implements InputFilter {
    private final int digitsBeforeZero;
    private final int digitsAfterZero;

    public DecimalDigitsInputFilter(int digitsBeforeZero, int digitsAfterZero) {
        this.digitsBeforeZero = digitsBeforeZero;
        this.digitsAfterZero = digitsAfterZero;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {

        String fullString = dest.toString().substring(0, dstart) + source + dest.toString().substring(dend);
        if (fullString.matches("^\\d{0," + digitsBeforeZero + "}(\\.\\d{0," + digitsAfterZero + "})?$")) {
            return null;
        }
        return "";
    }
}
