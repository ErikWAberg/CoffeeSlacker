package coffeeslacker.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrewStatService {

    private final BrewStatRepository mBrewStatRepository;
    private LocalDate mPreviousStatReset = null;
    private final String cLastStatResetFile = "previous-reset.txt";

    @Autowired
    public BrewStatService(BrewStatRepository pBrewStatRepository) {
        mBrewStatRepository = pBrewStatRepository;
    }
/*
    public void deleteEverything() {
        mBrewStatRepository.deleteAll();
    }
*/

    public BrewStat getBrewStatByDate(LocalDate pDate) {
        BrewStat tBrewStat = mBrewStatRepository.findByDate(pDate);
        if (tBrewStat == null) {
            return new BrewStat(pDate, 0, 0);
        }
        if (tBrewStat.getClaimed() == null) {
            tBrewStat.setClaimed(0);
        }
        return tBrewStat;
    }

    public BrewStat save(BrewStat pBrewStat) {
        return mBrewStatRepository.save(pBrewStat);
    }

    public BrewStat getTopBrewStat() {
        return mBrewStatRepository.findFirstByOrderByBrewsDesc();
    }

    public List<BrewStat> getBrewStatSortedByBrewCount() {
        return mBrewStatRepository.findAll(new Sort(Sort.Direction.DESC, "brews")).stream()
                .limit(15)
                .collect(Collectors.toList());
    }

    public List<BrewStat> getAllBrewStats() {
        return mBrewStatRepository.findAll();
    }


    public void incrementTodaysBrews() {
        final BrewStat tBrewStatByDate = getBrewStatByDate(LocalDate.now());
        tBrewStatByDate.incrementBrews();
        save(tBrewStatByDate);
    }

    public void incrementTodaysClaimed() {
        final BrewStat tBrewStatByDate = getBrewStatByDate(LocalDate.now());
        tBrewStatByDate.incrementClaimed();
        save(tBrewStatByDate);
    }

    public List<BrewStat> findByClaimed(int claimed) {
        return mBrewStatRepository.findByClaimed(claimed);
    }

    public void delete(BrewStat pBrewStat) {
        mBrewStatRepository.delete(pBrewStat);
    }

    public void deleteZeroBrewEntries() {
        mBrewStatRepository.findByBrews(0)
                .forEach(mBrewStatRepository::delete);
    }

    public boolean shouldResetMonthlyStats() {
        // i dont care man
        if(mPreviousStatReset == null) {
            if(!Files.exists(Paths.get(cLastStatResetFile))) {
                return true;
            }
            mPreviousStatReset = LocalDate.parse(getPersistedResetDate());
        }
        return !mPreviousStatReset.equals(LocalDate.now().withDayOfMonth(1));
    }

    public void monthlyStatsWereReset() {
        mPreviousStatReset = LocalDate.now().withDayOfMonth(1);
        persistResetDate();
    }


    private String getPersistedResetDate() {
        try {
            return new String(Files.readAllBytes(Paths.get(cLastStatResetFile)));
        } catch (IOException pE) {
            pE.printStackTrace();
        }
        return null;
    }

    private void persistResetDate() {

        try {
            Files.write(Paths.get(cLastStatResetFile),
                    LocalDate.now().withDayOfMonth(1).toString().getBytes(Charset.forName("UTF-8")),
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException pE) {
            pE.printStackTrace();
        }

    }


}
