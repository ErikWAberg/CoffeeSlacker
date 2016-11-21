package coffeeslacker.statistics;

import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;

public class BrewStat {

    @Id
    public String id;

    private LocalDate date;
    private int brews;
    private Integer claimed;
    private List<LocalTime> startTimes;

    public BrewStat(final LocalDate date, final int brews, final Integer claimed) {
        this.date = date;
        this.brews = brews;
        this.claimed = claimed;
        startTimes = new LinkedList<>();
    }

    public void incrementBrews() {
        brews++;
        startTimes.add(LocalTime.now());
    }

    public void incrementClaimed() {
        claimed++;
    }

    public int getBrews() {
        return brews;
    }

    public void setBrews(final int pBrews) {
        brews = pBrews;
    }

    public Integer getClaimed() {
        return claimed;
    }

    public void setClaimed(final Integer pClaimed) {
        claimed = pClaimed;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<LocalTime> getStartTimes() {
        return startTimes;
    }

    public void setDate(final LocalDate pDate) {
        date = pDate;
    }

    @Override
    public String toString() {
        return "BrewStat{" +
                "id='" + id + '\'' +
                ", date=" + date +
                ", brews=" + brews +
                '}';
    }


}
