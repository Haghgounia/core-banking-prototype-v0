package com.behsazan.corebanking.posting.configuration;

import com.behsazan.corebanking.posting.configuration.PostingConfigurationModels.PreviewReport;
import com.behsazan.corebanking.posting.configuration.PostingConfigurationModels.PreviewRequest;
import com.behsazan.corebanking.posting.configuration.PostingConfigurationModels.StatusReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Never joins a financial transaction. Oracle SERIALIZABLE gives consistent reads across SELECTs. */
@Service
@ConditionalOnProperty(prefix = "core-banking.deal-posting.readiness", name = "enabled", havingValue = "true")
public class PostingConfigurationPreviewService {
    private final PostingConfigurationPreviewEngine engine;

    public PostingConfigurationPreviewService(PostingConfigurationRepository repository) {
        engine = new PostingConfigurationPreviewEngine(repository);
    }

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public StatusReport status() { return engine.status(); }

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public PreviewReport preview(PreviewRequest request) { return engine.preview(request); }
}
