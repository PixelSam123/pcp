package io.github.pixelsam123.pcp;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.smallrye.mutiny.unchecked.UncheckedSupplier;

public class Utils {
    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean areItemsEqual(Tuple tuple) {
        Object firstItem = tuple.nth(0);

        for (Object item : tuple) {
            if (!item.equals(firstItem)) {
                return false;
            }
        }

        return true;
    }

    public static <T> Uni<T> runInWorkerPool(UncheckedSupplier<T> supplier) {
        return Uni
            .createFrom()
            .item(Unchecked.supplier(supplier))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
