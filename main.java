import java.util.concurrent.*;
import java.util.*;

class ClaimGateway{
    //submit_claims()
    //getBatchSize()
}
class PaymentGateway{}

//BrokerThread
class BrokerThread extends Thread{
    private int thread_num;
    private int min_claims;
    private int max_claims;
    private int new_claims;
    private int total_days;
    private ArrayList<ClaimGateway> gate;
    private ArrayList<Integer> split_claims = new ArrayList();
    private CyclicBarrier barrier;

    //get thread_num,min_claims,max_claims from read file in main
    public BrokerThread(String n,int thread,int min,int max,int days,ArrayList<ClaimGateway> g){
        super(n);
        thread_num = thread;
        min_claims = min;
        max_claims = max;
        total_days = days;
        gate = g;
    }

    public void setBarrier(CyclicBarrier ba)    { barrier = ba;}

    public void NewClaims(){
        Random rand = new Random();
        new_claims = rand.nextInt(min_claims,max_claims+1);
        System.out.printf("%15s >> new claims = %5d",Thread.currentThread().getName(),new_claims);
        SplitClaim();
    }

    public void SplitClaim(){

        split_claims.clear();
        int remain = new_claims;

        int batch_size = gate.get(0).getBatchSize(); //add function getBatchSize() in class ClaimsGateway

        while(remain > batch_size){
            split_claims.add(batch_size);
            remain -= batch_size;
        }
        if(remain > 0){
            split_claims.add(remain);
        }
        
        System.out.println("split to " + split_claims);
    }

    public void ClaimSubmission(){
        Random rand = new Random();
        int index = rand.nextInt(gate.size());
        ClaimGateway current_gate = gate.get(index);
        
        //update claim function --> from class ClaimGateway
        for(int claims : split_claims){
            current_gate.submit_claims(claims);
        }
    }

    public void run(){
        for(int day = 1; day <= total_days; day++){
            try{
                barrier.await();
                
                NewClaims();
                barrier.await();

                ClaimSubmission();
                barrier.await();
            }
            catch(InterruptedException | BrokenBarrierException e)  { break; }
        }

    }

}