package group8;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.harvard.cs262.ComputeServer.ComputeServer;
import edu.harvard.cs262.ComputeServer.WorkQueue;
import edu.harvard.cs262.ComputeServer.WorkTask;


public class QueuedServer implements ComputeServer, WorkQueue {
	private static final int NTHREADS = 32;
	private Hashtable<UUID, ComputeServer> workers;
	private Hashtable<WorkTask, Object> results;
	private LinkedList<UUID> freeWorkers, busyWorkers;
	private Hashtable<UUID, Object> responses;
	private ExecutorService executor;
	private Hashtable<UUID, Semaphore> sems;
	private QueuedServer(){
		super();
		workers = new Hashtable<UUID, ComputeServer>();
		sems = new Hashtable<UUID, Semaphore>();
		freeWorkers = new LinkedList<UUID>();
		busyWorkers = new LinkedList<UUID>(); 
		executor = Executors.newFixedThreadPool(NTHREADS);
		results = new Hashtable<WorkTask, Object>();

	}

	
	//Finds an open Worker and dispatches a task out to it
	/*
	 * Dispatcher 
	 */
	private class Dispatcher implements Callable<Object> {
		//The parent queue server this 
		private QueuedServer qs;
		private WorkTask task;
		private Semaphore sem;
		public Dispatcher(QueuedServer queueServer, WorkTask task, Semaphore sem){
			this.qs = queueServer;
			this.task = task;
			this.sem = sem;
		}

		//Forwards work to a ComputeServer
		private class Forwarder implements Callable<Object> {
			private Semaphore doneSem;
			private ComputeServer server;
			private WorkTask task;
			private QueuedServer queueServe;
			
			//Create our Forwarder
			public Forwarder(QueuedServer qs, Semaphore sem, ComputeServer worker, WorkTask task){
				this.doneSem = sem;
				this.server = worker;
				this.task = task;
				this.queueServe = qs;
			}

			//Make our remote call 
			public Object call () throws RemoteException {
				Object res = this.server.sendWork(task);
				synchronized(this.queueServe.results){
					this.queueServe.results.put(task, res);
				}
				this.doneSem.release();
				return res;

			}
	
		}

		//run our actuall callable
		public Object call(){
			while(true){
				synchronized(this.qs.freeWorkers){
					while(this.qs.freeWorkers.isEmpty()){
						try{
							this.qs.freeWorkers.wait();
						}catch (Exception e){
						
						}
					}
					UUID workerID = this.qs.freeWorkers.removeFirst();
					ComputeServer server = this.qs.workers.get(workerID);
					try {
						if(server.PingServer()){
							Forwarder forward = new Forwarder(this.qs, this.sem, server, this.task);
							Future<Object> future = this.qs.executor.submit(forward);
							this.qs.sems.put(workerID, this.sem);
							this.qs.busyWorkers.add(workerID);
							this.sem.acquire();
							Object res = this.qs.results.get(this.task);
							if(res == null && !future.isDone()){
								boolean done = !future.cancel(true);
								if(done){
									this.qs.freeWorkers.add(workerID);
									this.qs.freeWorkers.notify();
									this.qs.sems.remove(workerID);
									this.qs.results.remove(this.task);
									this.qs.busyWorkers.remove(workerID);
									return future.get();
								}
							} else {

								this.qs.freeWorkers.add(workerID);
								this.qs.freeWorkers.notify();
								this.qs.sems.remove(workerID);
								this.qs.results.remove(this.task);
								this.qs.busyWorkers.remove(workerID);
								return res;
							}

						}
						
					} catch (Exception e){
						this.qs.workers.remove(workerID);
						this.qs.busyWorkers.remove(workerID);
					}
									
				}

			}
		}
	}


	
	@Override
	public UUID registerWorker(ComputeServer server) throws RemoteException {
		synchronized(freeWorkers){
			UUID key = UUID.randomUUID();
			workers.put(key, server);
			freeWorkers.add(key);
			freeWorkers.notify();
			return key;
		}
	}

	@Override
	public boolean unregisterWorker(UUID workerID) throws RemoteException{
		synchronized(freeWorkers){
			if (null == workers.get(workerID)){
				return true;
			}
			Semaphore sem = this.sems.get(workerID);
			if(sem != null){
				sem.release();
			}
		
			workers.remove(workerID);
			freeWorkers.remove(workerID);
			busyWorkers.remove(workerID);
			return true;
		}
	}
		
	@Override
	public Object sendWork(WorkTask work) throws RemoteException {
		Object res = null;
		Semaphore sem = new Semaphore(0);
		Dispatcher disp = new Dispatcher(this, work, sem);
		Future<Object> future = executor.submit(disp);
		try {
			res = future.get();
		}catch (Exception e){
			
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
	  int port = 1099;
	  if(args.length >= 3){
	  	port = Integer.parseInt(args[2]);
	  }
      String serverName = args[1];
      Registry registry = LocateRegistry.getRegistry(args[0], port);
      registry.rebind(serverName, serverStub); // rebind to avoid AlreadyBoundException
      System.out.println("Server ready");
    } catch (Exception e) {
      System.err.println("Server exception: " + e.toString());
    }
  }

}
