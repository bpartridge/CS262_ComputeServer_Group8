package group8;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.UUID;

import edu.harvard.cs262.ComputeServer.ComputeServer;
import edu.harvard.cs262.ComputeServer.WorkQueue;
import edu.harvard.cs262.ComputeServer.WorkTask;

public class QueuedServer implements ComputeServer, WorkQueue {

	private Hashtable<UUID, ComputeServer> workers;
	private LinkedList<UUID> freeWorkers, busyWorkers;
	
	private QueuedServer(){
		super();
		workers = new Hashtable<UUID, ComputeServer>();
		freeWorkers = new LinkedList<UUID>();
		busyWorkers = new LinkedList<UUID>();
	}
	
	@Override
	public UUID registerWorker(ComputeServer server) throws RemoteException {
		UUID key = UUID.randomUUID();
		workers.put(key, server);
		freeWorkers.add(key);
		return key;
	}

	@Override
	public boolean unregisterWorker(UUID workerID) throws RemoteException{
		if (null == workers.get(workerID)){
			return true;
		}
		
		workers.remove(workerID);
		freeWorkers.remove(workerID);
		busyWorkers.remove(workerID);
		return true;
	}
		
	@Override
	public Object sendWork(WorkTask work) throws RemoteException {
		Object res = null;
		
		if(freeWorkers.peekFirst() != null) {
			UUID workerID = freeWorkers.removeFirst();			
			busyWorkers.addLast(workerID);
			ComputeServer myWorker = workers.get(workerID);

			try{				
				if(myWorker.PingServer()){   
	    		res = myWorker.sendWork(work);
	    		freeWorkers.addLast(workerID);
    			busyWorkers.remove(workerID);
	    	}
    	}catch(RemoteException e){
    		  System.err.println("RemoteException: can't ping worker");
    		  this.unregisterWorker(workerID);
    	}
		}
		
		return res;
	}

	@Override
	public boolean PingServer() throws RemoteException {
		return true;
	}

  public static void main(String args[]){
    try {
      if (System.getSecurityManager() == null) {
        System.setSecurityManager(new SecurityManager());
      }

      QueuedServer server = new QueuedServer();
      ComputeServer serverStub = (ComputeServer)UnicastRemoteObject.exportObject(server);
      
      String serverName = args[1];
      Registry registry = LocateRegistry.getRegistry(args[0]);
      registry.rebind(serverName, serverStub); // rebind to avoid AlreadyBoundException
      System.out.println("Server ready");
    } catch (Exception e) {
      System.err.println("Server exception: " + e.toString());
    }
  }

}
