package group8;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.math.BigDecimal;

import edu.harvard.cs262.ComputeServer.ComputeServer;
import edu.harvard.cs262.ComputeServer.WorkQueue;
import edu.harvard.cs262.ComputeServer.WorkTask;
import group8.*;

/* 
	This client code was edited from the version on the RMI tutorial at
	http://docs.oracle.com/javase/tutorial/rmi/
*/

public class ComputeFact {
    public static void main(String args[]) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
        		//Inputs
        		// args[0]: IP (registry)
						// args[1]: Server name
						// args[2]: Port (registry)
						// args[3]: parameter for Fact
        	  String name = args[1];
						int port = Integer.parseInt(args[2]);						
						int n = Integer.parseInt(args[3]);

						//Look up appropriate registry and server	using inputs
            Registry registry = LocateRegistry.getRegistry(args[0], port);
            ComputeServer comp = (ComputeServer)registry.lookup(name);
			
						//Create new Fact object
            Fact task = new Fact(n);
            
            //Send work to appropriate Server
            Object res = comp.sendWork(task);
            
            //Print result of task execution
            System.out.println(res);
        } catch (Exception e) {
            System.err.println("ComputeFact exception:");
            e.printStackTrace();
        }
    }    
}
