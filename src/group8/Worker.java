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
      
      //String serverName = "ComputeServer";
      String serverName = args[1];
      Registry registry = LocateRegistry.getRegistry(args[0]);
      WorkQueue serverStub = (WorkQueue) registry.lookup(serverName);

      ComputeServer myWorker = new Worker();
      ComputeServer myWorkerStub = (ComputeServer)UnicastRemoteObject.exportObject(myWorker);
      UUID myWorkerID = serverStub.registerWorker(myWorkerStub);
      System.out.println("Worker registered.");    
      
      Scanner scan = new Scanner(System.in);
      boolean reg = true;
      while(true)
      {
        System.out.println("\n====== Menu ======");
        System.out.println("u: unregister\nr: register\n");
        String s = scan.next();
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
      //System.exit(0);

    } catch (Exception e) {
      System.err.println("Worker exception: " + e.toString());
    }
  }

}
