
package com.gearshare.backend.borrowing;

import java.time.LocalDate;

public record BorrowRequestDateRequest(
        LocalDate startDate,
        LocalDate endDate
) {
}
