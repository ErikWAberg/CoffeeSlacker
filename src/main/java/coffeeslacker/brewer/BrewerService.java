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
        final List<Brewer> byMonthlyBrews = mBrewerRepository.findByMonthlyBrews(null);
        byMonthlyBrews.forEach(pOldDocument -> mBrewerRepository.save(pOldDocument));
        System.out.println("__________________________________________________");
        System.out.println("__________________________________________________");
        System.out.println("__________________________________________________");
        System.out.println("UPDATED " + byMonthlyBrews.size());
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
            tBrewer = new Brewer(null, pSlackUser, 0, 0, 0);
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

    public List<Brewer> getAllBrewers() {
        return mBrewerRepository.findAll();
    }

    public List<Brewer> getBrewersSortedByBrewCountThisMonth() {
        return mBrewerRepository.findAll(new Sort(Sort.Direction.DESC, "monthlyBrews").and(new Sort(Sort.Direction.DESC, "wins")).and(new Sort(Sort.Direction.DESC, "brews")).and(new Sort(Sort.Direction.ASC, "slackUser")));
    }

    public Brewer getBrewMaster() {
        final List<Brewer> tTopBrewers = getTopBrewers();

        if (tTopBrewers.size() == 1) {
            return tTopBrewers.get(0);
        }
        return null;
    }

    public List<Brewer> getTopBrewers() {
        Brewer tBrewer = mBrewerRepository.findFirstByOrderByMonthlyBrewsDesc();
        return mBrewerRepository.findByMonthlyBrews(tBrewer.getMonthlyBrews());
    }

    public int getBrewerCount() {
        if (mInitialBrewerCount < 2) {
            final List<Brewer> tAll = mBrewerRepository.findAll();
            mInitialBrewerCount = tAll.size();
        }
        return mInitialBrewerCount;
    }

    public void deleteBrewer(final String pSlackUser) {
        Brewer tBrewer = mBrewerRepository.findBySlackUser(pSlackUser);
        if (tBrewer != null) {
            mBrewerRepository.delete(tBrewer);
        }
    }
}
