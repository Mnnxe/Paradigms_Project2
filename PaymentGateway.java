/*
********** group members *****************
6813115     Renuka      Choosingkae
6813238 	Napat       Chonsaktrakul
6813248 	Pimpichcha  Saengngoen
6813258 	Sittichok   Boonthittanont
*/
package Project2_6813115;

public class PaymentGateway {
    private int id;
    private int dailyPaymentToken;
    private int remaining_tokens;

    public PaymentGateway(int id, int dailyPaymentToken) {
        this.id = id;
        this.dailyPaymentToken = dailyPaymentToken;
        this.remaining_tokens = dailyPaymentToken;
    }

    public int getId() {
        return id;
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
        return issued;
    }

    public synchronized void resetDailyTokens() {
        this.remaining_tokens = this.dailyPaymentToken;
    }

    public String toString() {
        return "PaymentGateway_" + id;
    }
}
