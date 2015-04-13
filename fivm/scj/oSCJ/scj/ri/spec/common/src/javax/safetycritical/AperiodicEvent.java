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

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.AsyncEvent;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.CALLER;

@SCJAllowed(LEVEL_1)
public class AperiodicEvent extends AsyncEvent {

    @SCJAllowed(LEVEL_1)
    public AperiodicEvent(AperiodicEventHandler handler) {
        addHandler(handler);
    }

    @SCJAllowed(LEVEL_1)
    public AperiodicEvent(AperiodicEventHandler[] handlers) {
    }

    @SCJAllowed(LEVEL_1)
    @RunsIn(CALLER)
    public void fire() {
        super.fire();
    }

    void cleanup() {
        setHandler(null);
    }
}
