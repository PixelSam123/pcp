package io.github.pixelsam123.pcp;

import io.smallrye.mutiny.tuples.Tuple;

public class Utils {
    public static boolean areItemsEqual(Tuple tuple) {
        Object firstItem = tuple.nth(0);

        for (Object item : tuple) {
            if (!item.equals(firstItem)) {
                return false;
            }
        }

        return true;
    }
}
