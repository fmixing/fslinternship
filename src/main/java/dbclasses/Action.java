package dbclasses;

import java.sql.Timestamp;

public class Action {
    private final int id;

    private final String userName;

    private final Timestamp timestamp;

    private final ActionType type;


    public Action(int id, String userName, Timestamp timestamp, ActionType type) {
        this.id = id;
        this.userName = userName;
        this.timestamp = timestamp;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public ActionType getType() {
        return type;
    }
}
