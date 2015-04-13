/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

package javax.safetycritical;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.HighResolutionTime;
import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import edu.purdue.scj.VMSupport;
import edu.purdue.scj.utils.Utils;

/**
 * 
 * Level 0 applications are assumed to be scheduled by a cyclic executive where
 * the schedule is created by static analysis tools offline.
 * 
 * @author plsek
 * 
 */
@SCJAllowed
@Scope(IMMORTAL)
public abstract class CyclicExecutive extends Mission implements Safelet {

    private MissionSequencer _sequencer;
    private static final int NANOS_PER_MILLI = 1000 * 1000;

    @SCJAllowed
    public CyclicExecutive(PriorityParameters priority,
            StorageParameters storage) {
        _sequencer = new SingleMissionSequencer(priority, storage, this);
    }

    @SCJAllowed
    public CyclicExecutive(StorageParameters storage) {
        _sequencer = new SingleMissionSequencer(null, storage, this);
    }

    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public MissionSequencer getSequencer() {
        return _sequencer;
    }

    @SCJAllowed(SUPPORT)
    public abstract CyclicSchedule getSchedule(PeriodicEventHandler[] peh);

    /** Do the Cyclic Execution. */
    protected final void exec(MissionManager manager) {

        if (manager.getHandlers() == 0) {
            // Mission has nothing to do, terminate.
            _terminateAll = true;

            // <FIXME> add error messages
            System.out.println("[SCJ] No Handlers Specified!");
            System.out.println("[SCJ] Mission terminates, this mission: "
                    + this);
            return;
        }

        PeriodicEventHandler[] handlers = new PeriodicEventHandler[manager
                .getHandlers()];
        PeriodicEventHandler handler = (PeriodicEventHandler) manager
                .getFirstHandler();
        int iter = 0;
        while (handler != null) {
            handlers[iter] = handler;
            handler = (PeriodicEventHandler) handler.getNext();
            iter++;
        }

        CyclicSchedule schedule = getSchedule(handlers);
        CyclicSchedule.Frame[] frames = schedule.getFrames();

        Wrapper wrapper = new Wrapper();
        AbsoluteTime targetTime = Clock.getRealtimeClock().getTime();

        while (_phase == Mission.Phase.EXECUTE) {
            for (int i = 0; i < frames.length; i++) {
                targetTime.add(frames[i].getDuration(), targetTime);
                PeriodicEventHandler[] frameHandlers = frames[i].getHandlers();
                for (int j = 0; j < frameHandlers.length; j++) {
                    if (frameHandlers[j] != null) {
                        wrapper._handler = frameHandlers[j];
                        wrapper.runInItsInitArea();
                    }
                }
                if (_phase != Mission.Phase.EXECUTE)
                    break;
                else
                    waitForNextFrame(targetTime);
            }
        }

        // FIXME: cleanup the handlers, TODO: they should run in their memory
        // areas??
        // TerminationWrapper tWrapper = new TerminationWrapper();
        for (PeriodicEventHandler hnd : handlers) {
            // tWrapper._handler = hnd;
            // tWrapper.runInItsInitArea();
            hnd.cleanUp();
        }
    }

    private static void waitForNextFrame(AbsoluteTime targetTime) {
        int result;

        while (true) {
            result = VMSupport.delayCurrentThreadAbsolute(toNanos(targetTime));
            if (result == -1) {
                break;
            } else if (result == 0)
                break;
            // TODO: here, result == 1, the sleep is interrupted, try to sleep
            // again.
        }
    }

    static long toNanos(HighResolutionTime time) {
        long nanos = time.getMilliseconds() * NANOS_PER_MILLI
                + time.getNanoseconds();
        if (nanos < 0)
            nanos = Long.MAX_VALUE;

        return nanos;
    }

    /** For making every handler run in its own PrivateMemory */
    class Wrapper implements Runnable {

        PeriodicEventHandler _handler = null;

        void runInItsInitArea() {
            if (_handler != null) {
                _handler.getInitArea().enter(this);
            } else {
                Utils.panic("ERROR: handler is null");
            }
        }

        public void run() {
            try {
                _handler.handleAsyncEvent();
            } catch (Throwable t) {
                Utils.debugPrintln(t.toString());
                t.printStackTrace();
            }
        }
    }

    /** For making every handler run in its own PrivateMemory */
    class TerminationWrapper implements Runnable {

        PeriodicEventHandler _handler = null;

        void runInItsInitArea() {
            if (_handler != null) // TODO: if we will use the "maxHandlers"
                // fields in MissionManager, we dont need
                // this.
                _handler.getInitArea().enter(this);
            else {
                Utils.panic("ERROR: handler is null");
            }
        }

        public void run() {
            try {
                _handler.cleanUp();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
