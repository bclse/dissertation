import com.daml.ledger.javaapi.data.FiltersByParty;
import com.daml.ledger.javaapi.data.LedgerOffset;
import com.daml.ledger.javaapi.data.NoFilter;
import com.daml.ledger.javaapi.data.Transaction;
import com.daml.ledger.rxjava.DamlLedgerClient;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.Collections;

public class DealWithBob extends Thread {

    DamlLedgerClient client;
    String ledgerHost = "192.168.61.130";
    int ledgerPort = 5011;
    Bob bob;
    
    public DealWithBob(Bob bob) {
        this.bob = bob;
    }

    @Override
    public void run() {
        try {
            startLedgerClient();
            try {
                runIndefinitely();
                Thread.currentThread().join(); 
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            this.interrupt();
        }
    }

    public void startLedgerClient()  {
        client = DamlLedgerClient.newBuilder(ledgerHost, ledgerPort)
                .withAccessToken("Authorization: " + "Bearer " + bob.Token)
                .build();
        client.connect();
    }


    public void runIndefinitely() {
        Flowable<Transaction> transactions = client.getTransactionsClient().getTransactions(LedgerOffset.LedgerEnd.getInstance(),
                new FiltersByParty(Collections.singletonMap(bob.partyBob, NoFilter.instance)), true);

        transactions.forEach(this::processTransaction);
    }

    private void processTransaction(Transaction transaction) throws IOException {
        System.out.println("BOB API QUERY : " + System.currentTimeMillis() + " " + java.time.LocalDateTime.now());
        System.out.println("Process Transaction");
        bob.query();
    }
}
