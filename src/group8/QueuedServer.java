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

	
	/*
	 * Dispatcher 
	 *
	 * An internal class of Queued Server which has the task of completing a specific
	 * client request. The Dispatcher does this in cooperation with the QueuedServer whic
	 * created it, as well as some number of Forwarders, which attempt to complete a task
	 * on a specific ComputeServer. It also registers its semaphore with a the Hashtable
	 * results on the parent QueuedServer.
	 *
	 * This Dispatcher is a Callable, and as such it is run in its own thread by the
	 * calling QueuedServer, which blocks until Dispatcher returns its value
	 *
	 * @author Holly Anderson, Brenton Partridge, and Ore Babarinsa
	 *
	 */
	private class Dispatcher implements Callable<Object> {
		//The parent queue server this 
		private QueuedServer qs;
		private WorkTask task;
		private Semaphore sem;

		/*
		 * Dispatcher Constructor
		 * A dispatcher should be constructed with the QueuedServer which constructed it
		 * as well as a valid WorkTask and Semaphore(with initially 0 count) on which
		 * it can wait
		 */
		public Dispatcher(QueuedServer queueServer, WorkTask task, Semaphore sem){
			this.qs = queueServer;
			this.task = task;
			this.sem = sem;
		}

		//Forwards work to a ComputeServer
		/*
		 * Forwarder
		 *
		 * This is an internal class of the Dispatcher, which actually attempts the remote execution
		 * in a different thread. It simply takes a given QueuedServer, which must be the parent of 
		 * both the Forwarder and the Dispatcher, a semaphore on which the Dispatcher promises to 
		 * wait, a server on which to execute the worker, and a task to execute on the server.
		 *
		 * After getting this arguments, the Forwarder than attempts a simple remote execution.
		 * If the remote execution is successful, we register the result with the QueuedServer
		 * and then hit release on the Dispatcher's semaphore. This means that the Dispatcher now
		 * wake up and return the computed value to the user
		 */
		private class Forwarder implements Callable<Object> {
			private Semaphore doneSem;
			private ComputeServer server;
			private WorkTask task;
			private QueuedServer queueServe;
			
			/*
			 * Constructor for Forwarder
			 * We take a QueuedServer to which to the calling Dispatcher is tied, a Semaphore on which
			 * the Dispatcher will wait, a worker to which to send the work, as well as a task to execute
			 */
			public Forwarder(QueuedServer qs, Semaphore sem, ComputeServer worker, WorkTask task){
				this.doneSem = sem;
				this.server = worker;
				this.task = task;
				this.queueServe = qs;
			}

			//Here were define the behavior of our Forwarder when it executes in another thread
			@Override
			public Object call () throws RemoteException {
				//We simply attempt the remote execution on the specified server
				Object res = this.server.sendWork(task);
				//If we get here, we didn't explode! Now we just register our results...
				synchronized(this.queueServe.results){
					this.queueServe.results.put(task, res);
				}
				//And wake up our Dispatcher
				this.doneSem.release();
				return res;

			}
	
		}

		//Our overriding of the Callable<Object> call method so do our work
		@Override
		public Object call(){
			//A Dispatcher, when called, will infinitely re-attempt a task until it is complete
			while(true){
				//We, in a synchronized manner, grab a workerID from our parent QueuedServer's list of free workers
				synchronized(this.qs.freeWorkers){
					while(this.qs.freeWorkers.isEmpty()){
						try{
							//We wait if there is no valid worker
							this.qs.freeWorkers.wait();
						}catch (Exception e){
						
						}
					}
					//We then pull off the actual workerID
					UUID workerID = this.qs.freeWorkers.removeFirst();
					//And then grab the associated ComputeServer instance
					ComputeServer server = this.qs.workers.get(workerID);
					try {
						//We attempt to ping the Server
						if(server.PingServer()){
							//Here we create a new Forwarder, who will actually attempt the remote execution
							Forwarder forward = new Forwarder(this.qs, this.sem, server, this.task);
							Future<Object> future = this.qs.executor.submit(forward);
							//We take our semaphore and register with our parent QueuedServer
							this.qs.sems.put(workerID, this.sem);
							//We also make sure that we place our worker in the array of busy workers
							this.qs.busyWorkers.add(workerID);
							//We block on the Semaphore until...
							this.sem.acquire();
							//We are AWOKEN, after which we examine our table of registered results
							Object res = this.qs.results.get(this.task);
							//If our future isn't done and we have null, that means our worker was canceled
							if(res == null && !future.isDone()){
								//We attempt to cancel the work
								//In the case that during our cancelation, the work finished, we return that
								boolean done = !future.cancel(true);
								if(done){
									//Here we re add our worker to our list of free workers
									this.qs.freeWorkers.add(workerID);
									//we notify somoone waiting for a worker that they can wake up
									this.qs.freeWorkers.notify();
									//We remove our semaphore from our parent server's hashtable
									this.qs.sems.remove(workerID);
									//we also dispose of the cached copy of our results
									this.qs.results.remove(this.task);
									//We lastly remove our worker from the list of busy workers
									this.qs.busyWorkers.remove(workerID);
									//No we give the user their answer
									return future.get();
								}
							} else {
								//Here, after succesfully getting back an answer, add the user to the free pool
								this.qs.freeWorkers.add(workerID);
								//notify any dispatchers waiting for a free worker
								this.qs.freeWorkers.notify();
								//remove our semaphore from our parent's servers lookup table
								this.qs.sems.remove(workerID);
								//We remove our cached copy of the results
								this.qs.results.remove(this.task);
								//We remove our worker from the list of busy servers
								this.qs.busyWorkers.remove(workerID);
								return res;
							}

						}
					//If the ping attempt fails, we remove the worker from our list of valid workers
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
			//Here, we make sure that we notify anyone waiting on a free worker
			freeWorkers.notify();
			return key;
		}
	}
	/*
	 * unregisterWorker
	 * This function removes a worker from the system. However, if that worker is currently 
	 * registered as performing some sort of tasking, we will find a semaphore on which 
	 * a Dispatcher is waiting. this method will then release that Semaphore, such that the
	 * Dispatcher can cancel the Forwarder communicating with the unregistered Worker
	 *
	 * @param workerID - a UUID associated with a worker currently bound to this QueuedServer
	 */
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
