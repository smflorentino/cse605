package javax.safetycritical;

import javax.safetycritical.annotate.SCJAllowed;

/**
 * A Level-Zero Safety Critical Java application is comprised of one or more Level0Missions.
 *  Each Level0Mission is implemented as a subclass of this ab- stract Level0Mission class.
 */
@SCJAllowed
public abstract class Level0Mission extends Mission {

    @SCJAllowed 
    public Level0Mission( ) {
	super();
    }

    @SCJAllowed 
    protected abstract CyclicSchedule getSchedule( );
}