package virusfixer.ubnt.com.ubntvirusremoval.model;

/**
 * Created by Vlad on 14.5.16.
 */
public class Login {
    private String username, password;
    public Login(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }
}
