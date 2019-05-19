package dbclasses;

public enum ActionType {
    CREATED("profile-created"),
    LOGIN("login"),
    PASSWORD_CHANGED("change-password");

    private final String type;

    ActionType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}