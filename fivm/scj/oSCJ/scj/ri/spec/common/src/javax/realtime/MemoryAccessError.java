package javax.realtime;

import java.io.Serializable;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed
public class MemoryAccessError extends RuntimeException implements Serializable {
	
	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public MemoryAccessError() {
	}

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public MemoryAccessError(String description) {
		super(description);
	}
}
