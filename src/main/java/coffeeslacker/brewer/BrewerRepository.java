package coffeeslacker.brewer;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BrewerRepository extends MongoRepository<Brewer, String> {

    Brewer findByRfid(String rfid);
    Brewer findBySlackUser(String slackUser);
    Brewer findFirstByOrderByBrewsDesc();

}
