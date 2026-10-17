package com.brazcubas.apsii.repository;

import com.brazcubas.apsii.model.Forecast;
import com.brazcubas.apsii.model.PredictionModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Protege a leitura do campo opcional model_confidence pela via pública
 * (load / findAll / findByModelAndProbe), sem chamar método privado.
 */
@ExtendWith(MockitoExtension.class)
class CsvForecastRepositoryConfidenceTest {
    private static final String FIXTURE = "classpath:data-test/predictions-11-2.csv";

    @Mock
    private ModelRepository modelRepository;

    private CsvForecastRepository repository;

    @BeforeEach
    void setUp() {
        when(modelRepository.findAll()).thenReturn(List.of(
                model("model-a", "Modelo A"),
                model("model-b", "Modelo B")));
        repository = new CsvForecastRepository(new DefaultResourceLoader(), modelRepository, FIXTURE);
        repository.load();
    }

    @Test
    void blankConfidenceLoadsAsNull() {
        Forecast forecast = timelineRow("t-blank");

        assertNotNull(forecast);
        assertNull(forecast.modelConfidence());
    }

    @Test
    void confidenceZeroIsAcceptedAtLowerBound() {
        Forecast forecast = timelineRow("t-zero");

        assertNotNull(forecast);
        assertEquals(0.0, forecast.modelConfidence());
    }

    @Test
    void confidenceOneIsAcceptedAtUpperBound() {
        Forecast forecast = timelineRow("t-one");

        assertNotNull(forecast);
        assertEquals(1.0, forecast.modelConfidence());
    }

    @Test
    void confidenceAboveOneDropsTheRow() {
        assertRowDropped("t-above-one");
    }

    @Test
    void nonNumericConfidenceDropsTheRow() {
        assertRowDropped("t-text");
    }

    @Test
    void negativeConfidenceDropsTheRow() {
        assertRowDropped("t-negative");
    }

    @Test
    void notFiniteConfidenceDropsTheRow() {
        assertRowDropped("t-nan");
    }

    @Test
    void timelineKeepsOnlyValidRowsInChronologicalOrder() {
        List<String> ids = repository.findByModelAndProbe("model-a", 900001).stream()
                .map(Forecast::predictionId).toList();

        assertEquals(List.of("t-blank", "t-zero", "t-one"), ids);
    }

    private void assertRowDropped(String predictionId) {
        assertTrue(repository.findAll().stream().noneMatch(f -> f.predictionId().equals(predictionId)),
                "findAll() não deveria conter " + predictionId);
        assertTrue(repository.findByModelAndProbe("model-a", 900001).stream()
                        .noneMatch(f -> f.predictionId().equals(predictionId)),
                "a timeline não deveria conter " + predictionId);
    }

    private Forecast timelineRow(String predictionId) {
        return repository.findByModelAndProbe("model-a", 900001).stream()
                .filter(f -> f.predictionId().equals(predictionId))
                .findFirst()
                .orElse(null);
    }

    private static PredictionModel model(String id, String name) {
        return new PredictionModel(id, name, "Descrição de exemplo", "Grupo", "Teste", "1.0", true, null);
    }
}
