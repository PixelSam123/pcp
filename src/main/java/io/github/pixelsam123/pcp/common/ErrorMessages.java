package io.github.pixelsam123.pcp.common;

public class ErrorMessages {
    public static final String CREDENTIALS_MISMATCH = "User of your credentials doesn't exist";
    public static final String NO_EDIT_PERMISSION = "Not allowed to edit on another user's behalf";
    public static final String NO_DELETE_PERMISSION =
        "Not allowed to delete on another user's behalf";

    private ErrorMessages() {
        throw new IllegalStateException("Class that only contains static constants");
    }
}
