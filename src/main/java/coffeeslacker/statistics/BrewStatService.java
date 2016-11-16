package coffeeslacker.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrewStatService {

    private final BrewStatRepository mBrewStatRepository;

    @Autowired
    public BrewStatService(BrewStatRepository pBrewStatRepository) {
        mBrewStatRepository = pBrewStatRepository;
    }

    @PostConstruct
    public void postConstruct() {

    }

    public void deleteEverything() {
        mBrewStatRepository.deleteAll();
    }


    public BrewStat getBrewStatByDate(LocalDate pDate) {
        BrewStat tBrewStat = mBrewStatRepository.findByDate(pDate);
        if(tBrewStat == null) {
            tBrewStat = new BrewStat(pDate, 0);
            return mBrewStatRepository.save(tBrewStat);
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
}
