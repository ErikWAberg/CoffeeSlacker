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


    @PostConstruct
    public void asd() {
        mBrewerRepository.deleteAll();

        for (int i = 0; i < 20; i++) {
            Brewer tBrewer = new Brewer(null, "a" + i, 2);
            mBrewerRepository.save(tBrewer);
        }
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
    /*
    public Brewer adjustBrews(Brewer pBrewer, int amount) {
        Brewer tBrewer = mBrewerRepository.findBySlackUser(pSlackUser);
        tBrewer.adjustBrews(amount);
        return mBrewerRepository.save(tBrewer);
    }*/
/*
    public void setBrewMaster(Brewer pNewBrewMaster) {
        final Brewer tPrevBrewMaster = mBrewerRepository.findByBrewMaster(true);
        if(tPrevBrewMaster != null) {
            tPrevBrewMaster.setBrewMaster(false);
            mBrewerRepository.save(tPrevBrewMaster);
        }

        if(pNewBrewMaster != null) {
            pNewBrewMaster = getBrewer(pNewBrewMaster.getSlackUser());
            pNewBrewMaster.setBrewMaster(true);
            mBrewerRepository.save(pNewBrewMaster);
        }
    }*/

    public Brewer getBrewMaster() {
        //return mBrewerRepository.findByBrewMaster(true);
        final List<Brewer> tNumTopBrewers = getTopBrewers();
        if (tNumTopBrewers.size() == 1) {
            return tNumTopBrewers.get(0);
        }
        return null;
    }

    public List<Brewer> getBrewersSortedByBrewCount() {
        return mBrewerRepository.findAll(new Sort(Sort.Direction.DESC, "brews"));
        //return mBrewerRepository.findAll(new Sort(Sort.Direction.DESC, "brewMaster").and(new Sort(Sort.Direction.DESC, "brews")));
    }


    private List<Brewer> getTopBrewers() {
        Brewer tBrewer = mBrewerRepository.findFirstByOrderByBrewsDesc();
        return mBrewerRepository.findByBrews(tBrewer.getBrews());
    }

}
