package cn.iinti.majora.adr.utils;


import android.arch.core.util.Function;

public class Joiner {
    public static <T> String join(Iterable<T> collection) {
        return join(collection, null);
    }

    public static <T> String join(Iterable<T> collection, Function<T, String> transformer) {
        if (collection == null) {
            return "";
        }
        if (transformer == null) {
            transformer = Object::toString;
        }
        StringBuilder sb = new StringBuilder();
        for (T t : collection) {
            String str = "";
            if (t != null) {
                str = transformer.apply(t);
            }
            sb.append(str).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }


}
