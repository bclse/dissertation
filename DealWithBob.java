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
    String ledgerHost = "192.168.173.137";
    int ledgerPort = 5011;
    Bob bob;
    String Token ="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOm51bGwsInN1YiI6InBhcnRpY2lwYW50X2FkbWluIiwiZXhwIjoxOTYzMDI3ODAzLCJzY29wZSI6ImRhbWxfbGVkZ2VyX2FwaSJ9.0DTTAfGRLnzoLNkFM0ws-En8AZqtmXSx3Mqxj3FVD2v9qC5iVYb94DMV3L3me3TAFmrf2xtF3jRmugvMtbdlqwL7CM1R6QQQ0x6Ubfm_fEJGHJqbYyzWcBnAqFpEumgpI7P8ocRH5jIu6W3RuLqb97rkRKhzRL4JgXh1D-jrtmFm0-DFyOtfUaCxY4Bpca73-0eLIpAW-XRtQ1IeIy0mwrJ_I0anI7bY2FJEYTE__AFaN7x1gKGp3GAPTWeXT-YH2rQ9Hz5JUdx-YNUWvVI_jtb3Szlbtz7zCSeY-B1Qe1F47q-yA_sIB5tGaJ6DtKfjZfXiVH1DbhSrJTm9k-5EyQ";

    public DealWithBob(Bob bob) {
        this.bob = bob;
    }

    @Override
    public void run() {
        try {
            startLedgerClient();
            try {
                runIndefinitely();
                Thread.currentThread().join(); //Thread da alice espera que thread deal with alice termine
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            this.interrupt();
        }
    }

    public void startLedgerClient()  {
        client = DamlLedgerClient.newBuilder(ledgerHost, ledgerPort)
                //.withSslContext(GrpcSslContexts.forClient().build())
                .withAccessToken("Authorization: " + "Bearer " + bob.Token_noAUT)
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
