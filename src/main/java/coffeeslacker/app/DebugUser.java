package coffeeslacker.app;

public class DebugUser {

    private final String user;
    private final String password;

    public DebugUser(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
