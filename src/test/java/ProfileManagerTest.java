import dbclasses.Action;
import dbclasses.ActionType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Потенциально можно было использовать встроенную БД в тестах, чтобы не портить данные в реальной базе данных.
 * Но, как мне кажется, обычно тесты запускаются во время CI и на тестовых серверах, не на продакшне,
 * и данные там не будут испорчены. К тому же полезно протестить, как приложение работает с реальной БД.
 */
public class ProfileManagerTest {

    private static final String password = "standard_password";

    private String dbUrl = "jdbc:postgresql://localhost:5432/alice";

    @Before
    public void setUp() throws Exception {
        String url = ActionsExecutor.getUrl(getClass().getClassLoader());
        if (url != null) {
            dbUrl = url;
        }
        tearDown();
    }

    @Test
    public void testSimpleMethods() {
        ProfileManager profileManager = new ProfileManager(dbUrl);
        String profile = "profile";

        profileManager.createProfile(profile, password);

        List<Action> userActions = profileManager.getUserActions(profile);
        Assert.assertEquals(1, userActions.size());
        Action action = userActions.get(0);
        Assert.assertEquals(ActionType.CREATED, action.getType());
        Assert.assertEquals(profile, action.getUserName());

        Assert.assertTrue(profileManager.login(profile, password));
        Assert.assertFalse(profileManager.login(profile, password + "1"));

        userActions = profileManager.getUserActions(profile);
        Assert.assertEquals(2, userActions.size());
        action = userActions.get(1);
        Assert.assertEquals(ActionType.LOGIN, action.getType());
        Assert.assertEquals(profile, action.getUserName());

        profileManager.changePassword(profile, password, password + "1");

        userActions = profileManager.getUserActions(profile);
        Assert.assertEquals(3, userActions.size());
        action = userActions.get(2);
        Assert.assertEquals(ActionType.PASSWORD_CHANGED, action.getType());
        Assert.assertEquals(profile, action.getUserName());

        Assert.assertFalse(profileManager.login(profile, password));
        Assert.assertTrue(profileManager.login(profile, password + "1"));
    }


    @Test
    public void testConcurrent() throws Exception {
        ProfileManager profileManager = new ProfileManager(dbUrl);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            threads.add(i, new Thread(() -> {
                String profile = UUID.randomUUID().toString().replaceAll("-", "");
                while (true) {
                    try {
                        profileManager.createProfile(profile, password);
                        break;
                    } catch (ProfilesLibException ignored) {
                    }
                }
                while (true) {
                    try {
                        profileManager.login(profile, password);
                        break;
                    } catch (ProfilesLibException ignored) {
                    }
                }
                while (true) {
                    try {
                        profileManager.changePassword(profile, password, password + "1");
                        break;
                    } catch (ProfilesLibException ignored) {
                    }
                }
            }));
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    @After
    public void tearDown() throws Exception {
        String dropActions = "delete from Actions";
        String dropProfiles = "delete from Profiles";
        String dropSequence = "alter sequence actions_sq restart";
        try (Connection c = DriverManager.getConnection(dbUrl)) {
            Statement statement = c.createStatement();
            statement.execute(dropActions);
            statement.execute(dropProfiles);
            statement.execute(dropSequence);
        }
    }
}