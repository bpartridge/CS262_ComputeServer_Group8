package group8;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.UUID;

import edu.harvard.cs262.ComputeServer.ComputeServer;
import edu.harvard.cs262.ComputeServer.WorkQueue;
import edu.harvard.cs262.ComputeServer.WorkTask;

public class TestServerForWorker implements WorkQueue {

  private ComputeServer workerStub;

  @Override
  public UUID registerWorker(ComputeServer workerStub) throws RemoteException {
    this.workerStub = workerStub;
    return null;
  }

  @Override
  public boolean unregisterWorker(UUID workerID) throws RemoteException {
    return true;
  }
  
  public static void main(String args[]){
    try {
      if (System.getSecurityManager() == null) {
        System.setSecurityManager(new SecurityManager());
      }

      TestServerForWorker server = new TestServerForWorker();
      WorkQueue serverStub = (WorkQueue)UnicastRemoteObject.exportObject(server);
      
      Registry registry = LocateRegistry.getRegistry();
      registry.rebind("TestServerForWorker", serverStub); // rebind to avoid AlreadyBoundException
      
      System.out.println("Server ready");
    } catch (Exception e) {
      System.err.println("Server exception: " + e.toString());
    }
  }
}

