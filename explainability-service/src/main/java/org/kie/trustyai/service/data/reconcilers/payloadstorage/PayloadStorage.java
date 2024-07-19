package org.kie.trustyai.service.data.reconcilers.payloadstorage;

import org.kie.trustyai.service.payloads.consumer.PartialPayload;

public abstract class PayloadStorage<T extends PartialPayload, U extends PartialPayload> implements PayloadStorageInterface<T, U>{
}
