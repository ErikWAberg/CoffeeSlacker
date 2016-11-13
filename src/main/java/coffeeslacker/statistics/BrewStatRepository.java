package coffeeslacker.statistics;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;

public interface BrewStatRepository extends MongoRepository<BrewStat, LocalDate> {

    BrewStat findByDate(LocalDate date);
    BrewStat findFirstByOrderByBrewsDesc();

}
