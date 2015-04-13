package javax.safetycritical;

import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Level.SUPPORT;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;

/**
 * LinearMissionSequencer
 * 
 * @param <SpecificMission>
 */
@SCJAllowed
public class LinearMissionSequencer extends MissionSequencer {
    boolean returnedInitialMission;

    /**
     * Throws IllegalStateException if invoked during initialization of a
     * level-zero or level-one mission.
     * 
     * @param priority
     * @param storage
     * @param m
     */
    @SCJAllowed
    @SCJRestricted(value = INITIALIZATION, maySelfSuspend = false)
    public LinearMissionSequencer(PriorityParameters priority,
            StorageParameters storage, Mission m) {
        super(priority, storage);
        returnedInitialMission = false;
    }

    /**
     * Throws IllegalStateException if invoked during initialization of a
     * level-zero or level-one mission.
     * 
     * @param priority
     * @param storage
     * @param missions
     */
    @SCJAllowed
    @SCJRestricted(value = INITIALIZATION, maySelfSuspend = false)
    public LinearMissionSequencer(PriorityParameters priority,
            StorageParameters storage, Mission[] missions) {
        super(priority, storage);
        returnedInitialMission = false;
    }

    /**
     * @see javax.safetycritical.MissionSequencer#getNextMission()
     */
    @SCJAllowed(SUPPORT)
    @SCJRestricted(value = INITIALIZATION, maySelfSuspend = false)
    @Override
    protected Mission getNextMission() {
        return null;
    }
}
