package coffeeslacker.brewer;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Brewer implements Comparable {

    @Id
    public String id;

    private String rfid;
    private String slackUser;
    private int brews;
    private int monthlyBrews;
    private int wins;

    public Brewer(String rfid, String slackUser, int brews, Integer monthlyBrews, Integer wins) {
        this.rfid = rfid;
        this.slackUser = slackUser;
        this.brews = brews;
        this.monthlyBrews = monthlyBrews == null ? 0 : monthlyBrews;
        this.wins = wins == null ? 0 : wins;
    }

    public String getRfid() {
        return rfid;
    }

    public String getSlackUser() {
        return slackUser;
    }

    public int getBrews() {
        return brews;
    }

    public void adjustBrews(int amount) {
        brews += amount;
        if (brews < 0) {
            brews = 0;
        }

        if(amount > 0) {
            monthlyBrews += amount;
        }

    }

    public int getMonthlyBrews() {
        return monthlyBrews;
    }

    public void setMonthlyBrews(int monthlyBrews) {
        this.monthlyBrews = monthlyBrews;
    }

    public int getWins() {
        return wins;
    }

    public void incrementWins() {
        this.wins++;
    }


    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) return true;
        if (pOther == null || getClass() != pOther.getClass()) return false;

        final Brewer tBrewer = (Brewer) pOther;

        return slackUser != null ? slackUser.equals(tBrewer.slackUser) : tBrewer.slackUser == null;
    }

    @Override
    public int compareTo(final Object pOther) {
        if (this.equals(pOther) || getClass() != pOther.getClass()) {
            return 0;
        }

        final Brewer tBrewer = (Brewer) pOther;
        return this.brews - tBrewer.brews;
    }

    public void setRfid(final String pRfid) {
        rfid = pRfid;
    }

    @Override
    public String toString() {
        return "Brewer{" +
                "rfid='" + rfid + '\'' +
                ", slackUser='" + slackUser + '\'' +
                ", brews=" + brews +
                '}';
    }

    public void setBrews(final int brews) {
        this.brews = brews;
    }

    public void setWins(int pWins) {
        wins = pWins;
    }
}
