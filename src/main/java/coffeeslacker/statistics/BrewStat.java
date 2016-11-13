package coffeeslacker.statistics;

import org.springframework.data.annotation.Id;

import java.time.LocalDate;

public class BrewStat {

    @Id
    public String id;

    private LocalDate date;
    private int brews;

    public BrewStat(final LocalDate date, final int brews) {
        this.date = date;
        this.brews = brews;
    }

    public void incrementBrews() {
        brews++;
    }

    public int getBrews() {
        return brews;
    }

    public void setBrews(final int pBrews) {
        brews = pBrews;
    }

    public LocalDate getDate() {
        return date;
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
