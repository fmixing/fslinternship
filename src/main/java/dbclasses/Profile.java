package dbclasses;

import java.sql.Timestamp;
import java.util.Objects;

public class Profile {
    private final String profileName;

    private final String password;

    private final Timestamp lastSeen;


    public Profile(String profileName, String password, Timestamp lastSeen) {
        this.profileName = profileName;
        this.password = password;
        this.lastSeen = lastSeen;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getPassword() {
        return password;
    }

    public Timestamp getLastSeen() {
        return lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Profile profile = (Profile) o;
        return profileName.equals(profile.profileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileName);
    }
}
