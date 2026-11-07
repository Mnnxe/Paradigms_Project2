/*
********** group members *****************
6813115	    Renuka      Choosingkae
6813238 	Napat       Chonsaktrakul
6813248 	Pimpichcha  Saengngoen
6813258 	Sittichok   Boonthittanont
*/
package Project2_6813115;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

//////////////////////////////////////////////////////////////////////////////////////////////////

class BrokerThread extends Thread{
    private int min_claims;
    private int max_claims;
    private int new_claims;
    private int total_days;
    //private string split;
    private ArrayList<ClaimGateway> gate;
    private ArrayList<Integer> split_claims = new ArrayList<>();
    private CyclicBarrier barrier;

    //get min_claims,max_claims from read file in main
    public BrokerThread(String n,int min,int max,int days,ArrayList<ClaimGateway> g){
        super(n);
        min_claims = min;
        max_claims = max;
        total_days = days;
        gate = g;
    }

    public void setBarrier(CyclicBarrier ba)    { barrier = ba;}
    public String toString()                    { return getName(); }

    public void NewClaims(){
        Random rand = new Random();
        new_claims = rand.nextInt(min_claims,max_claims+1);
        SplitClaim();
        System.out.printf("%16s >> new claims = %5d   split to %s%n",Thread.currentThread().getName(),new_claims,split_claims);
        //SplitClaim();
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
        /*System.out.printf("%3s", ' ');
        System.out.println("split to " + split_claims);*/
    }

    public void ClaimSubmission(){
        Random rand = new Random();
        int index = rand.nextInt(gate.size());
        ClaimGateway current_gate = gate.get(index);
        
        //update claim function --> from class ClaimGateway
        /*for(int claims : split_claims){
            current_gate.submit_claims(claims);
        }*/
        
        for (int claims : split_claims) {
            synchronized (current_gate) {
                current_gate.submit_claims(claims);
                System.out.printf("%16s >> submit %3d claims to ClaimGateway_%d   ClaimGateway_%d pending claims = %5d%n",
                        Thread.currentThread().getName(), claims, current_gate.getId(), current_gate.getId(), current_gate.getPendingClaims());
            }  
        }
    }

    public void run(){
        for(int day = 1; day <= total_days; day++){
            try{
                barrier.await(); //brokers & assessors for the new day
                
                NewClaims();
                barrier.await(); //brokers NewClaims()

                ClaimSubmission();
                barrier.await(); //brokers ClaimSubmission()
                barrier.await(); //main empty line
                
                barrier.await(); //Assessors ClaimApproval()
                barrier.await(); //Assessors ShowPendingPayments()
                barrier.await(); //Assessors PaymentIssue()
            }
            catch(InterruptedException | BrokenBarrierException e)  { break; }
        }
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////

class AssessorThread extends Thread {
    private int total_days;
    private int daily_capacity;
    private ArrayList<ClaimGateway> claim_gates;
    private ArrayList<PaymentGateway> payment_gates;
    private CyclicBarrier barrier;
    private Random rand = new Random();

    private int pending_payments = 0;      // accumulated from previous days
    private int successful_payments = 0;   // used in the summary report
    private int approved_today = 0;

    // daily capacity comes from config file via main
    public AssessorThread(String n, int days, int capacity,
                          ArrayList<ClaimGateway> cg, ArrayList<PaymentGateway> pg) {
        super(n);
        total_days = days;
        daily_capacity = capacity;
        claim_gates = cg;
        payment_gates = pg;
    }

    public void setBarrier(CyclicBarrier ba)    { barrier = ba; }
    public int getSuccessfulPayments()          { return successful_payments; }
    public String toString()                    { return getName(); }

    // 5.2 claim approvals
    private void ClaimApproval() {
        int index = rand.nextInt(claim_gates.size());      // log on to only 1 gateway
        ClaimGateway gate = claim_gates.get(index);

        synchronized (gate) {      // approve + print together, so the pending number is correct
            approved_today = gate.approveClaims(daily_capacity);
            System.out.printf("%16s >> approve %3d claims from ClaimGateway_%d   ClaimGateway_%d pending claims = %5d%n",
                    Thread.currentThread().getName(), approved_today, gate.getId(), gate.getId(), gate.getPendingClaims());
        }
    }

    // 5.3 update and show pending payments
    private void ShowPendingPayments() {
        pending_payments += approved_today;
        System.out.printf("%16s >> pending payments = %3d%n",
                Thread.currentThread().getName(), pending_payments);
    }

    // 5.4 payment issues
    private void PaymentIssue() {
        int index = rand.nextInt(payment_gates.size());    // log on to only 1 gateway
        PaymentGateway gate = payment_gates.get(index);

        synchronized (gate) {
            int issued = gate.issuePayments(pending_payments);  // as many tokens as possible
            pending_payments -= issued;                          // unpaid ones go to next day
            successful_payments += issued;
            System.out.printf("%16s >> issue %3d payments to PaymentGateway_%d   PaymentGateway_%d tokens = %3d%n",
                    Thread.currentThread().getName(), issued, gate.getId(), gate.getId(), gate.getRemainingTokens());
        }
    }

    public void run() {
        for (int day = 1; day <= total_days; day++) {
            try {
                barrier.await(); //brokers & assessors for the new day
                barrier.await(); //brokers completed NewClaims()
                barrier.await(); //brokers completed ClaimSubmission()
                barrier.await(); //main empty line

                ClaimApproval();
                barrier.await(); //Assessors ClaimApproval()
                
                ShowPendingPayments();
                barrier.await(); //Assessors ShowPendingPayments()
                
                if (pending_payments != 0) {
                    PaymentIssue();
                }
                barrier.await(); //Assessors PaymentIssue()
            }
            catch (InterruptedException | BrokenBarrierException e) { break; }
        }
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////

public class Proj2Main {

    // objects
    private ArrayList<ClaimGateway> allClaimGateways = new ArrayList<>();
    
    private ArrayList<PaymentGateway> allPaymentGateways = new ArrayList<>();
    
    private ArrayList<BrokerThread> allBrokerThread = new ArrayList<>();
    
    private ArrayList<AssessorThread> allAssessorThread = new ArrayList<>();
    
    private int day;
    private int num_claimgateway;
    private int batch_size;
    private int num_paygateway;
    private int token;
    private int num_brokerthread;
    private int min_claim;
    private int max_claim;
    private int num_assesthread;
    private int capacity;
    
    private CyclicBarrier barrier;

    public static void main(String[] args) {
        Proj2Main mainApp = new Proj2Main();
        mainApp.readFile();
        mainApp.initializeObjects();
        mainApp.first_report();
        mainApp.setupBarrier();
        mainApp.runSimulation();
    }

    //Read config file and parse values
    public void readFile() {
        String path = "src/main/java/Project2_6813115/";
        String filename = "config.txt";
        Scanner fileScan = null;
        Scanner scan = new Scanner(System.in);
        
        while (fileScan == null) {
            try {
                fileScan = new Scanner(new File(path + filename));
            }
            catch (FileNotFoundException e) {
                System.err.println(e);
                System.out.print("New file name = ");
                filename = scan.next();
            }
        }
         
        int inputcount = 0;
        while (fileScan.hasNextLine()) {
            String line = fileScan.nextLine();
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String[] cols = line.split(","); 
            parseConfigLine(inputcount, cols);
            inputcount++;
        }
        fileScan.close();
    }

    public void parseConfigLine(int inputline, String[] cols) {
        switch (inputline) {
            case 0:
                day = Integer.parseInt(cols[1].trim());
                break;
            case 1:
                num_claimgateway = Integer.parseInt(cols[1].trim());
                batch_size = Integer.parseInt(cols[2].trim());
                break;
            case 2:
                num_paygateway = Integer.parseInt(cols[1].trim());
                token = Integer.parseInt(cols[2].trim());
                break;
            case 3:
                num_brokerthread = Integer.parseInt(cols[1].trim());
                min_claim = Integer.parseInt(cols[2].trim());
                max_claim = Integer.parseInt(cols[3].trim());
                break;
            case 4:
                num_assesthread = Integer.parseInt(cols[1].trim());
                capacity = Integer.parseInt(cols[2].trim());
                break;
        }
    }

    //Initialize lists once after config file is read
    public void initializeObjects() {
        for (int i = 0; i < num_claimgateway; i++) {
            allClaimGateways.add(new ClaimGateway(i, batch_size));
        }

        for (int i = 0; i < num_paygateway; i++) {
            allPaymentGateways.add(new PaymentGateway(i, token));
        }

        for (int i = 0; i < num_brokerthread; i++) {
            allBrokerThread.add(new BrokerThread("BrokerThread_" + i, min_claim, max_claim, day, allClaimGateways));
        }

        for (int i = 0; i < num_assesthread; i++) {
            allAssessorThread.add(new AssessorThread("AssessorThread_" + i, day, capacity, allClaimGateways, allPaymentGateways));
        }
    }

    public void setupBarrier() { 
        int totalThreads = 1 + allBrokerThread.size() + allAssessorThread.size();
        barrier = new CyclicBarrier(totalThreads);

        for (BrokerThread b : allBrokerThread) {
            b.setBarrier(barrier);
        }

        for (AssessorThread a : allAssessorThread) {
            a.setBarrier(barrier);
        }
    }

    public void first_report() {
        //initializeObjects();
        
        System.out.printf("%16s >> ", Thread.currentThread().getName());
        System.out.printf("%15s Parameters %15s \n", "=".repeat(25), "=".repeat(25));
        
        System.out.printf("%16s >> ", Thread.currentThread().getName());
        System.out.printf("%-19s : %-4d \n", "Days of simulation", day);
        
        System.out.printf("%16s >> ", Thread.currentThread().getName());
        System.out.printf("%-19s : ", "ClaimGateways");
        System.out.println(allClaimGateways);
        
        System.out.printf("%16s >> ", Thread.currentThread().getName());
        System.out.printf("%-19s : %-4d \n", "Batch size", batch_size);
        
        System.out.printf("%16s >> ", Thread.currentThread().getName());
        System.out.printf("%-19s : ", "PaymentGateways");
        System.out.println(allPaymentGateways);
        
        System.out.printf("%16s >> ", Thread.currentThread().getName());
        System.out.printf("%-19s : %-4d \n", "Daily tokens", token);
        
        System.out.printf("%16s >> ", Thread.currentThread().getName());
        System.out.printf("%-19s : ", "BrokerThreads");
        System.out.println(allBrokerThread);
        
        System.out.printf("%16s >> ", Thread.currentThread().getName());
        System.out.printf("%-19s : min = %-4d, max = %-4d \n", "Daily claims", min_claim, max_claim);
        
        System.out.printf("%16s >> ", Thread.currentThread().getName());
        System.out.printf("%-19s : ", "AssessorThreads");
        System.out.println(allAssessorThread);
        
        System.out.printf("%16s >> ", Thread.currentThread().getName());
        System.out.printf("%-19s : %-4d \n", "Daily capacity", capacity);
        
        //System.out.printf("%16s", Thread.currentThread().getName() + " >> \n");
    }   

    public void newday_report(int d) {
        System.out.printf("%16s >> %n", Thread.currentThread().getName());
        System.out.printf("%16s >> %s%n", Thread.currentThread().getName(), "=".repeat(70));
        System.out.printf("%16s >> Day %d%n", Thread.currentThread().getName(), d);
        
        for (ClaimGateway c : allClaimGateways) {
            System.out.printf("%16s >> ClaimGateway_%d pending claims = %3d%n", 
                    Thread.currentThread().getName(), c.getId(), c.getPendingClaims());
        }
        
        for (PaymentGateway p : allPaymentGateways) {
            p.resetDailyTokens();
            System.out.printf("%16s >> reset PaymentGateway_%d tokens = %3d%n", 
                    Thread.currentThread().getName(), p.getId(), p.getRemainingTokens());
        }
        
        System.out.printf("%16s >> %n", Thread.currentThread().getName());
    }

    public void runSimulation() {
        // Start all threads
        for (BrokerThread b : allBrokerThread) b.start();
        for (AssessorThread a : allAssessorThread) a.start();

        for (int d = 1; d <= day; d++) {
            newday_report(d);

            try {
                barrier.await(); //brokers & assessors for the new day
                barrier.await(); //brokers completed NewClaims()
                
                barrier.await(); //brokers completed ClaimSubmission()
                System.out.printf("%16s >> %n", Thread.currentThread().getName()); //empty line before doing approve->pending->issue
                barrier.await(); //main empty line
                
                barrier.await(); //Assessors ClaimApproval()
                barrier.await(); //Assessors ShowPendingPayments()
                barrier.await(); //Assessors PaymentIssue()
            }
            catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Wait for all worker threads to complete
        for (BrokerThread b : allBrokerThread) {
            try { 
                b.join(); 
            } 
            catch (InterruptedException e) { 
                Thread.currentThread().interrupt(); 
            }
        }
        for (AssessorThread a : allAssessorThread) {
            try { 
                a.join(); 
            } 
            catch (InterruptedException e) { 
                Thread.currentThread().interrupt(); 
            }
        }

        printSummary();
    }

    //Final sorted summary output
    public void printSummary() {
        System.out.printf("%16s >> %n", Thread.currentThread().getName());
        System.out.printf("%16s >> Summary%n", Thread.currentThread().getName());

        Collections.sort(allAssessorThread, new Comparator<AssessorThread>() {
            @Override
            public int compare(AssessorThread a1, AssessorThread a2) {
                if (a2.getSuccessfulPayments() != a1.getSuccessfulPayments()) {
                    return Integer.compare(a2.getSuccessfulPayments(), a1.getSuccessfulPayments()); // Descending payments
                }
                return a1.getName().compareTo(a2.getName()); // Ascending name
            }
        });

        for (AssessorThread a : allAssessorThread) {
            System.out.printf("%16s >> %s successful payments = %d%n", 
                    Thread.currentThread().getName(), a.getName(), a.getSuccessfulPayments());
        }
    }
}
