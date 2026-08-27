package com.behsazan.corebanking.system.modelcomparison;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EaOracleXmiWriterTest {
    @Test
    void generatedXmiCanBeReadBackByEaParserAndContainsPhysicalArtifacts() {
        OracleEaColumn childId = column("CHILD_ID", false, 1);
        OracleEaColumn parentId = column("PARENT_ID", false, 2);
        OracleEaColumn parentPk = column("PARENT_ID", false, 1);

        OracleEaTable child = new OracleEaTable(
                "CIF", "CHILD", "جدول فرزند", "ITS_CIF", false,
                List.of(childId, parentId),
                List.of(new OracleEaKeyConstraint("PK_CHILD", "P", List.of("CHILD_ID"), "ENABLED", "PK_CHILD")),
                List.of(new OracleEaIndex("IX_CHILD_PARENT", false, "NORMAL", "VALID",
                        List.of(new OracleEaIndexColumn("PARENT_ID", 1, "ASC")))),
                List.of(new OracleEaCheckConstraint("CK_CHILD_ID", "CHILD_ID > 0", "ENABLED"))
        );
        OracleEaTable parent = new OracleEaTable(
                "CIF", "PARENT", "جدول والد", "ITS_CIF", false,
                List.of(parentPk),
                List.of(new OracleEaKeyConstraint("PK_PARENT", "P", List.of("PARENT_ID"), "ENABLED", "PK_PARENT")),
                List.of(), List.of()
        );
        OracleEaForeignKey fk = new OracleEaForeignKey(
                "FK_CHILD_PARENT", "CIF", "CHILD", "CIF", "PARENT", "PK_PARENT", "P",
                "NO ACTION", "ENABLED", "NOT DEFERRABLE", "IMMEDIATE",
                List.of(new OracleEaForeignKeyColumn("PARENT_ID", "PARENT_ID", 1))
        );
        OracleEaPhysicalModel physicalModel = new OracleEaPhysicalModel("CIF", List.of(child, parent), List.of(fk), List.of());

        byte[] xml = new EaOracleXmiWriter().write(physicalModel, "23c");
        String raw = new String(xml, StandardCharsets.UTF_8);
        EaXmiModel parsed = new EaXmiModelParser().parse(new ByteArrayInputStream(xml));

        assertEquals(2, parsed.tables().size());
        assertTrue(parsed.tables().stream().anyMatch(t -> t.tableName().equals("CHILD") && t.primaryKeyColumns().equals(List.of("CHILD_ID"))));
        EaTableDefinition parsedChild = parsed.tables().stream().filter(t -> t.tableName().equals("CHILD")).findFirst().orElseThrow();
        assertTrue(parsedChild.foreignKeys().stream().anyMatch(parsedFk ->
                parsedFk.constraintName().equals("FK_CHILD_PARENT")
                        && parsedFk.childColumns().equals(List.of("PARENT_ID"))
                        && "PARENT".equals(parsedFk.parentTable())
                        && parsedFk.parentColumns().equals(List.of("PARENT_ID"))));
        assertTrue(parsedChild.checkConstraints().stream().anyMatch(check ->
                check.constraintName().equals("CK_CHILD_ID") && check.condition().contains("CHILD_ID > 0")));
        assertTrue(raw.contains("stereotype=\"FK\"") || raw.contains("name=\"FK\""));
        assertTrue(raw.contains("FK_CHILD_PARENT"));
        assertTrue(raw.contains("IX_CHILD_PARENT"));
        assertTrue(raw.contains("CK_CHILD_ID"));
        assertTrue(raw.contains("جدول فرزند"));
    }

    private static OracleEaColumn column(String name, boolean nullable, int position) {
        return new OracleEaColumn(name, "NUMBER", 22, null, null, 19, 0, nullable, position,
                null, "شرح " + name, false, false);
    }
}
