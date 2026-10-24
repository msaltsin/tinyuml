package test.tinyuml.ui.diagram;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import junit.framework.TestCase;
import org.tinyuml.draw.LabelSource;
import org.tinyuml.draw.SimpleLabel;
import org.tinyuml.model.ElementType;
import org.tinyuml.model.UmlModelImpl;
import org.tinyuml.ui.AppFrame;
import org.tinyuml.ui.diagram.DiagramEditor;
import org.tinyuml.ui.diagram.commands.AddNodeCommand;
import org.tinyuml.ui.diagram.commands.SetLabelTextCommand;
import org.tinyuml.umldraw.shared.UmlNode;
import org.tinyuml.umldraw.structure.StructureDiagram;

public class LaboratoryFrameTest extends TestCase {
  private AppFrame frame;
  protected void setUp() throws Exception {
    SwingUtilities.invokeAndWait(() -> frame = new AppFrame());
  }
  protected void tearDown() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        Field timer = AppFrame.class.getDeclaredField("timer");
        timer.setAccessible(true);
        ((java.util.Timer) timer.get(frame)).cancel();
      } catch (Exception error) { throw new AssertionError(error); }
      frame.dispose();
    });
  }
  private Component find(Container parent, String name) {
    for (Component child : parent.getComponents()) {
      if (name.equals(child.getName())) return child;
      if (child instanceof Container) {
        Component found = find((Container) child, name);
        if (found != null) return found;
      }
    }
    return null;
  }
  private String counts() { return ((JLabel) find(frame, "diagramCounts")).getText(); }
  private String selection() { return ((JLabel) find(frame, "diagramSelection")).getText(); }
  private UmlNode add(DiagramEditor editor, ElementType type, int x) {
    UmlNode node = editor.getDiagram().getElementFactory().createNode(type);
    node.setParent(editor.getDiagram());
    node.setSize(120, 70);
    editor.execute(new AddNodeCommand(editor, editor.getDiagram(), node, x, 100));
    return node;
  }
  private void select(DiagramEditor editor, UmlNode node) {
    editor.setSelectionMode();
    editor.mouseClicked(new MouseEvent(editor, MouseEvent.MOUSE_CLICKED,
      System.currentTimeMillis(), 0, (int) node.getAbsoluteX1() + 20,
      (int) node.getAbsoluteY1() + 30, 1, false, MouseEvent.BUTTON1));
  }
  private void openTab(StructureDiagram diagram) {
    try {
      Method method = AppFrame.class.getDeclaredMethod("createEditor", StructureDiagram.class);
      method.setAccessible(true);
      method.invoke(frame, diagram);
    } catch (Exception error) { throw new AssertionError(error); }
  }
  public void testStatusChangesAfterCreateDeleteUndoRedo() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DiagramEditor editor = frame.getCurrentEditor();
      UmlNode node = add(editor, ElementType.CLASS, 100);
      assertTrue(counts().contains("Class: 01"));
      select(editor, node);
      assertTrue(selection().contains("Class: Class 1"));
      frame.delete();
      assertTrue(counts().startsWith("Total Items: 00"));
      assertTrue(selection().endsWith("Seleccionados: 00"));
      editor.undo();
      assertTrue(counts().startsWith("Total Items: 01"));
      editor.redo();
      assertTrue(counts().startsWith("Total Items: 00"));
    });
  }
  public void testCopyPasteAndUndoUpdateStatus() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DiagramEditor editor = frame.getCurrentEditor();
      UmlNode node = add(editor, ElementType.CLASS, 100);
      select(editor, node);
      frame.copy();
      frame.paste();
      assertTrue(counts().contains("Class: 02"));
      editor.undo();
      assertTrue(counts().contains("Class: 01"));
      editor.redo();
      assertTrue(counts().contains("Class: 02"));
    });
  }
  public void testEscapeClearsSelectionAndCounter() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DiagramEditor editor = frame.getCurrentEditor();
      select(editor, add(editor, ElementType.COMPONENT, 100));
      assertTrue(selection().endsWith("01"));
      editor.getActionMap().get("cancelEditing").actionPerformed(null);
      assertEquals("Sin selecci\u00f3n | Seleccionados: 00", selection());
    });
  }
  public void testRenamedSelectedElementRefreshes() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DiagramEditor editor = frame.getCurrentEditor();
      UmlNode node = add(editor, ElementType.CLASS, 100);
      select(editor, node);
      ((LabelSource) node).setLabelText("Pedido");
      editor.notifyElementResized(node);
      assertTrue(selection().contains("Class: Pedido"));
    });
  }
  public void testLabelEditingUndoRedoRefreshesSelectedName() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DiagramEditor editor = frame.getCurrentEditor();
      UmlNode node = add(editor, ElementType.CLASS, 100);
      select(editor, node);
      SimpleLabel label = new SimpleLabel();
      label.setSource((LabelSource) node);
      editor.execute(new SetLabelTextCommand(label, "Pedido"));
      assertTrue(selection().contains("Class: Pedido"));
      editor.undo();
      assertTrue(selection().contains("Class: Class 1"));
      editor.redo();
      assertTrue(selection().contains("Class: Pedido"));
    });
  }
  public void testActiveTabAndBackgroundEventsAreIndependent() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DiagramEditor first = frame.getCurrentEditor();
      UmlNode firstNode = add(first, ElementType.CLASS, 100);
      select(first, firstNode);
      StructureDiagram second = new StructureDiagram(new UmlModelImpl());
      second.setLabelText("Otro diagrama");
      openTab(second);
      DiagramEditor secondEditor = frame.getCurrentEditor();
      add(secondEditor, ElementType.NOTE, 100);
      assertTrue(counts().contains("Note: 01"));
      first.notifyElementAdded(firstNode);
      assertTrue(counts().contains("Note: 01"));
      JTabbedPane tabs = (JTabbedPane) first.getParent().getParent().getParent().getParent();
      tabs.setSelectedIndex(0);
      assertSame(first, frame.getCurrentEditor());
      assertTrue(counts().contains("Class: 01"));
      assertTrue(counts().contains("Note: 00"));
      assertTrue(selection().contains("Class:"));
    });
  }
  public void testNewModelResetsCountsAndSelection() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DiagramEditor editor = frame.getCurrentEditor();
      select(editor, add(editor, ElementType.PACKAGE, 100));
      editor.clearUndoManager();
      frame.newModel();
      assertTrue(counts().startsWith("Total Items: 00"));
      assertTrue(selection().endsWith("Seleccionados: 00"));
    });
  }
  public void testPersistedDiagramRecalculatesCountsWhenOpened() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        DiagramEditor editor = frame.getCurrentEditor();
        add(editor, ElementType.CLASS, 100);
        add(editor, ElementType.NOTE, 280);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new ObjectOutputStream(bytes).writeObject(editor.getDiagram());
        StructureDiagram restored = (StructureDiagram) new ObjectInputStream(
          new ByteArrayInputStream(bytes.toByteArray())).readObject();
        openTab(restored);
        assertTrue(counts().startsWith("Total Items: 02"));
        assertTrue(counts().contains("Class: 01"));
        assertTrue(counts().contains("Note: 01"));
        assertTrue(selection().endsWith("Seleccionados: 00"));
      } catch (Exception error) { throw new AssertionError(error); }
    });
  }
}
