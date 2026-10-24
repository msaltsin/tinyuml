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

/** Mouse-event regressions for multiple selection and nested packages. */
public class MultipleSelectionRegressionTest extends TestCase {
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
    double factor = 1.0;
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

  public void testBandSelectsOnlyDirectChildrenAndMovesNestedNodeOnce() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      PackageElement pkg = (PackageElement) add(ElementType.PACKAGE, 100, 100);
      pkg.setSize(200, 150);
      UmlNode nested = diagram.getElementFactory().createNode(ElementType.CLASS);
      pkg.addChild(nested);
      nested.setSize(70, 40);
      nested.setAbsolutePos(140, 150);
      UmlNode other = add(ElementType.COMPONENT, 380, 100);
      band(60, 60, 520, 280);
      assertEquals(Arrays.<DiagramElement>asList(pkg, other), editor.getSelectedElements());
      double x = nested.getAbsoluteX1(), y = nested.getAbsoluteY1();
      double relativeX = nested.getOrigin().getX(), relativeY = nested.getOrigin().getY();
      mouse(MouseEvent.MOUSE_PRESSED, 110, 130, MouseEvent.BUTTON1, false);
      mouse(MouseEvent.MOUSE_DRAGGED, 180, 200, MouseEvent.BUTTON1, false);
      mouse(MouseEvent.MOUSE_RELEASED, 180, 200, MouseEvent.BUTTON1, false);
      assertEquals(x + 70, nested.getAbsoluteX1());
      assertEquals(y + 70, nested.getAbsoluteY1());
      assertEquals(relativeX, nested.getOrigin().getX());
      assertEquals(relativeY, nested.getOrigin().getY());
    });
  }

  public void testBoundsQueryDoesNotVisitNestedPackages() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      PackageElement outer = (PackageElement) add(ElementType.PACKAGE, 100, 100);
      outer.setSize(220, 180);
      PackageElement inner = (PackageElement) diagram.getElementFactory().createNode(ElementType.PACKAGE);
      outer.addChild(inner);
      inner.setSize(120, 100);
      inner.setAbsolutePos(130, 140);
      UmlNode leaf = diagram.getElementFactory().createNode(ElementType.NOTE);
      inner.addChild(leaf);
      leaf.setSize(50, 30);
      leaf.setAbsolutePos(150, 170);
      List<DiagramElement> selected = new ArrayList<DiagramElement>();
      diagram.getChildrenInSpecifiedBounds(new Rectangle2D.Double(50, 50, 400, 300), selected);
      assertEquals(Arrays.<DiagramElement>asList(outer), selected);
      selected.clear();
      outer.getChildrenInSpecifiedBounds(new Rectangle2D.Double(50, 50, 400, 300), selected);
      assertEquals(Arrays.<DiagramElement>asList(inner), selected);
    });
  }

  public void testClickWithoutDraggingDoesNotTranslateMultipleSelection() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      UmlNode one = add(ElementType.CLASS, 100, 100);
      UmlNode two = add(ElementType.COMPONENT, 280, 100);
      Rectangle2D first = one.getAbsoluteBounds(), second = two.getAbsoluteBounds();
      MultiSelection selection = new MultiSelection(editor, Arrays.<DiagramElement>asList(one, two));
      selection.startDragging(120, 130);
      selection.stopDragging(120, 130);
      assertEquals(first, one.getAbsoluteBounds());
      assertEquals(second, two.getAbsoluteBounds());
      assertFalse(editor.canUndo());
    });
  }

  public void testReverseBandStillSelectsOnlyDirectChildren() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      PackageElement pkg = (PackageElement) add(ElementType.PACKAGE, 100, 100);
      pkg.setSize(200, 150);
      UmlNode nested = diagram.getElementFactory().createNode(ElementType.NOTE);
      pkg.addChild(nested);
      nested.setSize(70, 40);
      nested.setAbsolutePos(140, 150);
      UmlNode other = add(ElementType.COMPONENT, 380, 100);
      band(520, 280, 60, 60);
      assertEquals(Arrays.<DiagramElement>asList(pkg, other), editor.getSelectedElements());
    });
  }

  public void testNestedNodeRemainsIndividuallySelectable() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      PackageElement pkg = (PackageElement) add(ElementType.PACKAGE, 100, 100);
      pkg.setSize(200, 150);
      UmlNode nested = diagram.getElementFactory().createNode(ElementType.CLASS);
      pkg.addChild(nested);
      nested.setSize(70, 40);
      nested.setAbsolutePos(140, 150);
      Object originalParent = nested.getParent();
      click(160, 180);
      assertEquals(Arrays.<DiagramElement>asList(nested), editor.getSelectedElements());
      assertSame(originalParent, nested.getParent());
    });
  }
}
