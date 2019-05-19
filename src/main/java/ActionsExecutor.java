import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Класс для вызова всех методов из ProfileManager с тем же порядком параметров
 */
public class ActionsExecutor {
    public static void main(String[] args) throws Exception {
        String url = getUrl(ActionsExecutor.class.getClassLoader());
        ProfileManager profileManager = new ProfileManager(url);
        Class<ProfileManager> profileManagerClass = ProfileManager.class;
        for (Method m : profileManagerClass.getMethods()) {
            if (m.getName().equals(args[0])) {
                List<String> argsList = Arrays.asList(args).subList(1, args.length);
                m.invoke(profileManager, argsList.toArray());
            }
        }
    }


    public static String getUrl(ClassLoader classLoader) throws Exception {
        InputStream resource = classLoader.getResourceAsStream("dbconnection.properties");
        if (resource == null) {
            return null;
        }

        Properties prop = new Properties();
        prop.load(resource);
        return prop.getProperty("url");
    }
}
