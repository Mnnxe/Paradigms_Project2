import java.util.ArrayList;

public class PaymentGateway {
    private int id;
    private int numberOfGateways;
    private int dailyPaymentToken;
    private int remaining_tokens;
    private ArrayList<Integer> payment_history;

    public PaymentGateway(int id, int numberOfGateways, int dailyPaymentToken) {
        this.id = id;
        this.numberOfGateways = numberOfGateways;
        this.dailyPaymentToken = dailyPaymentToken;
        this.remaining_tokens = dailyPaymentToken;
        this.payment_history = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public int getNumberOfGateways() {
        return numberOfGateways;
    }

    public int getDailyPaymentToken() {
        return dailyPaymentToken;
    }

    public synchronized int getRemainingTokens() {
        return remaining_tokens;
    }

    public synchronized int issuePayments(int requestedPayments) {
        int issued = Math.min(requestedPayments, remaining_tokens);
        remaining_tokens -= issued;
        if (issued > 0) {
            payment_history.add(issued);
        }
        return issued;
    }

    public synchronized void resetDailyTokens() {
        this.remaining_tokens = this.dailyPaymentToken;
    }

    public synchronized ArrayList<Integer> getPaymentHistory() {
        return new ArrayList<>(payment_history);
    }
}