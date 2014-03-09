package group8;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

import edu.harvard.cs262.ComputeServer.ComputeServer;
import edu.harvard.cs262.ComputeServer.WorkQueue;
import edu.harvard.cs262.ComputeServer.WorkTask;

public class WorkerV2 implements ComputeServer {

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
      
      String serverName = "ComputeServer";
      Registry registry = LocateRegistry.getRegistry();
      WorkQueue serverStub = (WorkQueue) registry.lookup(serverName);

      ComputeServer myWorker = new Worker();
      ComputeServer myWorkerStub = (ComputeServer)UnicastRemoteObject.exportObject(myWorker);
      serverStub.registerWorker(myWorkerStub);
      System.out.println("Worker registered");    
      
      //unregister option??
      
    } catch (Exception e) {
      System.err.println("Worker exception: " + e.toString());
    }
  }

}
