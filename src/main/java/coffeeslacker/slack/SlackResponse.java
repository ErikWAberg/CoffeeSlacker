package coffeeslacker.slack;

public class SlackResponse {

    private String text;

    public SlackResponse(String text) {
        this.text = text;
    }

    public SlackResponse() {
    }

    public String getText() {
        return text;
    }
}
