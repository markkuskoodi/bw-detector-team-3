package ee.digit25.detector.process;

import ee.digit25.detector.domain.transaction.common.Transaction;
import ee.digit25.detector.domain.transaction.external.api.TransactionModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data Transfer Object to hold the result of asynchronous transaction validation.
 *
 * This class encapsulates:
 * - The original transaction model from the external API
 * - The validation result (legitimate or not)
 * - The mapped database entity ready for persistence
 * - Any exception that occurred during validation
 *
 * Used by CompletableFuture to safely pass validation results between
 * async worker threads and the main processing thread.
 */
@Getter
@AllArgsConstructor
public class ValidationResult {
    /**
     * The original transaction model being validated
     */
    private final TransactionModel transactionModel;

    /**
     * True if the transaction passed all validation rules, false otherwise
     */
    private final boolean isValid;

    /**
     * The mapped database entity ready for persistence
     */
    private final Transaction transactionEntity;

    /**
     * Any exception that occurred during validation (null if successful)
     */
    private final Exception error;

    /**
     * Factory method for successful validation result
     */
    public static ValidationResult success(TransactionModel model, boolean isValid, Transaction entity) {
        return new ValidationResult(model, isValid, entity, null);
    }

    /**
     * Factory method for failed validation (exception occurred)
     */
    public static ValidationResult failure(TransactionModel model, Exception error) {
        return new ValidationResult(model, false, null, error);
    }

    /**
     * Check if validation completed successfully (no exception)
     */
    public boolean hasError() {
        return error != null;
    }
}
