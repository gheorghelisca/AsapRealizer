/*******************************************************************************
 *******************************************************************************/
package asap.realizer.feedback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.GuardedBy;
import saiba.bml.feedback.BMLSyncPointProgressFeedback;
import saiba.bml.feedback.BMLWarningFeedback;
import asap.bml.ext.bmla.feedback.BMLABlockProgressFeedback;
import asap.bml.ext.bmla.feedback.BMLAPredictionFeedback;
import asap.bml.ext.bmla.feedback.BMLASyncPointProgressFeedback;
import asap.realizer.planunit.TimedPlanUnit;
import asap.realizer.scheduler.BMLBlockManager;
import asap.realizerport.BMLFeedbackListener;

import com.google.common.collect.ImmutableSet;

/**
 * Default implementation for the FeedbackManager
 * @author Herwin
 *
 */
@Slf4j
public class FeedbackManagerImpl implements FeedbackManager
{
    private final BMLBlockManager bmlBlockManager;
    private final String defaultCharacterId;

    @GuardedBy("feedbackListeners")
    private final List<BMLFeedbackListener> feedbackListeners = Collections.synchronizedList(new ArrayList<BMLFeedbackListener>());


    public FeedbackManagerImpl(BMLBlockManager bbm, String vhId)
    {
        bmlBlockManager = bbm;
        defaultCharacterId = vhId;
    }

    @Override
    public void addFeedbackListener(BMLFeedbackListener fb)
    {
        feedbackListeners.add(fb);
    }
    
    @Override
    public void puException(TimedPlanUnit timedMU, String message, double time)
    {
    	puException(timedMU, message, time, defaultCharacterId);
    }
    
    @Override
    public void puException(TimedPlanUnit timedMU, String message, double time, String vhId)
    {
        String bmlId = timedMU.getBMLId();        
        String exceptionText = message + "\nBehavior " + timedMU.getBMLId() + ":" + timedMU.getId() + " dropped.";
        warn(new BMLWarningFeedback(bmlId+":"+timedMU.getId(), "EXECUTION_FAILURE",exceptionText), time, vhId);
    }
    
    private BMLASyncPointProgressFeedback constructBMLASyncPointProgressFeedback(BMLSyncPointProgressFeedback fb, String vhId)
    {
        BMLASyncPointProgressFeedback fba = BMLASyncPointProgressFeedback.build(fb);        
        fba.setCharacterId(vhId);
        fba.setPosixTime(System.currentTimeMillis());
        return fba;
    }
    
    @Override
    public void feedback(BMLSyncPointProgressFeedback fb)
    {
    	feedback(fb, defaultCharacterId);
    }
    
    @Override
    public void feedback(BMLSyncPointProgressFeedback fb, String vhId)
    {
        BMLASyncPointProgressFeedback fba = constructBMLASyncPointProgressFeedback(fb, vhId);
        synchronized (feedbackListeners)
        {
            for (BMLFeedbackListener fbl : feedbackListeners)
            {
                try
                {
                    fbl.feedback(fba.toBMLFeedbackString());
                }
                catch (Exception ex)
                {
                    log.warn("Exception in FeedbackListener: {}, feedback: {}", ex, fb.toBMLFeedbackString());
                }
            }
        }
        bmlBlockManager.syncProgress(fb);
    }

    @Override
    public void feedback(List<BMLSyncPointProgressFeedback> fbs)
    {
        feedback(fbs, defaultCharacterId);
    }
    
    @Override
    public void feedback(List<BMLSyncPointProgressFeedback> fbs, String vhId)
    {
        synchronized (feedbackListeners)
        {
            for (BMLSyncPointProgressFeedback fb : fbs)
            {
                BMLASyncPointProgressFeedback fba = constructBMLASyncPointProgressFeedback(fb, vhId);
                for (BMLFeedbackListener fbl : feedbackListeners)
                {
                    try
                    {
                        fbl.feedback(fba.toBMLFeedbackString());
                    }
                    catch (Exception ex)
                    {
                        log.warn("Exception in FeedbackListener: {}, feedback: {}", ex, fb.toBMLFeedbackString());
                    }
                }
            }
        }
        for (BMLSyncPointProgressFeedback fb : fbs)
        {
            bmlBlockManager.syncProgress(fb);
        }
    }

    @Override
    public void removeAllFeedbackListeners()
    {
        feedbackListeners.clear();
    }
    
    @Override
    public ImmutableSet<String> getSyncsPassed(String bmlId, String behaviorId)
    {
        return getSyncsPassed(bmlId, behaviorId, defaultCharacterId);
    }
    
    @Override
    public ImmutableSet<String> getSyncsPassed(String bmlId, String behaviorId, String vhId)
    {
        return bmlBlockManager.getSyncsPassed(bmlId, behaviorId);
    }

    @Override
    public void removeFeedbackListener(BMLFeedbackListener fb)
    {
        feedbackListeners.remove(fb);
    }
    
    @Override
    public void blockProgress(BMLABlockProgressFeedback psf)
    {
    	blockProgress(psf, defaultCharacterId);
    }
    
    @Override
    public void blockProgress(BMLABlockProgressFeedback psf, String vhId)
    {
        psf.setCharacterId(vhId);
        synchronized (feedbackListeners)
        {
            for (BMLFeedbackListener fbl : feedbackListeners)
            {
                try
                {
                    fbl.feedback(psf.toBMLFeedbackString());
                }
                catch (Exception ex)
                {
                    log.warn("Exception in FeedbackListener: {}, feedback: {}", ex, psf);
                }
            }
        }
        bmlBlockManager.blockProgress(psf);
    }

    
    @Override
    public void prediction(BMLAPredictionFeedback bpf)
    {
        synchronized (feedbackListeners)
        {
            String feedbackString = bpf.toBMLFeedbackString();       
            for (BMLFeedbackListener pl : feedbackListeners)
            {
                try
                {                    
                    pl.feedback(feedbackString);                    
                }
                catch (Exception ex)
                {
                    log.warn("Exception in FeedbackListener: {}, feedback: {}", ex, feedbackString);
                }
            }
        }        
    }    

    @Override
    public void warn(BMLWarningFeedback w, double time)
    {
    	warn(w, time, defaultCharacterId);
    }
    
    @Override
    public void warn(BMLWarningFeedback w, double time, String vhId)
    {
        w.setCharacterId(vhId);
        synchronized (feedbackListeners)
        {
            for (BMLFeedbackListener wl : feedbackListeners)
            {
                try
                {
                    wl.feedback(w.toBMLFeedbackString());
                }
                catch (Exception ex)
                {
                    log.warn("Exception in WarningListener: {}, feedback: {}", ex, w.toBMLFeedbackString());
                }
            }
        }
        bmlBlockManager.warn(w, time);
    }
    
}
