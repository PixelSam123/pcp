package io.github.pixelsam123.pcp.common;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.smallrye.mutiny.unchecked.UncheckedSupplier;

public class Utils {
    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Convenience function to check if all uni items are equal.
     * Useful for cross-checking of reactively fetched entity IDs.
     *
     * @param unis unis to check
     * @return are all uni items equal
     */
    public static Uni<Boolean> areUniItemsEqual(Uni<?>... unis) {
        return Uni.combine().all().unis(unis).with(items -> {
            Object firstItem = items.getFirst();

            for (Object item : items) {
                if (!item.equals(firstItem)) {
                    return false;
                }
            }

            return true;
        });
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
