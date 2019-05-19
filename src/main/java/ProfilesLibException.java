/**
 * Создана ошибка для того, чтобы ее было удобнее отлавливать: по ней точно понятно, что она из библиотеки
 */
public class ProfilesLibException extends RuntimeException {

    public ProfilesLibException(String message) {
        super(message);
    }

    public ProfilesLibException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProfilesLibException(Throwable cause) {
        super(cause);
    }
}
