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

/* 
	This approximation of e was taken from user Zed on Stack Overflow at
 	http://stackoverflow.com/questions/1481780/better-approximation-of-e-with-java
*/

public class EApprox implements WorkTask, Serializable {

    private static final long serialVersionUID = 227L;
		private static final BigDecimal one = BigDecimal.ONE;
    private final int n;

    public EApprox(int n) {
        this.n = n;
    }

    public BigDecimal doWork() {
        return computeE(n);
    }
    
		//Compute approximation of e as the sum of 1/k! from k=0 to n
		//Note: appromxiation is calculated to 999 decimal places
		//Note: returns 1 if n is non-positive
    public static BigDecimal computeE(int n) {
    	BigDecimal fact = one;
    	BigDecimal e = one;
    	MathContext mc = new MathContext(1000, RoundingMode.HALF_UP);
      for(int i=1;i<=n;i++) {
		  	fact = fact.multiply(new BigDecimal(i));
		  	e = e.add(BigDecimal.ONE.divide(fact, mc), mc);
			}
			return e;
    }
}