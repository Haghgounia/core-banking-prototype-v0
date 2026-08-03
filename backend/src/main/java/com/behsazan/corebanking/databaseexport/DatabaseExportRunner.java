package com.behsazan.corebanking.databaseexport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(prefix = "core-banking.database-export", name = "enabled", havingValue = "true")
public class DatabaseExportRunner implements ApplicationRunner {
    private final OracleDatabaseExporter exporter;
    private final ConfigurableApplicationContext applicationContext;

    @Value("${core-banking.database-export.schema:DPS}")
    private String schemaName;

    @Value("${core-banking.database-export.table-prefix:REF_}")
    private String tablePrefix;

    @Value("${core-banking.database-export.output-directory:database/oracle/exports}")
    private String outputDirectory;

    public DatabaseExportRunner(
            OracleDatabaseExporter exporter,
            ConfigurableApplicationContext applicationContext
    ) {
        this.exporter = exporter;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DatabaseExportRequest request = new DatabaseExportRequest(
                schemaName,
                tablePrefix,
                Path.of(outputDirectory)
        );

        System.out.printf("Exporting Oracle schema %s, table prefix '%s'...%n",
                request.schemaName(), request.tablePrefix());

        OracleDatabaseExporter.ExportResult result = exporter.export(request);

        System.out.printf("Database export completed.%n");
        System.out.printf("Tables: %d%n", result.tableCount());
        System.out.printf("Rows: %d%n", result.rowCount());
        System.out.printf("Directory: %s%n", result.outputDirectory());

        SpringApplication.exit(applicationContext);
    }
}
