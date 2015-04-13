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
package javax.realtime;

import java.util.Iterator;

//import edu.purdue.scj.utils.Utils;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(LEVEL_1)
public class AsyncEvent extends AbstractAsyncEvent  {
	/**
	 * The set of handlers associated with this event. This is package access as
	 * some async events are used internally by the implementation and access to
	 * them is controlled via other means - hence we don't want to have to use
	 * the public API's.
	 */
	final IdentityArraySet handlers = new IdentityArraySet();

	/** The lock used to protect access to the handler list */
	final Object lock = handlers;

	public AsyncEvent() {
	}

	public void addHandler(AsyncEventHandler handler) {
		if (handler != null)
				handlers.add(handler); // ignores duplicates
	}

	public void removeHandler(AsyncEventHandler handler) {
		synchronized (lock) {
			handlers.remove(handler); // deals with null
		}
	}

	public boolean handledBy(AsyncEventHandler handler) {
			return handlers.contains(handler);
	}

	public void setHandler(AsyncEventHandler handler) {
			handlers.clear();
			if (handler != null)
				handlers.add(handler);
	}

	// // TODO: check semantics of the method
	// public ReleaseParameters createReleaseParameters() {
	// return new AperiodicParameters(null, null, null, null);
	// }

	// TODO: there is happening definition now, check spec
	public void bindTo(String happening) throws UnknownHappeningException {
		throw new UnknownHappeningException("Unsupported happening");
	}

	// TODO: see above
	public void unbindTo(String happening) throws UnknownHappeningException {
		throw new UnknownHappeningException(
				"AsyncEvent not bound to this happening");
	}

	// TODO: throws MITViolationException, ArrivalTimeQueueOverflowException
	@SCJAllowed(LEVEL_1)
	public void fire() {
			for (Iterator i = handlers.iterator(); i.hasNext();) {
				((AsyncEventHandler) i.next()).releaseHandler();
			}
	}
}
