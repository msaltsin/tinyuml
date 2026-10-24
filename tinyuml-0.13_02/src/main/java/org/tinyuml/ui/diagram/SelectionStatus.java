package org.tinyuml.ui.diagram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.tinyuml.draw.Connection;
import org.tinyuml.draw.DiagramElement;
import org.tinyuml.draw.LabelSource;
import org.tinyuml.draw.NullElement;
import org.tinyuml.umldraw.shared.NoteElement;
import org.tinyuml.umldraw.shared.UmlDiagramElement;
import org.tinyuml.umldraw.structure.ClassElement;
import org.tinyuml.umldraw.structure.ComponentElement;
import org.tinyuml.umldraw.structure.PackageElement;
import org.tinyuml.umldraw.structure.StructureDiagram;

/** Describes the current flat selection without changing the diagram. */
public final class SelectionStatus {
  private SelectionStatus() { }

  /** The diagram background itself is never a selected item. */
  public static List<DiagramElement> selectedItems(StructureDiagram diagram,
      Collection<DiagramElement> selection) {
    List<DiagramElement> result = new ArrayList<DiagramElement>();
    for (DiagramElement element : selection) {
      if (element != null && element != diagram &&
          element != NullElement.getInstance() && !result.contains(element)) {
        result.add(element);
      }
    }
    return result;
  }

  public static String describeSelection(StructureDiagram diagram,
      Collection<DiagramElement> selection) {
    List<DiagramElement> items = selectedItems(diagram, selection);
    String information = "Sin selecci\u00f3n";
    if (items.size() == 1) {
      DiagramElement element = items.get(0);
      information = category(element) + ": " + name(element);
    } else if (items.size() > 1) {
      information = "Selecci\u00f3n m\u00faltiple";
    }
    return information;
  }

  public static String formatSelection(StructureDiagram diagram,
      Collection<DiagramElement> selection) {
    return describeSelection(diagram, selection) + String.format(Locale.ROOT,
      " | Seleccionados: %02d", selectedItems(diagram, selection).size());
  }

  private static String category(DiagramElement element) {
    if (element instanceof PackageElement) return "Package";
    if (element instanceof ClassElement) return "Class";
    if (element instanceof ComponentElement) return "Component";
    if (element instanceof NoteElement) return "Note";
    return element instanceof Connection ? "Relation" : "Element";
  }

  private static String name(DiagramElement element) {
    String value = null;
    if (element instanceof LabelSource) value = ((LabelSource) element).getLabelText();
    else if (element instanceof UmlDiagramElement &&
        ((UmlDiagramElement) element).getModelElement() != null) {
      value = ((UmlDiagramElement) element).getModelElement().getName();
    }
    if (value == null || value.trim().length() == 0) return "(sin nombre)";
    return value.replace('\n', ' ').replace('\r', ' ').trim();
  }
}
