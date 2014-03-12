package group8;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.math.BigDecimal;
import java.math.*;
import java.io.Serializable;

import edu.harvard.cs262.ComputeServer.ComputeServer;
import edu.harvard.cs262.ComputeServer.WorkQueue;
import edu.harvard.cs262.ComputeServer.WorkTask;
import group8.*;


/* this calculation of an approximation of e was taken from user Zed on
 http://stackoverflow.com/questions/1481780/better-approximation-of-e-with-java
*/

public class EApprox implements WorkTask, Serializable {

    private static final long serialVersionUID = 227L;
		private static final BigDecimal one = BigDecimal.ONE;
    private final int digits;

    public EApprox(int digits) {
        this.digits = digits;
    }

    /*
     * Calculate e.
     */
    public BigDecimal doWork() {
        return computeE(digits);
    }

    public static BigDecimal computeE(int digits) {
    	BigDecimal fact = one;
    	BigDecimal e = one;
      for(int i=1;i<digits;i++) {
		  	fact = fact.multiply(new BigDecimal(i));
		  	e = e.add(BigDecimal.ONE.divide(fact, new MathContext(10000, RoundingMode.HALF_UP)));
			}
			return e;
    }
}