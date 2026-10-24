package test.tinyuml.ui.diagram;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.swing.JViewport;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.TestCase;
import org.tinyuml.draw.Scaling;
import org.tinyuml.model.ElementType;
import org.tinyuml.model.UmlModelImpl;
import org.tinyuml.ui.commands.PngExporter;
import org.tinyuml.ui.commands.SvgExporter;
import org.tinyuml.ui.diagram.DiagramEditor;
import org.tinyuml.umldraw.shared.UmlNode;
import org.tinyuml.umldraw.structure.StructureDiagram;

/** Verifies export and paint behavior affected by the RF-005 view transform. */
public class LaboratoryRenderingTest extends TestCase {
  private DiagramEditor editor;
  private UmlNode node;
  protected void setUp() {
    StructureDiagram diagram = new StructureDiagram(new UmlModelImpl());
    diagram.setLabelText("Export test");
    editor = new DiagramEditor(null, diagram);
    node = diagram.getElementFactory().createNode(ElementType.CLASS);
    diagram.addChild(node);
    node.setAbsolutePos(900, 500);
    BufferedImage image = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_RGB);
    editor.paintComponentNonScreen(image.getGraphics());
  }
  public void testFitKeepsPngExportDimensionsAndGeometry() throws Exception {
    Dimension expected = editor.getTotalCanvasSize();
    Rectangle2D bounds = node.getAbsoluteBounds();
    JViewport viewport = new JViewport();
    viewport.setView(editor);
    viewport.setExtentSize(new Dimension(400, 250));
    editor.fitToWindow();
    Path directory = Files.createTempDirectory(Paths.get("target"), "lab-png-");
    Path file = directory.resolve("diagram.png");
    try {
      new PngExporter().writePNG(editor, file.toFile());
      BufferedImage exported = ImageIO.read(file.toFile());
      assertEquals(expected.width, exported.getWidth());
      assertEquals(expected.height, exported.getHeight());
      assertEquals(bounds, node.getAbsoluteBounds());
      assertTrue(editor.getDiagram().isGridVisible());
    } finally { Files.deleteIfExists(file); Files.delete(directory); }
  }
  public void testSvgExportStillProducesAnSvgDocument() throws Exception {
    editor.setScaling(Scaling.forFactor(0.2));
    Path directory = Files.createTempDirectory(Paths.get("target"), "lab-svg-");
    Path file = directory.resolve("diagram.svg");
    try {
      new SvgExporter().writeSVG(editor, file.toFile());
      assertTrue(Files.size(file) > 100);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      assertEquals("svg", factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement().getTagName());
    } finally { Files.deleteIfExists(file); Files.delete(directory); }
  }
  public void testScreenPaintingDoesNotAlterTheCallersTransform() {
    editor.setScaling(Scaling.forFactor(0.25));
    BufferedImage image = new BufferedImage(400, 250, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setClip(0, 0, 400, 250);
    java.awt.geom.AffineTransform transform = graphics.getTransform();
    editor.paintComponent(graphics);
    assertEquals(transform, graphics.getTransform());
    graphics.dispose();
  }
}
