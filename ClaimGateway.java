/*
********** group members *****************
6813115     Renuka      Choosingkae
6813238 	Napat       Chonsaktrakul
6813248 	Pimpichcha  Saengngoen
6813258 	Sittichok   Boonthittanont
*/
package Project2_6813115;

import java.util.*;

public class ClaimGateway {
    private int id;
    private int batchSize;
    private int pending_claims;
    private Random rand = new Random();

    public ClaimGateway(int id, int batchSize) {
        this.id = id;
        this.batchSize = batchSize;
        this.pending_claims = 0;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getId() {
        return id;
    }

    public synchronized void submit_claims(int claims) {
        pending_claims += claims;
    }

    public synchronized int approveClaims(int capacity) {
        int approved = 0;
        if (pending_claims > 0) {
            int max = Math.min(capacity, pending_claims);
            approved = rand.nextInt(max) + 1;
            pending_claims -= approved;
        }
        return approved;
    }

    public synchronized int getPendingClaims() {
        return pending_claims;
    }

    public String toString() {
        return "ClaimGateway_" + id;
    }
}
