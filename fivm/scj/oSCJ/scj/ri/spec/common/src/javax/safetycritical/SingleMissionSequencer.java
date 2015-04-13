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

import static javax.safetycritical.annotate.Level.SUPPORT;

import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.SCJAllowed;


import javax.safetycritical.annotate.SCJRestricted;


@SCJAllowed
public class SingleMissionSequencer extends MissionSequencer {

    private Mission _mission;

    @SCJAllowed
    public SingleMissionSequencer(PriorityParameters priority,
            StorageParameters storage, Mission mission) {
        super(priority, storage);
        _mission = mission;
    }

    @SCJAllowed(SUPPORT)
    @Override
    @SCJRestricted()
    protected Mission getNextMission() {
        return _mission;
    }
}
