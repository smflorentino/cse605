package javax.realtime;

import java.util.*;

public class AbsoluteTime extends HighResolutionTime {

    // private static long getMillisNonNull(AbsoluteTime absoluteTime) { return 0; }

    //private static long getMillisNonNull(Date date) { return 0; }

    public AbsoluteTime() {
        nanos=0;
        millis=0;
    }

    public AbsoluteTime(AbsoluteTime absoluteTime) {  }

    public AbsoluteTime(AbsoluteTime absoluteTime, Clock clock) {  }

    public AbsoluteTime(Clock clock) {  }

    public AbsoluteTime(Date date) {  }

    public AbsoluteTime(Date date, Clock clock) {  }

    public AbsoluteTime(long l, int i) {
        millis = l;
        nanos = i;
    }



    public AbsoluteTime(long l, int i, Clock clock) {  }

    public AbsoluteTime absolute(Clock clock) { return null; }

    public AbsoluteTime absolute(Clock clock, AbsoluteTime absoluteTime) { return null; }

    public AbsoluteTime add(long l, int i) { return null; }

    public AbsoluteTime add(long l, int i, AbsoluteTime absoluteTime) { return null; }

    public AbsoluteTime add(RelativeTime relativeTime) { return null; }

    public AbsoluteTime add(RelativeTime relativeTime, AbsoluteTime absoluteTime) { return null; }

    public Date getDate() { return null; }

    public RelativeTime relative(Clock clock) { return null; }

    public RelativeTime relative(Clock clock, RelativeTime relativeTime) { return null; }

    public void set(Date date) {  }

    public RelativeTime subtract(AbsoluteTime absoluteTime) { return null; }

    public RelativeTime subtract(AbsoluteTime absoluteTime, RelativeTime relativeTime) { return null; }

    public AbsoluteTime subtract(RelativeTime relativeTime) { return null; }

    public AbsoluteTime subtract(RelativeTime relativeTime, AbsoluteTime absoluteTime) { return null; }

    public java.lang.String toString() { return null; }

    AbsoluteTime(long l) {  }

    RelativeTime subtract(long l, int i, RelativeTime relativeTime) { return null; }

}