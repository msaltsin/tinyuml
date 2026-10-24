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

public class ViewportFitTest extends TestCase {
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
  public void testFitLargeCanvasChangesOnlyTheView() {
    UmlNode node = add(ElementType.CLASS, 3000, 2000);
    Rectangle2D oldBounds = node.getAbsoluteBounds();
    double width = diagram.getSize().getWidth(), height = diagram.getSize().getHeight();
    JViewport viewport = new JViewport();
    viewport.setExtentSize(new Dimension(640, 360));
    viewport.setView(editor);
    viewport.setViewPosition(new Point(500, 500));
    editor.clearUndoManager();
    editor.fitToWindow();
    assertTrue(editor.getScaling().getScaleFactor() < 0.5);
    assertTrue(editor.getPreferredSize().width <= viewport.getExtentSize().width);
    assertTrue(editor.getPreferredSize().height <= viewport.getExtentSize().height);
    assertEquals(new Point(0, 0), viewport.getViewPosition());
    assertEquals(oldBounds, node.getAbsoluteBounds());
    assertEquals(width, diagram.getSize().getWidth());
    assertEquals(height, diagram.getSize().getHeight());
    assertFalse(editor.canUndo());
  }

  public void testFitEmptyDiagramAndZeroSizedViewport() {
    editor.fitToWindow();
    JViewport viewport = new JViewport();
    viewport.setView(editor);
    viewport.setExtentSize(new Dimension(0, 0));
    editor.fitToWindow();
    assertEquals(1.0, editor.getScaling().getScaleFactor());
    viewport.setExtentSize(new Dimension(400, 250));
    editor.fitToWindow();
    assertTrue(editor.getScaling().getScaleFactor() > 0);
    assertEquals(0, diagram.getChildren().size());
  }

  public void testPresetZoomUpdatesScrollableSizeAndRemainsAvailable() {
    int width = editor.getTotalCanvasSize().width;
    editor.setScaling(Scaling.SCALING_150);
    assertEquals((int) Math.ceil(width * 1.5), editor.getPreferredSize().width);
    editor.setScaling(Scaling.SCALING_50);
    assertEquals((int) Math.ceil(width * 0.5), editor.getPreferredSize().width);
  }

  public void testInvalidScaleIsRejected() {
    for (double factor : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
      try { Scaling.forFactor(factor); fail("Invalid factor accepted"); }
      catch (IllegalArgumentException expected) { }
    }
  }
}
