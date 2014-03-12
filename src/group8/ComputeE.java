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

public class ComputeE {
    public static void main(String args[]) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
			int port = 1099;
			if(args.length >= 3){
				port = Integer.parseInt(args[2]);
			}
            String name = args[1];
            Registry registry = LocateRegistry.getRegistry(args[0], port);
            ComputeServer comp = (ComputeServer) registry.lookup(name);
			int digits = 10;
			if(args.length >= 4){
				digits = Integer.parseInt(args[3]);
			}
            EApprox task = new EApprox(digits);
            Object res = comp.sendWork(task);
            System.out.println(res);
        } catch (Exception e) {
            System.err.println("ComputePi exception:");
            e.printStackTrace();
        }
    }    
}
