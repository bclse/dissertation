import com.daml.ledger.javaapi.data.*;
import com.daml.ledger.rxjava.DamlLedgerClient;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.Collections;

public class DealWithAlice extends Thread {

    Alice alice;
    DamlLedgerClient client;
    String ledgerHost = "192.168.61.132";
    int ledgerPort = 5011;
    boolean aux = true;
    boolean aux1 = true;

    public DealWithAlice(Alice alice) {
        this.alice = alice;
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

    public void startLedgerClient() {
        client = DamlLedgerClient.newBuilder(ledgerHost, ledgerPort)
                .withAccessToken("Authorization: " + "Bearer " + alice.Token)
                .build();

        client.connect();
    }

    public void runIndefinitely() {

        Flowable<Transaction> transactions = client.getTransactionsClient().getTransactions(LedgerOffset.LedgerEnd.getInstance(),
                new FiltersByParty(Collections.singletonMap(alice.partyAlice, NoFilter.instance)), true);
        transactions.forEach(this::processTransaction);
    }

    private void processTransaction(Transaction transaction) throws IOException {
        System.out.println("ALICE API QUERY: " + System.currentTimeMillis() + " " + java.time.LocalDateTime.now());
        if (aux && aux1) {
            System.out.println("PROCESSING TRANSACTION: REQUEST CHANGE");
            alice.setPayload(); //Limpar o Payload porque assim estava a concatenar
            alice.setDamlConnection();
            aux1 = false;
        } else {
            System.out.println("PROCESSING TRANSACTION: CHANGE");
            aux1 = true;
        }

    }
}