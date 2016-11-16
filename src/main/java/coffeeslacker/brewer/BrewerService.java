package coffeeslacker.brewer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class BrewerService {

    private final BrewerRepository mBrewerRepository;

    @Autowired
    public BrewerService(BrewerRepository pBrewerRepository) {
        mBrewerRepository = pBrewerRepository;
    }

    private int mInitialBrewerCount = 0;

    @PostConstruct
    public void postConstruct() {

    }

    public void deleteEverything() {
        mBrewerRepository.deleteAll();
    }

    public Brewer registerRfid(final String pSlackUser, final String pRfid) {
        Brewer tBySlackUser = getBrewer(pSlackUser);
        tBySlackUser.setRfid(pRfid);
        return mBrewerRepository.save(tBySlackUser);
    }

    public Brewer getBrewerByRfid(String pRfid) {
        return mBrewerRepository.findByRfid(pRfid);
    }

    public Brewer getBrewer(String pSlackUser) {
        Brewer tBrewer = mBrewerRepository.findBySlackUser(pSlackUser);
        if (tBrewer == null) {
            tBrewer = new Brewer(null, pSlackUser, 0);
        }
        tBrewer = mBrewerRepository.save(tBrewer);
        return tBrewer;
    }

    public Brewer getBrewer(Brewer pBrewer) {
        return mBrewerRepository.findBySlackUser(pBrewer.getSlackUser());
    }

    public Brewer save(Brewer pBrewer) {
        return mBrewerRepository.save(pBrewer);
    }

    public List<Brewer> getBrewersSortedByBrewCount() {
        return mBrewerRepository.findAll(new Sort(Sort.Direction.DESC, "brews").and(new Sort(Sort.Direction.ASC, "slackUser")));
    }

    public Brewer getBrewMaster() {
        final List<Brewer> tTopBrewers = getTopBrewers();

        if (tTopBrewers.size() == 1) {
            return tTopBrewers.get(0);
        }
        return null;
    }

    private List<Brewer> getTopBrewers() {
        Brewer tBrewer = mBrewerRepository.findFirstByOrderByBrewsDesc();
        return mBrewerRepository.findByBrews(tBrewer.getBrews());
    }

    public int getBrewerCount() {
        if(mInitialBrewerCount < 2) {
            final List<Brewer> tAll = mBrewerRepository.findAll();
            mInitialBrewerCount = tAll.size();
        }
        return mInitialBrewerCount;
    }
}
