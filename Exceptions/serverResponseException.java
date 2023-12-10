package Exceptions;

public class serverResponseException extends Exception{

    private String errorCode;

    public serverResponseException(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
