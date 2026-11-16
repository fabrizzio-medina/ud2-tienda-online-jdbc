package com.store.util;

import java.math.BigDecimal;

public final class FormatUtils {

    private FormatUtils(){

    }
    public static boolean isNullOrBlank(String cadena) {
        return cadena == null || cadena.isBlank();
    }

    public static String format(BigDecimal importe) {
        return importe == null ? "-" : String.format("%,.2f", importe);
    }

    public static String join(String c1, String c2){
        return String.join(" ", c1, c2);
    }


}
