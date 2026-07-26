package com.backend.sensor_data.service.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.backend.sensor_data.service.strategy.ThresholdStrategy;

/**
 * Covers the Template Method itself -- process()'s fixed step order
 * (validate -> mapToEntity -> save -> checkThreshold -> broadcast) and its
 * fail-fast behavior (an exception at any step aborts everything after it).
 *
 * This is the piece none of the concrete processor tests
 * (StreetLightProcessorTest, AirPollutionProcessorTest, TrafficProcessorTest)
 * actually exercise -- they call validate()/mapToEntity()/etc. directly,
 * never through process() itself.
 *
 * ThresholdStrategy is only ever stored as a field by AbstractSensorProcessor
 * (never invoked directly -- checkThreshold() is the abstract hook subclasses
 * use to call it), so a bare mock is sufficient here; its behavior is
 * irrelevant to what this test verifies.
 */
class AbstractSensorProcessorTest {

    private final List<String> callOrder = new ArrayList<>();

    /**
     * Minimal concrete processor that records which step ran and, for the
     * data-flow test, whether the entity handed to each downstream step is
     * the exact same object mapToEntity() produced.
     */
    private class RecordingProcessor extends AbstractSensorProcessor<String, StringBuilder> {

        private boolean throwOnValidate;
        private boolean throwOnMapToEntity;
        private boolean throwOnSave;
        private boolean throwOnCheckThreshold;
        private boolean throwOnBroadcast;

        @SuppressWarnings("unchecked")
        RecordingProcessor() {
            super((ThresholdStrategy<StringBuilder>) mock(ThresholdStrategy.class));
        }

        @Override
        protected void validate(String dto) {
            callOrder.add("validate");
            if (throwOnValidate) throw new IllegalArgumentException("validate failed");
        }

        @Override
        protected StringBuilder mapToEntity(String dto) {
            callOrder.add("mapToEntity");
            if (throwOnMapToEntity) throw new IllegalStateException("mapToEntity failed");
            return new StringBuilder(dto);
        }

        @Override
        protected void save(StringBuilder entity) {
            callOrder.add("save:" + entity);
            if (throwOnSave) throw new RuntimeException("save failed");
        }

        @Override
        protected void checkThreshold(StringBuilder entity) {
            callOrder.add("checkThreshold:" + entity);
            if (throwOnCheckThreshold) throw new RuntimeException("checkThreshold failed");
        }

        @Override
        protected void broadcast(StringBuilder entity) {
            callOrder.add("broadcast:" + entity);
            if (throwOnBroadcast) throw new RuntimeException("broadcast failed");
        }
    }

    // ---------------- happy path: exact step order ----------------

    @Test
    void process_callsAllFiveStepsInExactOrder() {
        RecordingProcessor processor = new RecordingProcessor();

        processor.process("sensor-reading");

        assertThat(callOrder).containsExactly(
                "validate",
                "mapToEntity",
                "save:sensor-reading",
                "checkThreshold:sensor-reading",
                "broadcast:sensor-reading");
    }

    @Test
    void process_entityFromMapToEntity_isPassedToAllThreeDownstreamSteps() {
        RecordingProcessor processor = new RecordingProcessor();

        processor.process("shared-entity-value");

        // save/checkThreshold/broadcast all recorded the SAME entity instance
        // mapToEntity() produced -- confirms the template method threads one
        // entity through, rather than each step reconstructing its own.
        assertThat(callOrder.get(2)).isEqualTo("save:shared-entity-value");
        assertThat(callOrder.get(3)).isEqualTo("checkThreshold:shared-entity-value");
        assertThat(callOrder.get(4)).isEqualTo("broadcast:shared-entity-value");
    }

    // ---------------- fail-fast: exception at each step aborts the rest ----------------

    @Test
    void process_validateThrows_noOtherStepRuns() {
        RecordingProcessor processor = new RecordingProcessor();
        processor.throwOnValidate = true;

        assertThatThrownBy(() -> processor.process("x"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(callOrder).containsExactly("validate");
    }

    @Test
    void process_mapToEntityThrows_saveCheckThresholdAndBroadcastNeverRun() {
        RecordingProcessor processor = new RecordingProcessor();
        processor.throwOnMapToEntity = true;

        assertThatThrownBy(() -> processor.process("x"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(callOrder).containsExactly("validate", "mapToEntity");
    }

    @Test
    void process_saveThrows_checkThresholdAndBroadcastNeverRun() {
        RecordingProcessor processor = new RecordingProcessor();
        processor.throwOnSave = true;

        assertThatThrownBy(() -> processor.process("x"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("save failed");

        assertThat(callOrder).containsExactly("validate", "mapToEntity", "save:x");
    }

    @Test
    void process_checkThresholdThrows_broadcastNeverRuns() {
        // This is the scenario most worth protecting: if checkThreshold() (the
        // alert-firing step) throws, the data has ALREADY been saved (previous
        // step succeeded) but the WebSocket broadcast is correctly skipped --
        // confirms broadcast never fires on top of a failed alert check.
        RecordingProcessor processor = new RecordingProcessor();
        processor.throwOnCheckThreshold = true;

        assertThatThrownBy(() -> processor.process("x"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("checkThreshold failed");

        assertThat(callOrder).containsExactly("validate", "mapToEntity", "save:x", "checkThreshold:x");
    }

    @Test
    void process_broadcastThrows_allPriorStepsStillCompleted() {
        RecordingProcessor processor = new RecordingProcessor();
        processor.throwOnBroadcast = true;

        assertThatThrownBy(() -> processor.process("x"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("broadcast failed");

        assertThat(callOrder).containsExactly(
                "validate", "mapToEntity", "save:x", "checkThreshold:x", "broadcast:x");
    }
}
