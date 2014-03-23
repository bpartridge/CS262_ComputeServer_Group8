package group8;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.UUID;
import java.util.Scanner;

import edu.harvard.cs262.ComputeServer.ComputeServer;
import edu.harvard.cs262.ComputeServer.WorkQueue;
import edu.harvard.cs262.ComputeServer.WorkTask;

public class Worker implements ComputeServer {

  public Object sendWork(WorkTask work) throws RemoteException {
  	//Print verification when sendWork has been called
    System.out.println("sendWork called on worker");
    return work.doWork();
  }

  public boolean PingServer() throws RemoteException {
    return true;
  }

  public static void main(String args[]) {
    try {
      if (System.getSecurityManager() == null) {
        System.setSecurityManager(new SecurityManager());
      }

	  	//Look up appropriate registry and server using inputs
	  	// args[0]: IP (registry)
			// args[1]: Server name
			// args[2]: Port (registry)
      String serverName = args[1];
      int port = Integer.parseInt(args[2]);
      Registry registry = LocateRegistry.getRegistry(args[0], port);
      WorkQueue serverStub = (WorkQueue)registry.lookup(serverName);

			//Create new Worker and ComputeServer stub for the new worker
      ComputeServer myWorker = new Worker();
      ComputeServer myWorkerStub = (ComputeServer)UnicastRemoteObject.exportObject(myWorker);
      
      //Register worker and store corresponding UUID
      UUID myWorkerID = serverStub.registerWorker(myWorkerStub);
      System.out.println("Worker registered.");    
      
      //Create new Scanner
      Scanner scan = new Scanner(System.in);
      
      //Create boolean to keep track of whether the Worker is registered
      boolean reg = true;
      
      //Allow Worker to register and unregister itself (when appropriate) using keyboard input
      while(true)
      {
      	//Menu
        System.out.println("\n====== Menu ======");
        System.out.println("u: unregister\nr: register\n");
        String s = scan.next();
        
        //Allow worker to unregister if currently registered
        if(s.equals("u"))
        {
          if(reg)
          {
            serverStub.unregisterWorker(myWorkerID);
            reg = false;
            System.out.println("Worker unregistered.");
          }
          else
          {
            System.out.println("Error: not registered.");
          }
        }
        
        //Allow worker to register if currently unregistered
        else if(s.equals("r"))
        {
          if(!reg)
          {
            myWorkerID = serverStub.registerWorker(myWorkerStub);
            reg = true;
            System.out.println("Worker registered.");    
          }
          else
          {
            System.out.println("Error: already registered.");
          }
        }
      }

    } catch (Exception e) {
      System.err.println("Worker exception: " + e.toString());
    }
  }

}
