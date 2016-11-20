package coffeeslacker.statistics;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface BrewStatRepository extends MongoRepository<BrewStat, LocalDate> {

    BrewStat findByDate(LocalDate date);
    BrewStat findFirstByOrderByBrewsDesc();
    List<BrewStat> findByClaimed(int claimed);

}
