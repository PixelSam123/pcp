package io.github.pixelsam123.pcp.common;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.smallrye.mutiny.unchecked.UncheckedSupplier;

public class Utils {
    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Convenience function to check if multiple items in a tuple are equal.
     * Useful for cross-checking of reactively fetched entity IDs.
     *
     * @param tuple tuples to check
     * @return are all tuple items equal
     */
    public static boolean areItemsEqual(Tuple tuple) {
        Object firstItem = tuple.nth(0);

        for (Object item : tuple) {
            if (!item.equals(firstItem)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Convenience function to transform the given (unchecked) supplier into a regular supplier,
     * then run it on the default worker pool.
     *
     * @param supplier the item supplier, must not be {@code null}, can produce {@code null}
     * @param <T>      the type of item supplied by this supplier
     * @return the new {@link Uni}
     */
    public static <T> Uni<T> runInWorkerPool(UncheckedSupplier<T> supplier) {
        return Uni
            .createFrom()
            .item(Unchecked.supplier(supplier))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
