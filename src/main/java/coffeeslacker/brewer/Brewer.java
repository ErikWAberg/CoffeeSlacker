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
    //private boolean brewMaster;

    public Brewer(String rfid, String slackUser, int brews) { //, boolean brewMaster) {
        this.rfid = rfid;
        this.slackUser = slackUser;
        this.brews = brews;
        //this.brewMaster = brewMaster;
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
        if(brews < 0) {
            brews = 0;
        }
    }

    public boolean isBetterThan(final Brewer pBrewer) {
        return pBrewer == null || this.compareTo(pBrewer) > 0;
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
        if(this.equals(pOther) || getClass() != pOther.getClass()) {
            return 0;
        }

        final Brewer tBrewer = (Brewer) pOther;
        return this.brews - tBrewer.brews;
    }

    @Override
    public String toString() {
        return "Brewer{" +
                "rfid='" + rfid + '\'' +
                ", slackUser='" + slackUser + '\'' +
                ", brews=" + brews +
                '}';
    }
/*
    public boolean isBrewMaster() {
        return brewMaster;
    }

    public void setBrewMaster(final boolean pBrewMaster) {
        brewMaster = pBrewMaster;
    }
    public void update(final String pSlackUser) {
        slackUser = pSlackUser;
    }
*/
    public void setRfid(final String pRfid) {
        rfid = pRfid;
    }
}
