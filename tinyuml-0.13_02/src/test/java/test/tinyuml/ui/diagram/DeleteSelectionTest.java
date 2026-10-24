package test.tinyuml.ui.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JViewport;
import junit.framework.TestCase;
import org.tinyuml.draw.DiagramElement;
import org.tinyuml.draw.Scaling;
import org.tinyuml.model.ElementType;
import org.tinyuml.model.UmlModelImpl;
import org.tinyuml.ui.diagram.DiagramEditor;
import org.tinyuml.ui.diagram.commands.AddNodeCommand;
import org.tinyuml.umldraw.shared.UmlNode;
import org.tinyuml.umldraw.structure.StructureDiagram;

public class DeleteSelectionTest extends TestCase {
  private static class TestEditor extends DiagramEditor {
    List<DiagramElement> selected = new ArrayList<DiagramElement>();
    boolean accept = true;
    int confirmations;
    int confirmedCount;
    TestEditor(StructureDiagram diagram) { super(null, diagram); }
    public List<DiagramElement> getSelectedElements() { return selected; }
    protected boolean confirmDeleteSelection(int count) {
      confirmations++;
      confirmedCount = count;
      return accept;
    }
  }
  private StructureDiagram diagram;
  private TestEditor editor;
  protected void setUp() {
    diagram = new StructureDiagram(new UmlModelImpl());
    editor = new TestEditor(diagram);
  }
  private UmlNode add(ElementType type, int x, int y) {
    UmlNode node = diagram.getElementFactory().createNode(type);
    node.setParent(diagram);
    node.setSize(100, 60);
    editor.execute(new AddNodeCommand(editor, diagram, node, x, y));
    return node;
  }
  public void testCreateUndoRedoRecomputeCounts() {
    add(ElementType.CLASS, 100, 100);
    assertEquals(1, diagram.getChildren().size());
    editor.undo();
    assertEquals(0, diagram.getChildren().size());
    editor.redo();
    assertEquals(1, diagram.getChildren().size());
  }

  public void testDeleteEmptySelectionDoesNotCreateUndoEntry() {
    editor.deleteSelection();
    assertEquals(0, editor.confirmations);
    assertFalse(editor.canUndo());
  }

  public void testDeleteBackgroundDoesNothing() {
    editor.selected.add(diagram);
    editor.deleteSelection();
    assertFalse(editor.canUndo());
  }

  public void testSingleDeleteDoesNotAskAndCanBeUndone() {
    editor.selected.add(add(ElementType.NOTE, 100, 100));
    editor.deleteSelection();
    assertEquals(0, editor.confirmations);
    assertEquals(0, diagram.getChildren().size());
    editor.undo();
    assertEquals(1, diagram.getChildren().size());
  }

  public void testCancelMultipleDeletePreservesDiagramAndHistory() {
    editor.selected.add(add(ElementType.CLASS, 100, 100));
    editor.selected.add(add(ElementType.COMPONENT, 260, 100));
    editor.clearUndoManager();
    editor.accept = false;
    editor.deleteSelection();
    assertEquals(1, editor.confirmations);
    assertEquals(2, editor.confirmedCount);
    assertEquals(2, diagram.getChildren().size());
    assertFalse(editor.canUndo());
  }

  public void testConfirmMultipleDeleteUndoAndRedo() {
    editor.selected.add(add(ElementType.CLASS, 100, 100));
    editor.selected.add(add(ElementType.PACKAGE, 260, 100));
    editor.deleteSelection();
    assertEquals(2, editor.confirmedCount);
    assertEquals(0, diagram.getChildren().size());
    editor.undo();
    assertEquals(2, diagram.getChildren().size());
    editor.redo();
    assertEquals(0, diagram.getChildren().size());
  }

  public void testDeleteKeyUsesTheConfirmationPath() {
    editor.selected.add(add(ElementType.CLASS, 100, 100));
    editor.selected.add(add(ElementType.NOTE, 250, 100));
    editor.accept = false;
    editor.getActionMap().get("deleteSelection").actionPerformed(new ActionEvent(editor, 0, "DELETE"));
    assertEquals(2, editor.confirmedCount);
    assertEquals(2, diagram.getChildren().size());
  }
}
