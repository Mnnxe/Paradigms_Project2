import java.util.*;
import java.util.concurrent.*;

class AssessorThread extends Thread {
    private int total_days;
    private int daily_capacity;
    private ArrayList<ClaimGateway> claim_gates;
    private ArrayList<PaymentGateway> payment_gates;
    private CyclicBarrier barrier;
    private Random rand = new Random();

    private int pending_payments = 0;      // accumulated from previous days
    private int successful_payments = 0;   // used in the summary report

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

    // 5.2 claim approvals
    private void ClaimApproval() {
        int index = rand.nextInt(claim_gates.size());      // log on to only 1 gateway
        ClaimGateway gate = claim_gates.get(index);

        synchronized (gate) {      // approve + print together, so the pending number is correct
            int approved = gate.approveClaims(daily_capacity);
            pending_payments += approved;                   // 5.3 update pending payments
            System.out.printf("%16s >> approve %3d claims from ClaimGateway_%d   ClaimGateway_%d pending claims = %5d%n",
                    Thread.currentThread().getName(), approved, index, index, gate.getPendingClaims());
        }
    }

    // 5.3 show pending payments
    private void ShowPendingPayments() {
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
                    Thread.currentThread().getName(), issued, index, index, gate.getRemainingTokens());
        }
    }

    public void run() {
        for (int day = 1; day <= total_days; day++) {
            try {
                barrier.await();    // 5.1 day header printed
                barrier.await();    // brokers finished new claims
                barrier.await();    // brokers finished submitting claims

                ClaimApproval();
                ShowPendingPayments();
                if (pending_payments != 0) {
                    PaymentIssue();
                }
            }
            catch (InterruptedException | BrokenBarrierException e) { break; }
        }
    }
}