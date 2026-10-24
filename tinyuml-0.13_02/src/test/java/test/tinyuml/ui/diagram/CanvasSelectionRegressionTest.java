package test.tinyuml.ui.diagram;

import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;
import junit.framework.TestCase;
import org.tinyuml.draw.DiagramElement;
import org.tinyuml.draw.MultiSelection;
import org.tinyuml.draw.Scaling;
import org.tinyuml.draw.Selection;
import org.tinyuml.model.ElementType;
import org.tinyuml.model.UmlModelImpl;
import org.tinyuml.ui.diagram.DiagramEditor;
import org.tinyuml.ui.diagram.SelectionStatus;
import org.tinyuml.umldraw.shared.UmlNode;
import org.tinyuml.umldraw.structure.PackageElement;
import org.tinyuml.umldraw.structure.StructureDiagram;

/** Real mouse-event regressions for nested selection and the gray canvas margin. */
public class CanvasSelectionRegressionTest extends TestCase {
  private StructureDiagram diagram;
  private DiagramEditor editor;

  protected void setUp() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      diagram = new StructureDiagram(new UmlModelImpl());
      diagram.setSnapToGrid(false);
      editor = new DiagramEditor(null, diagram);
    });
  }

  private UmlNode add(ElementType type, int x, int y) {
    UmlNode node = diagram.getElementFactory().createNode(type);
    diagram.addChild(node);
    node.setSize(100, 60);
    node.setAbsolutePos(x, y);
    return node;
  }

  private void mouse(int type, double x, double y, int button, boolean popup) {
    double factor = editor.getScaling().getScaleFactor();
    MouseEvent event = new MouseEvent(editor, type, System.currentTimeMillis(),
      type == MouseEvent.MOUSE_DRAGGED ? MouseEvent.BUTTON1_DOWN_MASK : 0,
      (int) Math.round(x * factor), (int) Math.round(y * factor), 1, popup,
      type == MouseEvent.MOUSE_DRAGGED ? MouseEvent.NOBUTTON : button);
    if (type == MouseEvent.MOUSE_PRESSED) editor.mousePressed(event);
    else if (type == MouseEvent.MOUSE_RELEASED) editor.mouseReleased(event);
    else if (type == MouseEvent.MOUSE_DRAGGED) editor.mouseDragged(event);
    else editor.mouseClicked(event);
  }

  private void click(int x, int y) {
    mouse(MouseEvent.MOUSE_PRESSED, x, y, MouseEvent.BUTTON1, false);
    mouse(MouseEvent.MOUSE_RELEASED, x, y, MouseEvent.BUTTON1, false);
    mouse(MouseEvent.MOUSE_CLICKED, x, y, MouseEvent.BUTTON1, false);
  }

  private void band(int x1, int y1, int x2, int y2) {
    mouse(MouseEvent.MOUSE_PRESSED, x1, y1, MouseEvent.BUTTON1, false);
    mouse(MouseEvent.MOUSE_DRAGGED, x2, y2, MouseEvent.BUTTON1, false);
    mouse(MouseEvent.MOUSE_RELEASED, x2, y2, MouseEvent.BUTTON1, false);
  }

  public void testOutsideClickNeverAddsTheDiagramToItself() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Rectangle2D original = diagram.getAbsoluteBounds();
      click(10, 10);
      assertFalse(diagram.getChildren().contains(diagram));
      assertEquals(original, diagram.getAbsoluteBounds());
      assertTrue(editor.getSelectedElements().isEmpty());
      assertFalse(editor.canUndo());
    });
  }

  public void testOutsideClickClearsSingleSelectionWithoutMovingNodes() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      UmlNode node = add(ElementType.CLASS, 100, 100);
      click(120, 130);
      Rectangle2D original = node.getAbsoluteBounds();
      editor.clearUndoManager();
      click(700, 500);
      assertEquals(original, node.getAbsoluteBounds());
      assertTrue(editor.getSelectedElements().isEmpty());
      assertFalse(editor.canUndo());
    });
  }

  public void testOutsideClickClearsMultipleSelection() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      UmlNode one = add(ElementType.CLASS, 100, 100);
      UmlNode two = add(ElementType.COMPONENT, 280, 100);
      band(80, 80, 400, 180);
      assertEquals(2, editor.getSelectedElements().size());
      Rectangle2D first = one.getAbsoluteBounds(), second = two.getAbsoluteBounds();
      editor.clearUndoManager();
      click(700, 500);
      assertEquals(first, one.getAbsoluteBounds());
      assertEquals(second, two.getAbsoluteBounds());
      assertTrue(editor.getSelectedElements().isEmpty());
      assertFalse(editor.canUndo());
    });
  }

  public void testOutsideClickAtReducedZoom() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      editor.setScaling(Scaling.forFactor(0.25));
      click(800, 600);
      assertFalse(diagram.getChildren().contains(diagram));
      assertTrue(editor.getSelectedElements().isEmpty());
      assertFalse(editor.canUndo());
    });
  }

  public void testRightClickOutsideHasNoSelectionOrMenuCast() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      mouse(MouseEvent.MOUSE_PRESSED, 700, 500, MouseEvent.BUTTON3, false);
      mouse(MouseEvent.MOUSE_RELEASED, 700, 500, MouseEvent.BUTTON3, true);
      assertTrue(editor.getSelectedElements().isEmpty());
      assertFalse(editor.canUndo());
    });
  }

  public void testClickBetweenSelectedNodesClearsSelectionWithoutChangingCanvas() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      UmlNode one = add(ElementType.CLASS, 100, 100);
      UmlNode two = add(ElementType.COMPONENT, 280, 100);
      band(80, 80, 400, 180);
      assertEquals(2, editor.getSelectedElements().size());
      Rectangle2D original = diagram.getAbsoluteBounds();
      click(230, 130);
      assertTrue(SelectionStatus.selectedItems(diagram, editor.getSelectedElements()).isEmpty());
      assertEquals(original, diagram.getAbsoluteBounds());
      assertEquals(100.0, one.getAbsoluteX1());
      assertEquals(280.0, two.getAbsoluteX1());
      assertFalse(editor.canUndo());
    });
  }

  public void testDiagramSelectionCannotReparentCanvas() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Rectangle2D original = diagram.getAbsoluteBounds();
      Selection selection = diagram.getSelection(editor);
      selection.startDragging(700, 500);
      selection.stopDragging(700, 500);
      assertEquals(original, diagram.getAbsoluteBounds());
      assertFalse(diagram.getChildren().contains(diagram));
      assertFalse(editor.canUndo());
    });
  }

  public void testCanvasResizeHandleStillWorksAndCanBeUndone() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Rectangle2D original = diagram.getAbsoluteBounds();
      int x = (int) original.getMaxX() + 2, y = (int) original.getMaxY() + 2;
      mouse(MouseEvent.MOUSE_PRESSED, x, y, MouseEvent.BUTTON1, false);
      mouse(MouseEvent.MOUSE_DRAGGED, x + 35, y + 28, MouseEvent.BUTTON1, false);
      mouse(MouseEvent.MOUSE_RELEASED, x + 35, y + 28, MouseEvent.BUTTON1, false);
      assertTrue(diagram.getSize().getWidth() > original.getWidth());
      assertTrue(diagram.getSize().getHeight() > original.getHeight());
      assertNull(diagram.getParent());
      assertFalse(diagram.getChildren().contains(diagram));
      editor.undo();
      assertEquals(original, diagram.getAbsoluteBounds());
    });
  }
}
