package com.behsazan.corebanking.system.modelcomparison;

import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.ComparisonConfiguration;
import com.behsazan.corebanking.system.modelcomparison.OracleEaXmiExportModels.ExportPreview;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

@Service
class OracleEaXmiExportService {
    private final OracleSchemaInspector connectionInspector;
    private final OracleEaMetadataInspector metadataInspector;
    private final EaOracleXmiWriter writer;
    private final EaXmiModelParser validationParser;

    OracleEaXmiExportService(
            OracleSchemaInspector connectionInspector,
            OracleEaMetadataInspector metadataInspector,
            EaOracleXmiWriter writer,
            EaXmiModelParser validationParser
    ) {
        this.connectionInspector = connectionInspector;
        this.metadataInspector = metadataInspector;
        this.writer = writer;
        this.validationParser = validationParser;
    }

    ComparisonConfiguration configuration() {
        return connectionInspector.configuration();
    }

    ExportPreview preview(OracleEaExportOptions options) {
        OracleEaPhysicalModel model = metadataInspector.inspect(options);
        List<OracleEaTable> selectedTables = model.tables().stream().filter(table -> !table.externalReference()).toList();
        return new ExportPreview(
                model.schemaName(),
                normalizePattern(options.tablePattern()),
                selectedTables.size(),
                (int) model.tables().stream().filter(OracleEaTable::externalReference).count(),
                selectedTables.stream().mapToInt(table -> table.columns().size()).sum(),
                (int) selectedTables.stream().flatMap(table -> table.keyConstraints().stream()).filter(OracleEaKeyConstraint::primaryKey).count(),
                (int) selectedTables.stream().flatMap(table -> table.keyConstraints().stream()).filter(OracleEaKeyConstraint::uniqueKey).count(),
                model.foreignKeys().size(),
                selectedTables.stream().mapToInt(table -> table.indexes().size()).sum(),
                selectedTables.stream().mapToInt(table -> table.checks().size()).sum(),
                Instant.now(),
                selectedTables.stream().map(OracleEaTable::tableName).sorted().limit(80).toList(),
                model.warnings()
        );
    }

    ExportedXmi export(OracleEaExportOptions options) {
        ComparisonConfiguration configuration = configuration();
        OracleEaPhysicalModel model = metadataInspector.inspect(options);
        byte[] content = writer.write(model, configuration.databaseVersion());

        EaXmiModel parsed = validationParser.parse(new ByteArrayInputStream(content));
        long expected = model.tables().size();
        if (parsed.rawTableDefinitionCount() != expected) {
            throw new IllegalStateException("Generated EA XMI validation failed: expected " + expected + " table definitions but parsed " + parsed.rawTableDefinitionCount());
        }

        String fileName = "oracle-" + model.schemaName().toLowerCase() + "-ea-xmi.xml";
        return new ExportedXmi(fileName, content, model.tables().size(), model.foreignKeys().size(), model.warnings());
    }

    private static String normalizePattern(String value) {
        return value == null || value.isBlank() ? "%" : value.trim().toUpperCase();
    }

    record ExportedXmi(String fileName, byte[] content, int tableCount, int foreignKeyCount, List<String> warnings) {
    }
}
