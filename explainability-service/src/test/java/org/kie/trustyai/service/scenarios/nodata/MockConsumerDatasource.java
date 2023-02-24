package org.kie.trustyai.service.scenarios.nodata;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.DataSource;

import io.quarkus.test.Mock;

@Mock
@Alternative
@ApplicationScoped
public class MockConsumerDatasource extends DataSource {

    private Dataframe current;

    public Dataframe getCurrent() {
        return current;
    }

    @Override
    public void appendDataframe(Dataframe dataframe) {
        this.current = dataframe;
    }

}
