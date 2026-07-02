package agzam4.utils;

import arc.struct.Seq;

public class Seqs {

	public interface IntFunc<T> {
		
		public int get(T t);
		
	}

	public static <T, S> int binarySearch(Seq<T> a, IntFunc<T> c) {
    	int low = 0;
    	int high = a.size - 1;
    	while (low <= high) {
    		int mid = (low + high) >>> 1;
    		T midVal = a.get(mid);
    		int cmp = c.get(midVal);
    		if (cmp > 0)
    			low = mid + 1;
    		else if (cmp < 0)
    			high = mid - 1;
    		else
    			return mid; // key found
    	}
    	return -(low + 1);  // key not found.
    }
	
}
