package com.example.untitled.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BulkOperationGuardTest {

    private final BulkOperationGuard guard = new BulkOperationGuard();

    @Test
    void closingAPermitTwiceReleasesOnlyOnce() {
        BulkOperationGuard.Permit first = guard.acquire();
        first.close();
        BulkOperationGuard.Permit second = guard.acquire();

        first.close(); // must not release the second operation's permit

        assertThat(guard.isBusy()).isTrue();
        second.close();
        assertThat(guard.isBusy()).isFalse();
    }
}
