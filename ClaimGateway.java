/*
********** group members *****************
6813115     Renuka      Choosingkae
6813238 	Napat       Chonsaktrakul
6813248 	Pimpichcha  Saengngoen
6813258 	Sittichok   Boonthittanont
*/
package Project2_6813115;

import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.io.*;

public class ClaimGateway {
    private int id;
    private int numberOfGateways;
    private int batchSize;
    private ArrayList<Integer> pending_claims;

    public ClaimGateway(int id, int numberOfGateways, int batchSize) {
        this.id = id;
        this.numberOfGateways = numberOfGateways;
        this.batchSize = batchSize;
        this.pending_claims = new ArrayList<>();
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getNumberOfGateways() {
        return numberOfGateways;
    }

    public int getId() {
        return id;
    }

    public synchronized void submit_claims(int claims) {
        pending_claims.add(claims);
    }

    public synchronized int approveClaims(int capacity) {
        int approved = 0;
        while (!pending_claims.isEmpty() && approved < capacity) {
            int currentBatch = pending_claims.get(0);
            if (approved + currentBatch <= capacity) {
                approved += currentBatch;
                pending_claims.remove(0);
            } else {
                int needed = capacity - approved;
                approved += needed;
                pending_claims.set(0, currentBatch - needed);
                break;
            }
        }
        return approved;
    }

    public synchronized int getPendingClaims() {
        int total = 0;
        for (int c : pending_claims) {
            total += c;
        }
        return total;
    }

    public synchronized ArrayList<Integer> getPendingClaimsList() {
        return new ArrayList<>(pending_claims);
    }
}

