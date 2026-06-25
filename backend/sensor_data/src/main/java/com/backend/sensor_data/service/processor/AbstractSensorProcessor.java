package com.backend.sensor_data.service.processor;

import com.backend.sensor_data.service.strategy.ThresholdStrategy;

public abstract class AbstractSensorProcessor<D, E> {

    protected final ThresholdStrategy<E> strategy;

    protected AbstractSensorProcessor(ThresholdStrategy<E> strategy) {
        this.strategy = strategy;
    }

    public final void process(D dto) {
        validate(dto);
        E entity = mapToEntity(dto);
        save(entity);
        checkThreshold(entity);
        broadcast(entity);
    }

    protected abstract void validate(D dto);
    protected abstract E mapToEntity(D dto);
    protected abstract void save(E entity);
    protected abstract void checkThreshold(E entity);
    protected abstract void broadcast(E entity);
}