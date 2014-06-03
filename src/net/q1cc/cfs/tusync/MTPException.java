package net.q1cc.cfs.tusync;

public class MTPException extends Exception {

    public MTPException() {
    }

    public MTPException(String message) {
        super(message);
    }

    public MTPException(Throwable cause) {
        super(cause);
    }

    public MTPException(String message, Throwable cause) {
        super(message, cause);
    }

    public MTPException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
