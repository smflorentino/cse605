package javax.realtime;

abstract class ParameterBase implements java.lang.Cloneable {
    final int DEFAULT_INITIAL_ARRAY_SIZE = 8;
    public IdentityArraySet<Schedulable> schedulables;

    ParameterBase() {  }

    synchronized void register(Schedulable schedulable) { }

    synchronized void deregister(Schedulable schedulable) { }

    public final synchronized Object clone() { return null; }
}