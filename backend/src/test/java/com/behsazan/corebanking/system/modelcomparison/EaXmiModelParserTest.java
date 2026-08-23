package com.behsazan.corebanking.system.modelcomparison;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EaXmiModelParserTest {
    private final EaXmiModelParser parser = new EaXmiModelParser();

    @Test
    void parsesTableColumnsPrimaryKeyAndMergesDuplicateEaDefinitions() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <XMI xmlns:UML="omg.org/UML1.3" timestamp="2026-08-22 10:00:00">
                  <XMI.header><XMI.documentation><XMI.exporter>Enterprise Architect</XMI.exporter><XMI.exporterVersion>2.5</XMI.exporterVersion></XMI.documentation></XMI.header>
                  <XMI.content>
                    <UML:Model name="EA Model">
                      <UML:Namespace.ownedElement>
                        %s
                        %s
                      </UML:Namespace.ownedElement>
                    </UML:Model>
                  </XMI.content>
                </XMI>
                """.formatted(table("PARTY"), table("PARTY"));

        EaXmiModel model = parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertThat(model.modelName()).isEqualTo("EA Model");
        assertThat(model.exporter()).isEqualTo("Enterprise Architect");
        assertThat(model.rawTableDefinitionCount()).isEqualTo(2);
        assertThat(model.tables()).hasSize(1);
        assertThat(model.tables().getFirst().tableName()).isEqualTo("PARTY");
        assertThat(model.tables().getFirst().sourceDefinitionCount()).isEqualTo(2);
        assertThat(model.tables().getFirst().primaryKeyColumns()).containsExactly("PARTY_ID");
        assertThat(model.tables().getFirst().columns()).extracting(EaColumnDefinition::columnName)
                .containsExactly("PARTY_ID", "PARTY_UID");
        assertThat(model.warnings()).hasSize(1);
    }


    @Test
    void enrichesMissingLengthSemanticsFromDuplicateDefinition() {
        String withoutSemantics = tableWithLengthSemantics("PARTY", null);
        String withCharSemantics = tableWithLengthSemantics("PARTY", "CHAR");
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <XMI xmlns:UML="omg.org/UML1.3">
                  <XMI.content><UML:Model name="EA Model"><UML:Namespace.ownedElement>
                    %s
                    %s
                  </UML:Namespace.ownedElement></UML:Model></XMI.content>
                </XMI>
                """.formatted(withoutSemantics, withCharSemantics);

        EaXmiModel model = parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        EaColumnDefinition uid = model.tables().getFirst().columns().stream()
                .filter(column -> "PARTY_UID".equals(column.columnName()))
                .findFirst()
                .orElseThrow();
        assertThat(uid.lengthSemantics()).isEqualTo("CHAR");
        assertThat(uid.displayType()).isEqualTo("VARCHAR2(36 CHAR)");
    }

    @Test
    void parsesPersianAliasDocumentationAndColumnDescription() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <XMI xmlns:UML="omg.org/UML1.3"><XMI.content><UML:Model name="EA Model"><UML:Namespace.ownedElement>
                  <UML:Class name="PARTY_CLASSIFICATION">
                    <UML:ModelElement.stereotype><UML:Stereotype name="table"/></UML:ModelElement.stereotype>
                    <UML:ModelElement.taggedValue>
                      <UML:TaggedValue tag="alias" value="طبقه‌بندی پارتی"/>
                      <UML:TaggedValue tag="documentation" value="&lt;span dir=&quot;rtl&quot;&gt;اطلاعات مربوط به طبقه‌بندی پارتی را نگهداری می‌کند.&lt;/span&gt;"/>
                    </UML:ModelElement.taggedValue>
                    <UML:Classifier.feature>
                      <UML:Attribute name="PARTY_ID"><UML:ModelElement.taggedValue>
                        <UML:TaggedValue tag="description" value="&lt;span dir=&quot;rtl&quot;&gt;شناسه پارتی&lt;/span&gt;"/>
                        <UML:TaggedValue tag="type" value="NUMBER"/><UML:TaggedValue tag="precision" value="19"/><UML:TaggedValue tag="scale" value="0"/><UML:TaggedValue tag="lowerBound" value="1"/>
                      </UML:ModelElement.taggedValue></UML:Attribute>
                    </UML:Classifier.feature>
                  </UML:Class>
                </UML:Namespace.ownedElement></UML:Model></XMI.content></XMI>
                """;

        EaXmiModel model = parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        EaTableDefinition table = model.tables().getFirst();

        assertThat(table.persianTitle()).isEqualTo("طبقه‌بندی پارتی");
        assertThat(table.documentation()).isEqualTo("اطلاعات مربوط به طبقه‌بندی پارتی را نگهداری می‌کند.");
        assertThat(table.columns().getFirst().comment()).isEqualTo("شناسه پارتی");
    }

    @Test
    void rejectsDocumentsWithDoctype() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE XMI [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <XMI xmlns:UML="omg.org/UML1.3"><XMI.content>&xxe;</XMI.content></XMI>
                """;

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(ModelComparisonValidationException.class);
    }


    private static String tableWithLengthSemantics(String name, String semantics) {
        String semanticsTag = semantics == null ? "" : "<UML:TaggedValue tag=\"LengthType\" value=\"" + semantics + "\"/>";
        return """
                <UML:Class name="%s">
                  <UML:ModelElement.stereotype><UML:Stereotype name="table"/></UML:ModelElement.stereotype>
                  <UML:Classifier.feature>
                    <UML:Attribute name="PARTY_ID"><UML:ModelElement.taggedValue>
                      <UML:TaggedValue tag="type" value="NUMBER"/><UML:TaggedValue tag="precision" value="19"/><UML:TaggedValue tag="scale" value="0"/><UML:TaggedValue tag="lowerBound" value="1"/>
                    </UML:ModelElement.taggedValue></UML:Attribute>
                    <UML:Attribute name="PARTY_UID"><UML:ModelElement.taggedValue>
                      <UML:TaggedValue tag="type" value="VARCHAR2"/><UML:TaggedValue tag="length" value="36"/>%s<UML:TaggedValue tag="lowerBound" value="1"/>
                    </UML:ModelElement.taggedValue></UML:Attribute>
                    <UML:Operation name="PK_PARTY"><UML:ModelElement.stereotype><UML:Stereotype name="PK"/></UML:ModelElement.stereotype>
                      <UML:BehavioralFeature.parameter><UML:Parameter kind="return"/><UML:Parameter name="PARTY_ID" kind="in"/></UML:BehavioralFeature.parameter>
                    </UML:Operation>
                  </UML:Classifier.feature>
                </UML:Class>
                """.formatted(name, semanticsTag);
    }

    private static String table(String name) {
        return """
                <UML:Class name="%s">
                  <UML:ModelElement.stereotype><UML:Stereotype name="table"/></UML:ModelElement.stereotype>
                  <UML:Classifier.feature>
                    <UML:Attribute name="PARTY_ID">
                      <UML:ModelElement.taggedValue>
                        <UML:TaggedValue tag="type" value="NUMBER"/><UML:TaggedValue tag="precision" value="19"/><UML:TaggedValue tag="scale" value="0"/><UML:TaggedValue tag="lowerBound" value="1"/>
                      </UML:ModelElement.taggedValue>
                    </UML:Attribute>
                    <UML:Attribute name="PARTY_UID">
                      <UML:ModelElement.taggedValue>
                        <UML:TaggedValue tag="type" value="VARCHAR2"/><UML:TaggedValue tag="length" value="36"/><UML:TaggedValue tag="LengthType" value="CHAR"/><UML:TaggedValue tag="lowerBound" value="1"/>
                      </UML:ModelElement.taggedValue>
                    </UML:Attribute>
                    <UML:Operation name="PK_PARTY">
                      <UML:ModelElement.stereotype><UML:Stereotype name="PK"/></UML:ModelElement.stereotype>
                      <UML:BehavioralFeature.parameter><UML:Parameter kind="return"/><UML:Parameter name="PARTY_ID" kind="in"/></UML:BehavioralFeature.parameter>
                    </UML:Operation>
                  </UML:Classifier.feature>
                </UML:Class>
                """.formatted(name);
    }
}
