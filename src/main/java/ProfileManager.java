import dbclasses.Action;
import dbclasses.ActionType;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * По идее, можно сделать эту библиотеку спринг-приложением, и работать через liquibase. Однако, насколько я понимаю,
 * библиотеки стараются делать как можно более легковесными и тащить меньше зависимостей, и я не совсем поняла,
 * как предполагается использовать эту библиотеку.
 */
@SuppressWarnings("UnusedReturnValue")
public class ProfileManager {
    private final Logger logger = LogManager.getLogger(getClass());

    private final String dbUrl;

    /**
     * Не уверена, насколько этот класс нормально работает, но в интернете рекламировался как Named locks
     */
    private final KeyLockManager lockManager = KeyLockManagers.newLock();


    public ProfileManager(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public boolean createProfile(String userName, String password) throws ProfilesLibException {
        return lockManager.executeLocked(userName, () -> {
            try (Connection c = DriverManager.getConnection(dbUrl)) {
                c.setAutoCommit(false);

                if (getPasswordForUser(userName, c).next()) {
                    logger.error("Profile with profile name {} already exists", userName);
                    return false;
                }

                String sql = "insert into Profiles (profile_name, password, last_seen) values (?, ?, ?)";
                PreparedStatement preparedStatement = c.prepareStatement(sql);
                preparedStatement.setObject(1, userName, Types.VARCHAR);
                preparedStatement.setObject(2, password, Types.VARCHAR);
                preparedStatement.setObject(3, null, Types.TIMESTAMP);
                preparedStatement.execute();

                insertAction(userName, ActionType.CREATED, c);

                logger.info("Profile {} created", userName);

                c.commit();
                return true;
            }
            catch (SQLException e) {
                logger.error("Error while creating profile with userName {}", userName, e);
                throw new ProfilesLibException("Error while creating profile", e);
            }
        });
    }

    private ResultSet getPasswordForUser(String userName, Connection c) throws SQLException {
        String sql = "select password from Profiles where profile_name = ?";

        PreparedStatement preparedStatement = c.prepareStatement(sql);
        preparedStatement.setObject(1, userName, Types.VARCHAR);

        return preparedStatement.executeQuery();
    }

    private void insertAction(String userName, ActionType actionType, Connection c) throws SQLException {
        String sql = "insert into Actions (id, profile_name, action_timestamp, action_type) values (nextval('actions_sq'), ?, ?, ?)";
        PreparedStatement preparedStatement = c.prepareStatement(sql);
        preparedStatement.setObject(1, userName, Types.VARCHAR);
        preparedStatement.setObject(2, new Timestamp(System.currentTimeMillis()), Types.TIMESTAMP);
        preparedStatement.setObject(3, actionType.name(), Types.VARCHAR);
        preparedStatement.execute();
    }


    public boolean login(String userName, String password) throws ProfilesLibException {
        return lockManager.executeLocked(userName, () -> {
            try (Connection c = DriverManager.getConnection(dbUrl)) {
                c.setAutoCommit(false);

                if (!userExists(userName, password, c)) {
                    return false;
                }

                String sql = "update Profiles set last_seen = ? where profile_name = ?";
                PreparedStatement preparedStatement = c.prepareStatement(sql);
                preparedStatement.setObject(1, new Timestamp(System.currentTimeMillis()), Types.TIMESTAMP);
                preparedStatement.setObject(2, userName, Types.VARCHAR);
                preparedStatement.execute();

                insertAction(userName, ActionType.LOGIN, c);

                logger.info("Profile {} logged in", userName);

                c.commit();

                return true;
            }
            catch (SQLException e) {
                logger.error("Error while login into profile with userName {}", userName, e);
                throw new ProfilesLibException("Error while login", e);
            }
        });
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean userExists(String userName, String password, Connection c) throws SQLException {
        ResultSet passwordForUser = getPasswordForUser(userName, c);
        if (!passwordForUser.next()) {
            return false;
        }
        String dbPassword = passwordForUser.getString(1);

        return Objects.equals(password, dbPassword);
    }


    public List<Action> getUserActions(String userName) throws ProfilesLibException {
        return lockManager.executeLocked(userName, () -> {
            try (Connection c = DriverManager.getConnection(dbUrl)) {
                String sql = "select * from Actions where profile_name = ?";

                PreparedStatement preparedStatement = c.prepareStatement(sql);
                preparedStatement.setObject(1, userName, Types.VARCHAR);

                ResultSet resultSet = preparedStatement.executeQuery();

                List<Action> actions = new ArrayList<>();
                while (resultSet.next()) {
                    int id = resultSet.getInt(1);
                    Timestamp timestamp = resultSet.getTimestamp(3);
                    String actionType = resultSet.getString(4);

                    actions.add(new Action(id, userName, timestamp, ActionType.valueOf(actionType)));
                }

                return actions;
            }
            catch (SQLException e) {
                logger.error("Error while getting profile {} actions", userName, e);
                throw new ProfilesLibException("Error while getting profile actions", e);
            }
        });
    }


    public boolean changePassword(String userName, String oldPassword, String newPassword) throws ProfilesLibException {
        return lockManager.executeLocked(userName, () -> {
            try (Connection c = DriverManager.getConnection(dbUrl)) {
                c.setAutoCommit(false);

                if (!userExists(userName, oldPassword, c)) {
                    logger.info("Profile {} with such login or password doesn't exist", userName);
                    return false;
                }

                String sql = "update Profiles set password = ? where profile_name = ?";
                PreparedStatement preparedStatement = c.prepareStatement(sql);
                preparedStatement.setObject(1, newPassword, Types.VARCHAR);
                preparedStatement.setObject(2, userName, Types.VARCHAR);
                preparedStatement.execute();

                insertAction(userName, ActionType.PASSWORD_CHANGED, c);

                logger.info("Profile {} changed password", userName);

                c.commit();
                return true;
            }
            catch (SQLException e) {
                logger.error("Error while changing password for profile with userName {}", userName, e);
                throw new ProfilesLibException("Error while changing password", e);
            }
        });
    }
}
