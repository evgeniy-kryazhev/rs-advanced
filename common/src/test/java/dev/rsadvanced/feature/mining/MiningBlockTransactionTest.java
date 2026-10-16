package dev.rsadvanced.feature.mining;

import dev.rsadvanced.feature.mining.module.ToolMiningSession;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MiningBlockTransactionTest {
    @Test
    void rejectedModuleRollsBackEarlierReservations() {
        var payment = new Reservation();
        var rejection = new Reservation();
        rejection.accept = false;
        try (var transaction = new MiningBlockTransaction()) {
            assertFalse(transaction.reserve(List.of(payment, rejection), 80, false));
        }
        assertEquals(0, payment.balance);
        assertEquals(1, payment.finishes);
        assertEquals(1, rejection.finishes);
    }

    @Test
    void successfulRemovalCommitsEveryModuleReservation() {
        var payment = new Reservation();
        try (var transaction = new MiningBlockTransaction()) {
            assertTrue(transaction.reserve(List.of(payment), 80, true));
            transaction.removed(true);
        }
        assertEquals(80, payment.balance);
        assertTrue(payment.removed);
    }

    @Test
    void failedReservationStillFinishesAndPreservesTheOriginalException() {
        var payment = new Reservation();
        var failure = new Reservation();
        RuntimeException reservationFailure = new IllegalStateException("reservation failed");
        failure.reserveFailure = reservationFailure;
        failure.finishFailure = new IllegalStateException("cleanup failed");
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            try (var transaction = new MiningBlockTransaction()) {
                transaction.reserve(List.of(payment, failure), 80, true);
            }
        });
        assertSame(reservationFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals(0, payment.balance);
        assertEquals(0, failure.balance);
        assertEquals(1, payment.finishes);
        assertEquals(1, failure.finishes);
    }

    @Test
    void oneCleanupFailureDoesNotPreventOtherRefunds() {
        var payment = new Reservation();
        var failure = new Reservation();
        failure.finishFailure = new IllegalStateException("cleanup failed");
        assertThrows(IllegalStateException.class, () -> {
            try (var transaction = new MiningBlockTransaction()) {
                assertTrue(transaction.reserve(List.of(payment, failure), 80, false));
            }
        });
        assertEquals(0, payment.balance);
        assertEquals(0, failure.balance);
        assertEquals(1, payment.finishes);
    }

    private static final class Reservation implements ToolMiningSession {
        private long balance;
        private int finishes;
        private boolean removed;
        private boolean accept = true;
        private RuntimeException reserveFailure;
        private RuntimeException finishFailure;

        @Override
        public boolean beforeBlock(long cost, boolean selected) {
            if (!accept) {
                return false;
            }
            balance += cost;
            if (reserveFailure != null) {
                throw reserveFailure;
            }
            return true;
        }

        @Override
        public void afterBlock(boolean removed) {
            finishes++;
            this.removed = removed;
            if (!removed) {
                balance = 0;
            }
            if (finishFailure != null) {
                throw finishFailure;
            }
        }
    }
}
