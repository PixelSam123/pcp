package io.github.pixelsam123.pcp.challenge;

public enum ChallengeSort {
    NEWEST("timestamp", "DESC"),
    OLDEST("timestamp", "ASC"),
    MOST_COMPLETED("completed_count", "DESC"),
    LEAST_COMPLETED("completed_count", "ASC");

    public final String sql;

    ChallengeSort(String column, String orderType) {
        sql = "ORDER BY " + column + " " + orderType;
    }
}
