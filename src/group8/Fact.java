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
	This calculation of factorial was taken from user Zed on Stack Overflow at
 	http://stackoverflow.com/questions/1481780/better-approximation-of-e-with-java
*/

public class Fact implements WorkTask, Serializable {

    private static final long serialVersionUID = 227L;
		private static final BigDecimal one = BigDecimal.ONE;
    private final int n;

    public Fact(int n) {
        this.n = n;
    }

    public BigDecimal doWork() {
        return computeFact(n);
    }
    
		//Compute factorial of input n
		//Note: returns 1 if n is non-positive
    public static BigDecimal computeFact(int n) {
    	BigDecimal fact = one;
      for(int i=1;i<=n;i++) {
		  	fact = fact.multiply(new BigDecimal(i));
			}
			return fact;
    }
}