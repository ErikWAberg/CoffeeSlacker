package coffeeslacker.brewer;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BrewerRepository extends MongoRepository<Brewer, String> {

    Brewer findByRfid(String rfid);

    Brewer findBySlackUser(String slackUser);

    Brewer findFirstByOrderByMonthlyBrewsDesc();

    List<Brewer> findByMonthlyBrews(Integer monthlyBrews);

}
