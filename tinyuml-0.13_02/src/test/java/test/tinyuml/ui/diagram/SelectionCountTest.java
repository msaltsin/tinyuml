package test.tinyuml.ui.diagram;

import java.util.Arrays;
import java.util.Collections;
import junit.framework.TestCase;
import org.tinyuml.draw.DiagramElement;
import org.tinyuml.draw.LabelSource;
import org.tinyuml.model.ElementType;
import org.tinyuml.model.RelationType;
import org.tinyuml.model.UmlModelImpl;
import org.tinyuml.ui.diagram.SelectionStatus;
import org.tinyuml.umldraw.shared.UmlNode;
import org.tinyuml.umldraw.structure.PackageElement;
import org.tinyuml.umldraw.structure.StructureDiagram;

public class SelectionCountTest extends TestCase {
  private StructureDiagram diagram;

  protected void setUp() { diagram = new StructureDiagram(new UmlModelImpl()); }

  private UmlNode add(ElementType type) {
    UmlNode node = diagram.getElementFactory().createNode(type);
    diagram.addChild(node);
    return node;
  }

  public void testNoSelectionAndBackgroundSelection() {
    assertEquals("Sin selecci\u00f3n | Seleccionados: 00",
      SelectionStatus.formatSelection(diagram, Collections.<DiagramElement>emptyList()));
    assertEquals("Sin selecci\u00f3n | Seleccionados: 00",
      SelectionStatus.formatSelection(diagram, Arrays.<DiagramElement>asList(diagram)));
  }

  public void testSingleSelectionReportsCategoryAndName() {
    UmlNode node = add(ElementType.CLASS);
    ((LabelSource) node).setLabelText("Cliente");
    assertEquals("Class: Cliente | Seleccionados: 01",
      SelectionStatus.formatSelection(diagram, Arrays.<DiagramElement>asList(node)));
    ((LabelSource) node).setLabelText("Cliente actualizado");
    assertTrue(SelectionStatus.formatSelection(diagram, Arrays.<DiagramElement>asList(node))
      .contains("Cliente actualizado"));
  }

  public void testMultipleSelectionAndDuplicates() {
    UmlNode one = add(ElementType.PACKAGE), two = add(ElementType.COMPONENT);
    assertEquals("Selecci\u00f3n m\u00faltiple | Seleccionados: 02",
      SelectionStatus.formatSelection(diagram, Arrays.<DiagramElement>asList(one, two, one)));
  }

  public void testUnnamedNoteAndRelationSelection() {
    UmlNode note = add(ElementType.NOTE), node = add(ElementType.CLASS);
    assertEquals("Note: (sin nombre) | Seleccionados: 01",
      SelectionStatus.formatSelection(diagram, Arrays.<DiagramElement>asList(note)));
    DiagramElement relation = diagram.getElementFactory().createConnection(RelationType.NOTE_CONNECTOR, note, node);
    assertTrue(SelectionStatus.formatSelection(diagram, Arrays.asList(relation)).startsWith("Relation:"));
    assertTrue(SelectionStatus.formatSelection(diagram, Arrays.asList(relation)).endsWith("01"));
  }
}
