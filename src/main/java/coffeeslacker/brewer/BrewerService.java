package coffeeslacker.brewer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrewerService {

    private final BrewerRepository mBrewerRepository;

    @Autowired
    public BrewerService(BrewerRepository pBrewerRepository) {
        mBrewerRepository = pBrewerRepository;
    }

    public Brewer registerBrewer(String pSlackUser, String pRfid) {
        Brewer tBrewer = getBrewerByRfid(pRfid);

        if(tBrewer == null) {
            tBrewer = new Brewer(pRfid, pSlackUser, 0);
        } else {
            tBrewer.update(pSlackUser);
        }

        mBrewerRepository.save(tBrewer);
        return tBrewer;
    }

    public Brewer getBrewerByRfid(String pRfid) {
        return mBrewerRepository.findByRfid(pRfid);
    }

    public Brewer getBrewerBySlackUser(String pSlackUser) {
        Brewer tBrewer = mBrewerRepository.findBySlackUser(pSlackUser);
        if(tBrewer == null) {
            tBrewer = registerBrewer(pSlackUser, null);
        }
        return tBrewer;
    }

    public Brewer incrementBrews(Brewer pBrewer) {
        pBrewer.incrementBrews();
        mBrewerRepository.save(pBrewer);
        return pBrewer;
    }

    public Brewer getTopBrewer() {
        return mBrewerRepository.findFirstByOrderByBrewsDesc();
    }

    public List<Brewer> getBrewersSortedByBrewCount() {
        return mBrewerRepository.findAll(new Sort(Sort.Direction.DESC, "brews")).stream()
                .limit(15)
                .collect(Collectors.toList());
    }

    public List<Brewer> getAllBrewers() {
        return mBrewerRepository.findAll();
    }


    public Brewer decrementBrews(final Brewer pBrewer) {
        pBrewer.decrementBrews();
        mBrewerRepository.save(pBrewer);
        return pBrewer;
    }
}
