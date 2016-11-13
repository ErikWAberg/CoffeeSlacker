package coffeeslacker.slack;

public class SlackNotification {

    private String team_id;
    private String token;
    private String team_domain;
    private String channel_id;
    private String channel_name;
    private String timestamp;
    private String user_id;
    private String user_name;
    private String text;
    private String trigger_word;

    public SlackNotification(String pTeam_id, String pToken, String pTeam_domain, String pChannel_id, String pChannel_name, String pTimestamp, String pUser_id, String pUser_name, String pText, String pTrigger_word) {
        team_id = pTeam_id;
        token = pToken;
        team_domain = pTeam_domain;
        channel_id = pChannel_id;
        channel_name = pChannel_name;
        timestamp = pTimestamp;
        user_id = pUser_id;
        user_name = pUser_name;
        text = pText;
        trigger_word = pTrigger_word;
    }

    public String getTeam_id() {
        return team_id;
    }

    public void setTeam_id(String pTeam_id) {
        team_id = pTeam_id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String pToken) {
        token = pToken;
    }

    public String getTeam_domain() {
        return team_domain;
    }

    public void setTeam_domain(String pTeam_domain) {
        team_domain = pTeam_domain;
    }

    public String getChannel_id() {
        return channel_id;
    }

    public void setChannel_id(String pChannel_id) {
        channel_id = pChannel_id;
    }

    public String getChannel_name() {
        return channel_name;
    }

    public void setChannel_name(String pChannel_name) {
        channel_name = pChannel_name;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String pTimestamp) {
        timestamp = pTimestamp;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String pUser_id) {
        user_id = pUser_id;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String pUser_name) {
        user_name = pUser_name;
    }

    public String getText() {
        return text;
    }

    public void setText(String pText) {
        text = pText;
    }

    public String getTrigger_word() {
        return trigger_word;
    }

    public void setTrigger_word(String pTrigger_word) {
        trigger_word = pTrigger_word;
    }

    @Override
    public String toString() {
        return "SlackNotification{" +
                "team_id='" + team_id + '\'' +
                ", token='" + token + '\'' +
                ", team_domain='" + team_domain + '\'' +
                ", channel_id='" + channel_id + '\'' +
                ", channel_name='" + channel_name + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", user_id='" + user_id + '\'' +
                ", user_name='" + user_name + '\'' +
                ", text='" + text + '\'' +
                ", trigger_word='" + trigger_word + '\'' +
                '}';
    }
}
