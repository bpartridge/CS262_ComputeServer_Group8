


public class Worker implements ComputeServer {

  

  public static void main(String args[]) {
    String serverName = "Server";
    Registry registry = LocateRegistry.getRegistry(args[0]);
    WorkQueue serverStub = (WorkQueue) registry.lookup(name);

    ComputeServer myWorker = new Worker();
    ComputeServer myWorkerStub = (ComputeServer)UnicastRemoteObject.exportObject(myWorker);

    serverStub.registerWorker(myWorkerStub);
  }

}