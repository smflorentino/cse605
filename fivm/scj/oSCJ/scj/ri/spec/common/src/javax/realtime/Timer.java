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

/**
 * SCJ timers will always be allocated in Mission Memory. They thus do not need
 * to worry about the liveness of the memory scope where they are allocated in.
 * So we remove the memory related code.
 */
public abstract class Timer extends AsyncEvent {

    // /*
    // * Implementation Notes:
    // *
    // * This implementation uses a state-based approach to mirror the spec
    // * directly. This makes it much simpler to see that we are correct with
    // * respect to the specification - otherwise we are constantly converting
    // * between the spec form and the code and that can lead to errors.
    // *
    // * The 1.0 implementation used a thread per timer which was quite a bit of
    // * overhead. With this design we wanted to provide a much more efficient
    // * mechanism. The basic idea was to provide two threads for all timers: -
    // * one to handle absolute timers, and - one to handle relative timers
    // *
    // * For absolute timers it is easy enough to maintain a list of timers
    // * ordered by their next firing time (targetTime as the spec calls it).
    // The
    // * thread performs an absolute timed blocking operation (sleep or wait)
    // that
    // * matches the timer at the head of the queue. If the head changes then
    // the
    // * thread is interrupted, or otherwise woken and it re-assesses its
    // * situation.
    // *
    // * Although this only uses one thread for all timers, the downside was
    // that
    // * it greatly complicated synchronization between the timer thread and
    // * client threads acting on the timers themselves. The basic problem is
    // that
    // * timer methods woud lock the timer first before locking the queue, while
    // * the timer thread would have to lock the queue before it could grab an
    // * individual timer and lock it. To alleviate this problem we have to
    // divide
    // * the responsibility for updating timer state between the timer methods
    // and
    // * the timer thread methods that operated on the queue:
    // *
    // * - all timer methods that mutate state synchronize on the lock object
    // * inherited from AsyncEvent - access to the timer thread queues is
    // * protected by a per-queue lock, via the methods of the timer thread. -
    // the
    // * timer fields needed by the queue is protected by the queue lock not
    // just
    // * the timer lock. To modify these fields (targetTime or durationTime) you
    // * need to acquire the timer lock then the queue lock. To read the fields
    // * you need to hold either the timer lock or the queue lock. - When the
    // * timer threads wake up they will acquire their queue lock first, grab
    // the
    // * head, release the queue lock and grab the timer lock.
    // *
    // *
    // * For relative timers we considered two approaches initially.
    // *
    // * The first idea was to have a queue of timers just as in the absolute
    // * case. The thread performs a relative timed blocking operation based on
    // * the head of the queue and if the queue changes we unblock the thread.
    // * Unfortunately, in contrast to the absolute timer case, this doesn't
    // work.
    // * The problem is that while the timer thread processes elapsed timers,
    // time
    // * will elapse. If we don't know how much time will elapse then we don't
    // * know how to adjust the next waiting time. Even if we try to measure the
    // * passage of time, that too takes time and the timers will not fire at
    // the
    // * right time.
    // *
    // * The second idea was to have the timer thread wait for the SIG_ALRM
    // signal
    // * and then update each timer in the queue (or use a delta queue to reduce
    // * the overhead). This seems feasible but the overhead is rather scary,
    // * particularly with sub millisecond clock tick rates. So that idea was
    // * shelved. If we had a dedicated programmable timer that we could use
    // then
    // * that would be a differnet matter, but we don't.
    // *
    // * Now one of the issues of absolute versus relative timers is that, by
    // * general convention it is conceded that absolute timeouts should be
    // * responsive to changes in the system clock, while relative changes
    // should
    // * not. Unfortunately, this requires two seperate timing mechanisms on a
    // * system. In OVM, as of this writing, we only have the system clock to
    // give
    // * us absolute time, or SIG_ALRM to allow us to count relative time
    // * ourselves. As use of SIG_ALRM has already been discounted then we're
    // * stuck. Note that this issue also works against using the current time
    // to
    // * try and keep track of how much time elapses while processing the
    // timers.
    // *
    // * So given that we can't do relative times properly, without introducing
    // * excessive overhead, where does that leave us:
    // *
    // * a) we use a relative sleep and queue, as previously described and try
    // to
    // * account for elapsed time by querying the current time; or
    // *
    // * b) we just convert relative times to absolute ones (which is what we
    // did
    // * in the 1.0 implementation).
    // *
    // * Persuing (a), we need to keep track of when each entry goes into the
    // * queue so that we can calculate the elapsed time correctly, but that
    // * effectively reduces to converting the waiting time to an absolute time,
    // * hence we really only have the option of doing (b).
    // *
    // * The way we do (b) is to note that the absolute trigger time of a
    // relative
    // * timer is durationTime+startTime, so we order the queue based on that
    // * value, and we compare that value to the current time. When we actually
    // * trigger a timer we set countingTime=now()-startTime, and that allows
    // * countingTime and durationTime to be compared directly. Further, we
    // don't
    // * need to adjust for clock resolution when doing this. To account for
    // * periodic relative timers we actually use a referenceTime which is
    // either
    // * the startTime or the last fire time.
    // *
    // *
    // * The downside of having a shared timer thread is extra synchronization
    // * that occurs, and due to that the additional context switching due to
    // * priority inheritance. For example, whenever a timer updates the head of
    // * the timer queue it must hold the lock and then interrupts the timer
    // * thread. This preempts the current thread and the timer thread then
    // finds
    // * the queue lock held, so it bequeaths its priority to the current
    // thread,
    // * which then runs until it releases the queue lock, at which point the
    // * timer thread gets the lock and has a chance to re-evaluate the timer
    // * queue. If by chance the new head timer should be fired then we have a
    // * similar to-and-fro as the timer thread blocks due to the current thread
    // * holding the timer lock. Note that the queue lock must be held when the
    // * timer thread is interrupted, otherwise the timer thread could get out
    // of
    // * sync with the timer queue.
    // *
    // * David Holmes - October 12, 2004
    // */
    //
    // private static int nextID = 0;
    //
    // // constants for each state
    // static final int DESTROYED = 0, NA_D_A = 1, // not-active, disabled,
    // // absolute
    // NA_D_R = 2, // not-active, disabled, relative
    // A_D_A = 3, // active, disabled, absolute
    // A_D_R = 4, // active, disabled, relative
    // A_E_A = 5, // active, enabled, absolute
    // A_E_R = 6; // active, enabled, relative
    //
    // /** Our absolute timer thread */
    // static AbsoluteTimerThread absThread;
    //
    // /** Our relative timer thread */
    // static RelativeTimerThread relThread;
    //
    // final int id; // for debugging
    //
    // /** the current state of this timer */
    // int currentState = -1;
    //
    // /**
    // * is this a periodic timer?
    // */
    // boolean periodic;
    //
    // /**
    // * Our period (asuming we're periodic)
    // */
    // long period;
    //
    // /**
    // * Were we last scheduled as an absolute timer? This corresponds to the
    // spec
    // * variable <tt>last_rescheduled_with_AbsoluteTime</tt>
    // */
    // boolean lastWasAbsolute;
    //
    // // variables for absolute mode of operation
    //
    // /**
    // * The next absolute time at which this timer should fire when it is
    // active
    // * and enabled
    // */
    // long nextTargetTime;
    //
    // /**
    // * The next absolute time at which this timer will fire. This field can
    // only
    // * be written when the timer lock and the absolute timer thread queue lock
    // * is held. It can be read when either of those locks are held.
    // */
    // long targetTime;
    //
    // // the 'currentTime' is always read from the clock
    //
    // // variables for relative mode of operation
    //
    // /**
    // * The next duration to elapse before an active, and enabled timer will
    // * fire.
    // */
    // long nextDurationTime;
    //
    // /**
    // * The duration to wait for before this timer will fire. This field can
    // only
    // * be written when the timer lock and the relative timer thread queue lock
    // * is held. It can be read when either of those locks are held.
    // */
    // long durationTime;
    //
    // /**
    // * The current 'count' of elapsed nanoseconds, updated on each tick
    // */
    // long countingTime;
    //
    // // general variables
    //
    // /** the time (absolute or relative) of our initial firing or skipping */
    // long startTime;
    //
    // /** Have we fired or skipped in this activation? */
    // boolean firedOrSkipped;
    //
    // /**
    // * The reference time for the current firing of a relative timer. This has
    // * the value startTime+nT where T is the period and n=0, 1 etc This field
    // * can only be written when the timer lock and the relative timer thread
    // * queue lock is held. It can be read when either of those locks are held.
    // */
    // long referenceTime;
    //
    // /** A link for the absolute timer queue */
    // Timer next;
    //
    // /** The clock associated with this timer */
    // Clock clock;
    //
    // /**
    // * Flag to signify whether this is the first time this timer has been
    // * started, and hence may need to adjust the scoped memory reference count
    // */
    // boolean firstStart = true;
    //
    // private static synchronized int getNextID() {
    // return nextID++;
    // }
    //
    // static {
    // absThread = new AbsoluteTimerThread();
    // absThread.setDaemon(true);
    // absThread.start();
    //
    // relThread = new RelativeTimerThread();
    // relThread.setDaemon(true);
    // relThread.start();
    // }
    //
    // /**
    // * Create a timer that fires according to the given <code>time</code>,
    // based
    // * on the {@link Clock} <code>clock</code> and is handled by the specified
    // * {@link AsyncEventHandler} <code>handler</code>.
    // *
    // * @param time
    // * The time used to determine when to fire the event. A
    // * <code>time</code> value of <code>null</code> is equivalent to
    // * a <code>RelativeTime</code> of 0, and in this case the
    // * <code>Timer</code> fires immediately upon a call to
    // * <code>start()</code>.
    // *
    // * @param clock
    // * The clock on which to base this timer, overriding the clock
    // * associated with the parameter <code>time</code>. If
    // * <code>null</code>, the system <code>Realtime clock</code> is
    // * used.
    // *
    // * @param handler
    // * The default <code>handler</code> to use for this event. If
    // * <code>null</code>, no <code>handler</code> is associated with
    // * the timer and nothing will happen when this event fires unless
    // * a <code>handler</code> is subsequently associated with the
    // * timer using the <code>addHandler()</code> or
    // * <code>setHandler()</code> method.
    // *
    // * @throws IllegalArgumentException
    // * Thrown if <code>time</code> is a negative
    // * <code>RelativeTime</code> value.
    // */
    // protected Timer(HighResolutionTime time, Clock clock,
    // AsyncEventHandler handler) {
    //
    // if (clock != null && clock != Clock.rtc)
    // throw new IllegalArgumentException("Unsupported Clock");
    //
    // this.clock = Clock.rtc;
    // id = getNextID();
    //
    // if (time instanceof AbsoluteTime) {
    // if (time.isNegative()) {
    // // not illegal, just a time before the epoch, but we
    // // can't just convert toNanos if it is negative
    // nextTargetTime = 0;
    // } else {
    // nextTargetTime = time.toNanos();
    // }
    // lastWasAbsolute = true;
    // currentState = NA_D_A;
    // } else {
    // lastWasAbsolute = false;
    // if (time == null) {
    // nextDurationTime = 0;
    // } else {// if it's anything other than Relativetime we're hosed ;-)
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // nextDurationTime = time.toNanos();
    // }
    // currentState = NA_D_R;
    // }
    //
    // addHandler(handler);
    //
    // // NOTE: we don't do gotoState here but require the subclass
    // // constructor to do that once construction is properly completed,
    // // as immediate firing complicates things. We simply set currentState
    // // so that the subclass constructor can do: gotoState(currentState)
    // }
    //
    // public String toString() {
    // return "Timer-" + id;
    // }
    //
    // /**
    // * Create a {@link ReleaseParameters} block appropriate to the timing
    // * characteristics of this event. The default is the most pessimistic:
    // * {@link AperiodicParameters}. This is typically called by code that is
    // * setting up a <code>handler</code> for this event that will fill in the
    // * parts of the release parameters for which it has values, e.g. cost.
    // *
    // * @return A newly created {@link ReleaseParameters} object.
    // *
    // * @throws IllegalStateException
    // * Thrown if this <code>Timer</code> has been <em>destroyed</em>
    // * .
    // */
    // public ReleaseParameters createReleaseParameters() {
    // synchronized (lock) {
    // checkDestroyed();
    // return super.createReleaseParameters();
    // }
    // }
    //
    // /**
    // * Disable this timer, preventing it from firing. It may subsequently be
    // re-
    // * <em>enabled</em>. If the timer is <em>disabled</em> when its fire time
    // * occurs then it will not fire. However, a <em>disabled</em> timer
    // created
    // * using an instance of <code>RelativeTime</code> for its time parameter
    // * continues to count while it is <em>disabled</em>, and no changes take
    // * place in a <em>disabled</em> timer created using an instance of
    // * <code>AbsoluteTime</code>, in both cases the potential firing is simply
    // * masked, or skipped. If the timer is subsequently re-<em>enabled</em>
    // * before its fire time and it is <em>enabled</em> when its fire time
    // * occurs, then it will fire. It is important to note that this method
    // does
    // * not delay the time before a possible firing. For example, if the timer
    // is
    // * set to fire at time 42 and the <code>disable()</code> is called at time
    // * 30 and <code>enable()</code> is called at time 40 the firing will occur
    // * at time 42 (not time 52). These semantics imply also that firings are
    // not
    // * queued. Using the above example, if enable was called at time 43 no
    // * firing will occur, since at time 42 <code>this</code> was
    // * <em>disabled</em>.
    // *
    // * If the <code>Timer</code> is not <em>enabled</em>, or is not active,
    // this
    // * method does nothing.
    // *
    // * @throws IllegalStateException
    // * Thrown if this <code>Timer</code> has been <em>destroyed</em>
    // * .
    // */
    // public void disable() {
    // synchronized (lock) {
    // checkDestroyed();
    // switch (currentState) {
    // case NA_D_A:
    // case NA_D_R:
    // case A_D_A:
    // case A_D_R:
    // break;
    // case A_E_A: {
    // gotoState(A_D_A);
    // break;
    // }
    // case A_E_R: {
    // gotoState(A_D_R);
    // break;
    // }
    // default:
    // throw new InternalError("Unexpected state: " + currentState);
    // }
    // }
    // }
    //
    // /**
    // * Re-enable this timer after it has been <em>disabled</em>. (See
    // * {@link Timer#disable()}.)
    // *
    // * If the <code>Timer</code> is not <em>disabled</em>, or is not active,
    // * this method does nothing.
    // *
    // * @throws IllegalStateException
    // * Thrown if this <code>Timer</code> has been <em>destroyed</em>
    // * .
    // */
    // public void enable() {
    // synchronized (lock) {
    // checkDestroyed();
    // switch (currentState) {
    // case NA_D_A:
    // case NA_D_R:
    // case A_E_A:
    // case A_E_R:
    // break;
    // case A_D_A: {
    // gotoState(A_E_A);
    // break;
    // }
    // case A_D_R: {
    // gotoState(A_E_R);
    // break;
    // }
    // default:
    // throw new InternalError("Unexpected state: " + currentState);
    // }
    // }
    // }
    //
    // /**
    // * Stop <code>this</code> from counting or comparing if <em>active</em>,
    // and
    // * release as many of its resources as possible back to the system. Every
    // * method invoked on a <code>Timer</code> that has been <em>destroyed</em>
    // * will throw <code>IllegalStateException</code>.
    // */
    // public void destroy() {
    // boolean wasRel = false;
    // boolean wasAbs = false;
    // synchronized (lock) {
    // checkDestroyed();
    // switch (currentState) {
    // case NA_D_A:
    // case NA_D_R:
    // break;
    // case A_E_A:
    // case A_D_A:
    // wasAbs = true;
    // break;
    // case A_E_R:
    // case A_D_R:
    // wasRel = true;
    // break;
    // default:
    // throw new InternalError("Unexpected state: " + currentState);
    // }
    // gotoState(DESTROYED);
    // }
    // // only one thread can get here
    //
    // handlers.clear();
    //
    // if (wasAbs)
    // absThread.remove(this);
    // else if (wasRel)
    // relThread.remove(this);
    // }
    //
    // /**
    // * Return the instance of {@link Clock} on which this timer is based.
    // *
    // * @return The instance of {@link Clock} associated with this
    // * <code>Timer</code>.
    // *
    // *
    // * @throws IllegalStateException
    // * Thrown if this <code>Timer</code> has been <em>destroyed</em>
    // * .
    // */
    // public Clock getClock() {
    // synchronized (lock) {
    // checkDestroyed();
    // return clock;
    // }
    // }
    //
    // /**
    // * Get the time at which this <code>Timer</code> is expected to fire. If
    // the
    // * <code>Timer</code> is <em>disabled</em>, the returned time is that of
    // the
    // * skipping of the firing. If the <code>Timer</code> is
    // <em>not-active</em>
    // * it throws <code>IllegalStateException</code>.
    // *
    // * @return An instance of {@link AbsoluteTime} object representing the
    // * absolute time at which <code>this</code> is expected to fire or
    // * to skip. If the timer has been created or re-scheduled (see
    // * {@link Timer#reschedule(HighResolutionTime time)}) using an
    // * instance of <code>RelativeTime</code> for its time parameter then
    // * it will return the sum of such time and the start time. The clock
    // * association of the returned time is the clock on which
    // * <code>this</code> timer is based.
    // *
    // * @throws IllegalStateException
    // * Thrown if this <code>Timer</code> has been <em>destroyed</em>
    // * , or if it is not <em>active</em>.
    // * @throws ArithmeticException
    // * Thrown if computation of the fire time gives an overflow
    // * after normalization.
    // */
    // public AbsoluteTime getFireTime() {
    // synchronized (lock) {
    // checkDestroyed();
    // switch (currentState) {
    // case NA_D_A:
    // case NA_D_R:
    // throw new IllegalStateException("timer not active");
    // case A_E_A:
    // case A_D_A:
    // return new AbsoluteTime(targetTime);
    // case A_E_R:
    // case A_D_R:
    // return new AbsoluteTime(referenceTime + durationTime);
    // default:
    // throw new InternalError("Unexpected state: " + currentState);
    // }
    // }
    // }
    //
    // /**
    // * Tests this to determine if <code>this</code> is <em>active</em> and is
    // * <em>enabled</em> such that when the given time occurs it will fire the
    // * event. Given the <code>Timer</code> current state it answer the
    // question
    // * <em>"Is firing expected?".</em>
    // *
    // * @return <code>true</code> if the timer is <em>active</em> and
    // * <em>enabled</em>; <code>false</code>, if the timer has either not
    // * been <em>started</em>, it has been <em>started</em> but it is
    // * <em>disabled</em>, or it has been <em>started</em> and is now
    // * <em>stopped.</em>
    // *
    // * @throws IllegalStateException
    // * Thrown if this <code>Timer</code> has been <em>destroyed</em>
    // * .
    // */
    // public boolean isRunning() {
    // synchronized (lock) {
    // checkDestroyed();
    // return (currentState == A_E_A || currentState == A_E_R);
    // }
    // }
    //
    // /**
    // * Changes the initial firing time (or skipping time) for this timer. If
    // * this timer is not active, or is active but has not yet fired (or
    // skipped)
    // * in the current activation, then the initial firing (or skipping) time
    // is
    // * determined as-if <code>time</code> had been passed to the constructor
    // of
    // * this timer. Otherwise, this method determines the initial firing (or
    // * skipping) time to be used when this timer is stopped and then
    // re-started.
    // *
    // * @param time
    // * The time used to reschedule the initial firing (or skipping)
    // * of this timer. If <code>time</code> is <code>null</code> then
    // * there is no change to the initial firing (or skipping) time.
    // *
    // * @throws IllegalStateException
    // * Thrown if this <code>Timer</code> has been <em>destroyed</em>
    // * .
    // * @throws IllegalArgumentException
    // * Thrown if <code>time</code> is a negative
    // * <code>RelativeTime</code> value.
    // */
    // public void reschedule(HighResolutionTime time) {
    // synchronized (lock) {
    // checkDestroyed();
    // switch (currentState) {
    // // note: if time is null we fall through doing nothing
    // case NA_D_A: {
    // if (time instanceof AbsoluteTime) {
    // nextTargetTime = time.toNanos();
    // lastWasAbsolute = true;
    // } else if (time != null) {
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // nextDurationTime = time.toNanos();
    // lastWasAbsolute = false;
    // gotoState(NA_D_R);
    // }
    // break;
    // }
    // case NA_D_R: {
    // if (time instanceof AbsoluteTime) {
    // nextTargetTime = time.toNanos();
    // lastWasAbsolute = true;
    // gotoState(NA_D_A);
    // } else if (time != null) {
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // nextDurationTime = time.toNanos();
    // lastWasAbsolute = false;
    // }
    // break;
    // }
    // case A_E_A: {
    // if (!firedOrSkipped) {
    // if (time instanceof AbsoluteTime) {
    // // this will write to targetTime
    // absThread.reschedule(this, time.toNanos());
    // gotoState(currentState);
    // } else if (time != null) {
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // absThread.remove(this);
    // // this will write to referenceTime and durationTime
    // relThread.add(this, startTime, time.toNanos());
    // gotoState(A_E_R);
    // }
    // } else {
    // if (time instanceof AbsoluteTime) {
    // nextTargetTime = time.toNanos();
    // lastWasAbsolute = true;
    // } else if (time != null) {
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // nextDurationTime = time.toNanos();
    // lastWasAbsolute = false;
    // }
    // }
    // break;
    // }
    // case A_E_R: {
    // if (!firedOrSkipped) {
    // if (time instanceof AbsoluteTime) {
    // relThread.remove(this);
    // // this will write to targetTime
    // absThread.add(this, time.toNanos());
    // gotoState(A_E_A);
    // } else if (time != null) {
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // // this will write to referenceTime and durationTime
    // relThread.reschedule(this, startTime, time.toNanos());
    // gotoState(currentState);
    // }
    // } else {
    // if (time instanceof AbsoluteTime) {
    // nextTargetTime = time.toNanos();
    // lastWasAbsolute = true;
    // } else if (time != null) {
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // nextDurationTime = time.toNanos();
    // lastWasAbsolute = false;
    // }
    // }
    // break;
    // }
    // case A_D_A: {
    // if (!firedOrSkipped) {
    // if (time instanceof AbsoluteTime) {
    // // this will write to targetTime
    // absThread.reschedule(this, time.toNanos());
    // gotoState(currentState);
    // } else if (time != null) {
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // absThread.remove(this);
    // // this will write to referenceTime and durationTime
    // relThread.add(this, startTime, time.toNanos());
    // gotoState(A_D_R);
    // }
    // } else {
    // if (time instanceof AbsoluteTime) {
    // nextTargetTime = time.toNanos();
    // lastWasAbsolute = true;
    // } else if (time != null) {
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // nextDurationTime = time.toNanos();
    // lastWasAbsolute = false;
    // }
    // }
    // break;
    // }
    // case A_D_R: {
    // if (!firedOrSkipped) {
    // if (time instanceof AbsoluteTime) {
    // relThread.remove(this);
    // // this will write to targetTime
    // absThread.add(this, time.toNanos());
    // gotoState(A_D_A);
    // } else if (time != null) {
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // // this will write to referenceTime and durationTime
    // relThread.reschedule(this, startTime, time.toNanos());
    // gotoState(currentState);
    // }
    // } else {
    // if (time instanceof AbsoluteTime) {
    // nextTargetTime = time.toNanos();
    // lastWasAbsolute = true;
    // } else if (time != null) {
    // if (time.isNegative())
    // throw new IllegalArgumentException("negative time");
    // nextDurationTime = time.toNanos();
    // lastWasAbsolute = false;
    // }
    // }
    // break;
    // }
    // default:
    // throw new InternalError("Unexpected state: " + currentState);
    // }
    // }
    // }
    //
    // /**
    // * Start this timer. A timer starts measuring time from when it is
    // started;
    // * this method makes the timer <em>active</em> and <em>enabled.</em>
    // *
    // * @throws IllegalStateException
    // * Thrown if this <code>Timer</code> has been <em>destroyed</em>
    // * , or if this timer is already <em>active.</em>
    // */
    // public void start() {
    // start(false);
    // }
    //
    // /**
    // * Start this timer. A timer starts measuring time from when it is
    // started.
    // * If <code>disabled</code> is <code>true</code> start the timer making it
    // * <em>active</em> in a <em>disabled</em> state. If <code>disabled</code>
    // is
    // * <code>false</code> this method behaves like the <code>start()</code>
    // * method.
    // *
    // * @param disabled
    // * If <code>true</code>, the timer will be <em>active</em> but
    // * <em>disabled</em> after it is started. If <code>false</code>
    // * this method behaves like the <code>start()</code> method.
    // *
    // * @throws IllegalStateException
    // * Thrown if this <code>Timer</code> has been <em>destroyed</em>
    // * , or if this timer is already <em>active</em>.
    // *
    // * @since 1.0.1
    // */
    // public void start(boolean disabled) {
    // synchronized (lock) {
    // // only two states that don't throw IllegalStateException are
    // // these two, and they both have the same behavior
    // if (currentState == NA_D_A || currentState == NA_D_R) {
    // startTime = RealtimeClock.getCurrentTimeNanos();
    // if (lastWasAbsolute) {
    // // this will write to targetTime
    // absThread.add(this, nextTargetTime);
    // if (disabled)
    // gotoState(A_D_A);
    // else
    // gotoState(A_E_A);
    // } else {
    // countingTime = 0;
    // // this will write to referenceTime and durationTime
    // relThread.add(this, startTime, nextDurationTime);
    // if (disabled)
    // gotoState(A_D_R);
    // else
    // gotoState(A_E_R);
    // }
    // } else
    // throw new IllegalStateException("already active");
    //
    // if (firstStart)
    // firstStart = false;
    // }
    // }
    //
    // /**
    // * Stops a timer if it is <em>active</em> and changes its state to
    // * <em>not-active</em> and <em>disabled</em>.
    // *
    // * @return <code>true</code> if <code>this</code> was <em>active</em> and
    // * <code>false</code> otherwise.
    // *
    // * @throws IllegalStateException
    // * Thrown if this <code>Timer</code> has been <em>destroyed</em>
    // * .
    // */
    // public boolean stop() {
    // synchronized (lock) {
    // if (ABS_DEBUG || REL_DEBUG)
    // pln(this + " stop: acquired lock");
    // checkDestroyed();
    // switch (currentState) {
    // case NA_D_A:
    // case NA_D_R:
    // if (ABS_DEBUG || REL_DEBUG)
    // pln(this + " stop: not active");
    // return false;
    // case A_E_A:
    // case A_D_A:
    // if (ABS_DEBUG)
    // pln(this + " stop: abs.remove()");
    // absThread.remove(this);
    // if (false)
    // pln(this + " stop: abs.remove() done");
    // gotoState(NA_D_A);
    // return true;
    // case A_E_R:
    // case A_D_R:
    // if (REL_DEBUG)
    // pln(this + " stop: rel.remove()");
    // relThread.remove(this);
    // if (REL_DEBUG)
    // pln(this + " stop: rel.remove() done");
    // gotoState(NA_D_R);
    // return true;
    // default:
    // throw new InternalError("Unexpected state: " + currentState);
    // }
    // }
    // }
    //
    // /**
    // * Should not be called. The fire method is reserved for the use of the
    // * timer.
    // *
    // * @throws UnsupportedOperationException
    // * Thrown if <code>fire</code> is called from outside the
    // * <code>Timer</code> implementation.
    // *
    // * @since 1.0.1 Throws <code>UnsupportedOperationException</code> instead
    // of
    // * doing unspecified damage.
    // */
    // public void fire() {
    // throw new UnsupportedOperationException("Can't fire() a Timer");
    // }
    //
    // /**
    // * Throw IllegalStateException if this timer has been destroyed. This
    // should
    // * only be called whehn the timer lock is held.
    // */
    // void checkDestroyed() {
    // if (currentState == DESTROYED)
    // throw new IllegalStateException("Timer destroyed");
    // }
    //
    // /**
    // * Perform a state transition. This should only be called when the timer
    // * lock is held.
    // */
    // void gotoState(int nextState) {
    // currentState = nextState;
    // switch (nextState) {
    // case NA_D_A: {
    // if (periodic)
    // firedOrSkipped = false;
    // break;
    // }
    // case NA_D_R: {
    // if (periodic)
    // firedOrSkipped = false;
    // break;
    // }
    // case A_E_A: {
    // long now = RealtimeClock.getCurrentTimeNanos();
    // if (now >= targetTime) {
    // if (ABS_DEBUG)
    // pln(this + " Fire time detected");
    // if (periodic) {
    // if (period > 0) {
    // fireInternal();
    // firedOrSkipped = true;
    // selfReschedule();
    // gotoState(currentState);
    // } else {
    // /* + */absThread.remove(this);
    // fireInternal();
    // gotoState(NA_D_A);
    // }
    // } else { // oneshot
    // /* + */absThread.remove(this);
    // fireInternal();
    // gotoState(NA_D_A);
    // }
    // } else {
    // if (ABS_DEBUG)
    // pln(this + " too soon to fire: " + now);
    // }
    // break;
    // }
    // case A_E_R: {
    // if (countingTime >= durationTime) {
    // if (REL_DEBUG)
    // pln(this + " time to fire");
    //
    // if (periodic) {
    // if (period > 0) {
    // fireInternal();
    // firedOrSkipped = true;
    // selfReschedule();
    // gotoState(currentState);
    // } else {
    // /* + */relThread.remove(this);
    // fireInternal();
    // gotoState(NA_D_R);
    // }
    // } else { // oneshot
    // /* + */relThread.remove(this);
    // fireInternal();
    // gotoState(NA_D_R);
    // }
    // }
    // break;
    // }
    // case A_D_A: {
    // if (RealtimeClock.getCurrentTimeNanos() >= targetTime) {
    // if (periodic) {
    // if (period > 0) {
    // // skipped
    // firedOrSkipped = true;
    // selfReschedule();
    // gotoState(currentState);
    // } else {
    // // skip
    // /* + */absThread.remove(this);
    // gotoState(NA_D_A);
    // }
    // } else { // oneshot
    // // skip
    // /* + */absThread.remove(this);
    // gotoState(NA_D_A);
    // }
    // }
    // break;
    // }
    // case A_D_R: {
    // if (countingTime >= durationTime) {
    // if (periodic) {
    // if (period > 0) {
    // // skipped
    // firedOrSkipped = true;
    // selfReschedule();
    // gotoState(currentState);
    // } else {
    // // skip
    // /* + */relThread.remove(this);
    // gotoState(NA_D_R);
    // }
    // } else { // oneshot
    // // skip
    // /* + */relThread.remove(this);
    // gotoState(NA_D_R);
    // }
    // }
    // break;
    // }
    // case DESTROYED: {
    // // do nothing here except set state
    // break;
    // }
    // default:
    // throw new InternalError("Invalid next state: " + nextState);
    // }
    // }
    //
    // /**
    // * Test method that returns either the expected fire time or the time at
    // * which we last fired if we already fired and are not periodic
    // */
    // AbsoluteTime getExpectedFireTime() {
    // synchronized (lock) {
    // checkDestroyed();
    // switch (currentState) {
    // case NA_D_A:
    // return new AbsoluteTime(nextTargetTime);
    // case A_E_A:
    // case A_D_A:
    // return new AbsoluteTime(targetTime);
    // case NA_D_R:
    // if (firstStart)
    // throw new IllegalStateException("timer never started");
    // return new AbsoluteTime(referenceTime + nextDurationTime);
    // case A_E_R:
    // case A_D_R:
    // return new AbsoluteTime(referenceTime + durationTime);
    //
    // default:
    // throw new InternalError("Unexpected state: " + currentState);
    // }
    // }
    // }
    //
    // // override other asyncevent methods to throw if destroyed
    //
    // /**
    // * Do a self reschedule. This only has meaning for periodic timers and so
    // is
    // * overridden by PeriodicTimer. This is only called with the current lock
    // * held
    // */
    // void selfReschedule() {
    // throw new InternalError("timer.selfReschedule invoked");
    // }
    //
    // /**
    // * Hook into the event firing mechanism. This is only called with the
    // * current lock held.
    // *
    // */
    // void fireInternal() {
    // super.fire();
    // }
    //
    // final String getState() {
    // switch (currentState) {
    // case NA_D_A:
    // return "NA_D_A";
    // case NA_D_R:
    // return "NA_D_R";
    // case A_E_A:
    // return "A_E_A";
    // case A_D_A:
    // return "A_D_A";
    // case A_E_R:
    // return "A_E_R";
    // case A_D_R:
    // return "A_D_R";
    // case DESTROYED:
    // return "DESTROYED";
    // default:
    // return "<unknown>-" + currentState;
    // }
    // }
    //
    // /**
    // * Triggers the firing of this timer by the timer thread. Because of the
    // * inherent race between this and other actions, whether the timer will
    // * actually fire depends on its state after the lock is acquired.
    // Depending
    // * on the current state we may need to update the countingTime. Simply
    // * re-entering the current state takes care of everything else.
    // *
    // */
    // final void trigger() {
    // synchronized (lock) {
    // if (ABS_DEBUG || REL_DEBUG)
    // pln("trigger invoked in state " + getState());
    // switch (currentState) {
    // case A_D_R:
    // case A_E_R:
    // countingTime = RealtimeClock.getCurrentTimeNanos()
    // - referenceTime;
    // if (REL_DEBUG)
    // pln("trigger set counting Time to " + countingTime);
    // break;
    // case DESTROYED:
    // return;
    // }
    //
    // gotoState(currentState);
    // }
    // }
    //
    // // keystroke savers :-)
    //
    // static void pln(String s) {
    // System.out.println(s);
    // }
    //
    // static void p(String s) {
    // System.out.print(s);
    // }
    //
    // // Our thread helper classes follow
    //
    // static final boolean ABS_DEBUG = false;
    //
    // static final String TRIGGER_EXCEPTION = "Exception invoking trigger()";
    // static final String OOME = "OutOfMemoryError printing stacktrace";
    //
    // static final boolean REL_DEBUG = false;
    //
    // /**
    // * Special thread that manages all absolute timers. Timers are stored in a
    // * queue based on their target time for trigerring. Timers are
    // automatically
    // * removed from the queue when they are triggered.
    // */
    // static class AbsoluteTimerThread extends RealtimeThread.VMThread {
    //
    // AbsoluteTimerThread() {
    // setName("AbsoluteTimer-Thread");
    // }
    //
    // // queue of linked Timers sorted by targetTime: smallest at head
    // // Timer's may be scope allocated so no barriers
    // Timer head = null;
    //
    // Object queueLock = new Object();
    //
    // /**
    // * Add a timer to the queue based on the given targetTime. Equal
    // * targetTimes always get added to the end to preserve the relative
    // * order of threads of the same priority (different priorities will be
    // * handled by the run queue)
    // */
    // void add(Timer t, long targetTime) {
    // synchronized (queueLock) {
    // t.targetTime = targetTime;
    // if (head == null || targetTime < head.targetTime) {
    // t.next = head;
    // head = t;
    // interrupt();
    // } else {
    // Timer curr = head;
    // while (curr.next != null) {
    // if (curr.next.targetTime > targetTime) {
    // // insert after curr
    // t.next = curr.next;
    // curr.next = t;
    // return;
    // }
    // curr = curr.next;
    // }
    // // insert at end
    // curr.next = t;
    // }
    // }
    // }
    //
    // /** Try to remove t from this queue */
    // boolean remove(Timer t) {
    // synchronized (queueLock) {
    // if (t == null || head == null) {
    // return false;
    // }
    // if (head == t) {
    // head = head.next;
    // t.next = null;
    // interrupt();
    // return true;
    // } else {
    // for (Timer prev = head, curr = head.next; curr != null; prev = curr, curr
    // = curr.next) {
    // if (curr == t) {
    // prev.next = curr.next;
    // t.next = null; // unlink
    // return true;
    // }
    // }
    // }
    // return false;
    // }
    // }
    //
    // /**
    // * Reorder the given timer in the queue based on the new targetTime
    // */
    // boolean reschedule(Timer t, long targetTime) {
    // synchronized (queueLock) {
    // // FIXME: do this more efficiently without potentially
    // // doing multiple interrupts and lots of locking
    //
    // // it is possible this timer has just been removed from the
    // // queue pending the call to trigger, which is blocked due
    // // to this timer reschedule call. This timer reschedule will
    // // complete first - adding the timer back into this queue -
    // // then trigger will proceed based on the new state of the
    // // timer.
    // if (remove(t)) {
    // add(t, targetTime);
    // return true;
    // } else {
    // add(t, targetTime);
    // return false;
    // }
    // }
    // }
    //
    // // returns a queue of timers that need triggering
    // Timer processHead(long currentTime) {
    // Timer retList = null;
    // Timer curr = head;
    // while (curr != null && curr.targetTime <= currentTime) {
    // // unlink first
    // head = curr.next;
    // curr.next = null;
    // if (retList == null)
    // retList = curr;
    // else {
    // curr.next = retList;
    // retList = curr;
    // }
    // curr = head;
    // }
    // return retList;
    // }
    //
    // public void run() {
    // long sleepTime = 0;
    //
    // Timer triggerList = null;
    //
    // while (true) {
    // synchronized (queueLock) {
    // if (head == null)
    // sleepTime = Long.MAX_VALUE;
    // else
    // sleepTime = head.targetTime;
    // }
    //
    // // we have highest priority so should get to block before any
    // // other thread can acquire the queue lock
    //
    // int rc = RealtimeJavaDispatcher.sleepAbsoluteRaw(sleepTime);
    // // by the time we can get the lock the head may have changed.
    // // But in that case we will have been interrupted before we
    // // can reacquire the lock.
    // synchronized (queueLock) {
    // // always evaluate Thread.interrupted to clear the
    // // interrupt state
    // if (rc == RealtimeJavaDispatcher.ABSOLUTE_INTERRUPTED
    // | Thread.interrupted()) {
    //
    // // we may have a new head. Rather than work that out
    // // we just loop around again knowing that the sleep
    // // will return immediately if needed.
    // } else if (rc == RealtimeJavaDispatcher.ABSOLUTE_PAST) {
    // triggerList = processHead(sleepTime);
    // } else {
    // triggerList = processHead(sleepTime);
    // }
    // } // release queue lock
    //
    // // now process the trigger list
    // while (triggerList != null) {
    // Timer timer = triggerList;
    // triggerList = triggerList.next;
    // timer.next = null; // unlink
    // try {
    // timer.trigger();
    // } catch (Throwable t) {
    // System.out.println(TRIGGER_EXCEPTION);
    // try {
    // t.printStackTrace();
    // } catch (OutOfMemoryError oome) {
    // System.out.println(OOME);
    // }
    // }
    // }
    // }
    // }
    // }
    //
    // /**
    // * Special thread that manages all relative timers. Timers are stored in a
    // * queue based on their duration time for triggering. Timers are
    // * automatically removed from the queue when they are triggered.
    // * <p>
    // * Timers are stored in the queue based on the absolute value of their
    // * durationTime, which is the durationTime plus the "reference time" of
    // the
    // * timer. The "reference time" is either the startTime, for the first
    // firing
    // * or skipping, or the last fire time for a periodic timer. When we
    // trigger
    // * a timer its countingTime is set to now()-referenceTime
    // */
    // static class RelativeTimerThread extends RealtimeThread.VMThread {
    //
    // RelativeTimerThread() {
    // setName("RelativeTimer-Thread");
    // }
    //
    // // queue of linked Timers sorted by durationTime: smallest at head
    // // May be scope allocated so no barriers
    // Timer head = null;
    //
    // Object queueLock = new Object();
    //
    // /**
    // * Add a timer to the queue based on the given referenceTime and
    // * durationTime. The fields of the timer are set once we hold the queue
    // * lock. Equal times always get added to the end to preserve the
    // * relative order of threads of the same priority (different priorities
    // * will be handled by the run queue)
    // */
    // void add(Timer t, long referenceTime, long durationTime) {
    // synchronized (queueLock) {
    // t.durationTime = durationTime;
    // t.referenceTime = referenceTime;
    // if (head == null
    // || (durationTime + referenceTime) < (head.durationTime +
    // head.referenceTime)) {
    // t.next = head;
    // head = t;
    // interrupt();
    // } else {
    // Timer curr = head;
    // while (curr.next != null) {
    // if ((curr.next.durationTime + curr.next.referenceTime) > (durationTime +
    // t.referenceTime)) {
    // // insert after curr
    // t.next = curr.next;
    // curr.next = t;
    // return;
    // }
    // curr = curr.next;
    // }
    // // insert at end
    // curr.next = t;
    // }
    // }
    // }
    //
    // /** Try to remove t from this queue */
    // boolean remove(Timer t) {
    // synchronized (queueLock) {
    // if (t == null || head == null) {
    // return false;
    // }
    // if (head == t) {
    // head = head.next;
    // t.next = null;
    // interrupt();
    // return true;
    // } else {
    // for (Timer prev = head, curr = head.next; curr != null; prev = curr, curr
    // = curr.next) {
    // if (curr == t) {
    // prev.next = curr.next;
    // t.next = null; // unlink
    // return true;
    // }
    // }
    // }
    //
    // return false;
    // }
    // }
    //
    // /**
    // * Reorder the given timer in the queue based on the new referenceTime
    // * and durationTime
    // */
    // boolean reschedule(Timer t, long referenceTime, long durationTime) {
    //
    // synchronized (queueLock) {
    // // FIXME: do this more efficiently without
    // // doing multiple interrupts and lots of locking
    //
    // // it is possible this timer has just been removed from the
    // // queue pending the call to trigger, which is blocked due
    // // to this timer reschedule call. This timer reschedule will
    // // complete first - adding the timer back into this queue -
    // // then trigger will proceed based on the new state of the
    // // timer.
    // if (remove(t)) {
    // add(t, referenceTime, durationTime);
    // return true;
    // } else {
    // add(t, referenceTime, durationTime);
    // return false;
    // }
    // }
    // }
    //
    // // returns a queue of timers that need triggering
    // Timer processHead(long currentTime) {
    // Timer retList = null;
    // Timer curr = head;
    // while (curr != null
    // && (curr.durationTime + curr.referenceTime) <= currentTime) {
    // // unlink first
    // head = curr.next;
    // curr.next = null;
    //
    // if (retList == null)
    // retList = curr;
    // else {
    // curr.next = retList;
    // retList = curr;
    // }
    // curr = head;
    // }
    // return retList;
    // }
    //
    // public void run() {
    // long sleepTime = 0;
    //
    // Timer triggerList = null;
    //
    // while (true) {
    // synchronized (queueLock) {
    // if (head == null)
    // sleepTime = Long.MAX_VALUE;
    // else
    // sleepTime = head.durationTime + head.referenceTime;
    // }
    //
    // // we have highest priority so should get to block before any
    // // other thread can acquire the queue lock
    //
    // int rc = RealtimeJavaDispatcher.sleepAbsoluteRaw(sleepTime);
    // // by the time we can get the lock the head may have changed.
    // // But in that case we will have been interrupted before we
    // // can reacquire the lock.
    // synchronized (queueLock) {
    // // always evaluate Thread.interrupted to clear the
    // // interrupt state
    // if (rc == RealtimeJavaDispatcher.ABSOLUTE_INTERRUPTED
    // | Thread.interrupted()) {
    //
    // // we may have a new head. Rather than work that out
    // // we just loop around again knowing that the sleep
    // // will return immediately if needed.
    // } else if (rc == RealtimeJavaDispatcher.ABSOLUTE_PAST) {
    //
    // triggerList = processHead(sleepTime);
    // } else {
    // triggerList = processHead(sleepTime);
    // }
    // } // release queue lock
    //
    // // now process the trigger list
    // while (triggerList != null) {
    // try {
    // triggerList.trigger();
    // } catch (Throwable t) {
    // System.out.println(TRIGGER_EXCEPTION);
    // try {
    // t.printStackTrace();
    // } catch (OutOfMemoryError oome) {
    // System.out.println(OOME);
    // }
    // }
    // triggerList = triggerList.next;
    // }
    // }
    // }
    // }
}
