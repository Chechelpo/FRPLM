package chechelpo.frplm.utils.tokenizers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public final class TokenizerCacheMaintenance {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TokenizerCacheMaintenance.class);

    private final TokenizerStore store;
    private final int retentionDays;

    TokenizerCacheMaintenance(
            TokenizerStore store,
            @Value("${tokenizer.cache.retention-days:60}")
            int retentionDays
    ) {
        if (retentionDays < 1) {
            throw new IllegalArgumentException(
                    "Tokenizer cache retention must be at least one day"
            );
        }

        this.store = store;
        this.retentionDays = retentionDays;
    }

    /**
     * Runs daily at 03:30.
     */
    @Scheduled(
            cron = "${tokenizer.cache.cleanup-cron:0 30 3 * * *}"
    )
    public void purgeUnusedEntries() {
        LocalDateTime cutoff =
                LocalDateTime.now().minusDays(retentionDays);

        try {
            int deleted = store.deleteUnusedBefore(cutoff);

            if (deleted > 0) {
                LOGGER.info(
                        "Deleted {} unused tokenizer cache entries older than {}",
                        deleted,
                        cutoff
                );
            }
        } catch (RuntimeException exception) {
            /*
             * Cache cleanup must never disrupt application operation.
             */
            LOGGER.warn(
                    "Tokenizer cache cleanup failed",
                    exception
            );
        }
    }
}