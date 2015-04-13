package javax.realtime;

import java.util.Comparator;

public class AEHComparator implements Comparator{

    public AEHComparator(){};

    public int compare(Object o1,
		      Object o2){
	return compareAEH((AsyncEventHandler) o1,
			  (AsyncEventHandler) o2);

    }

    public int compareAEH(AsyncEventHandler handle1,
		          AsyncEventHandler handle2){

	int prio1 = handle1.getPriority();
	int prio2 = handle2.getPriority();
	if(handle1 == handle2)
	    return 0;
	if(prio1 <= prio2)
	    return 1;
	else 
	    return -1;
    }

    public boolean equals(Object obj){
	return false;
    }

}