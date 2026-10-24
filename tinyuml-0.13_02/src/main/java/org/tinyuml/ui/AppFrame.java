/**
 * Copyright 2007 Wei-ju Wu
 *
 * This file is part of TinyUML.
 *
 * TinyUML is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * TinyUML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TinyUML; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.tinyuml.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.tinyuml.draw.AbstractCompositeNode;
import org.tinyuml.umldraw.shared.NoteElement;
import org.tinyuml.umldraw.structure.ClassElement;
import org.tinyuml.umldraw.structure.ComponentElement;
import org.tinyuml.umldraw.structure.PackageElement;
import org.tinyuml.draw.Label;
import org.tinyuml.draw.DiagramElement;
import org.tinyuml.draw.LabelChangeListener;
import org.tinyuml.model.UmlModel;
import org.tinyuml.util.AppCommandListener;
import org.tinyuml.umldraw.structure.StructureDiagram;
import org.tinyuml.model.UmlModelImpl;
import org.tinyuml.ui.commands.ModelReader;
import org.tinyuml.ui.commands.ModelWriter;
import org.tinyuml.ui.commands.PngExporter;
import org.tinyuml.ui.commands.SvgExporter;
import org.tinyuml.ui.diagram.DiagramEditor;
import org.tinyuml.ui.diagram.SelectionStatus;
import org.tinyuml.ui.diagram.EditorMouseEvent;
import org.tinyuml.ui.diagram.EditorStateListener;
import org.tinyuml.ui.diagram.SelectionListener;
import org.tinyuml.util.ApplicationResources;
import org.tinyuml.util.MethodCall;

/**
 * This class implements the Application frame. The top-level UI elements are
 * created here. Application events that affect the entire application are
 * handled here, local event handlers are also installed.
 *
 * @author Wei-ju Wu
 * @version 1.0
 */


public class AppFrame extends JFrame
implements EditorStateListener, AppCommandListener, SelectionListener {

  private JTabbedPane tabbedPane;
  private JLabel coordLabel = new JLabel("    ");
  private JLabel memLabel = new JLabel("    ");
  private JLabel countLabel = new JLabel();
  private JLabel selectionLabel = new JLabel();
  private UmlModel umlModel;
  private DiagramEditor currentEditor;
  private transient Timer timer = new Timer();
  private transient StaticStructureEditorToolbarManager staticToolbarManager;
  private transient EditorCommandDispatcher editorDispatcher;
  private transient MainToolbarManager toolbarmanager;
  private transient MenuManager menumanager;
  private transient File currentFile;
  private LinkedList<DiagramEditor> diagramEditors;
  private Collection<DiagramElement> lastCopiedElements;
  private transient Map<String, MethodCall> selectorMap =
    new HashMap<String, MethodCall>();


  /**
   * Reset the transient values for serialization.
   * @param stream an ObjectInputStream
   * @throws IOException if I/O error occured
   * @throws ClassNotFoundException if class was not found
   */
  @SuppressWarnings("PMD.UnusedFormalParameter")
  private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
    timer = new Timer();
    staticToolbarManager = null;
    editorDispatcher = null;
    toolbarmanager = null;
    menumanager = null;
    currentFile = null;
    initSelectorMap();
  }

 

  /**
   * Creates a new instance of AppFrame.
   */
  public AppFrame() {
	
	diagramEditors = new LinkedList<DiagramEditor>();
	lastCopiedElements = null;
		
    setTitle(getResourceString("application.title"));
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    editorDispatcher = new EditorCommandDispatcher(this);

    getContentPane().add(createEditorArea(), BorderLayout.CENTER);

    installMainToolbar();
    installMenubar();
    installStatusbar();

    addWindowListener(new WindowAdapter() {
      /**
       * {@inheritDoc}
       */
      public void windowClosing(WindowEvent e) {
        quitApplication();
      }
    });
    newModel();
    pack();
    scheduleMemTimer();
    initSelectorMap();
    //setExtendedState(JFrame.MAXIMIZED_BOTH);
  }
 
  
 


  /**
   * Returns the currently displayed editor.
   * @return the current editor
   */
  public DiagramEditor getCurrentEditor() {
	  return currentEditor;
	  //int currentTabIndex = tabbedPane.getSelectedIndex();		
	  //return diagramEditors.get(currentTabIndex);
  }
  /**
   * Returns the last collection of items ready to copy. 
   * @return Collection of items ready to copy
   */
  public Collection<DiagramElement> getElementsToCopy(){
	  return lastCopiedElements;
  }
  /**
   * Returns the menu manager.
   * @return the menu manager
   */
  public MenuManager getMenuManager() {
    return menumanager;
  }

  /**
   * Creates the tabbed pane for the editor area.
   * @return the tabbed pane
   */
  private JTabbedPane createEditorArea() {
    tabbedPane = new JTabbedPane();
    tabbedPane.setPreferredSize(new Dimension(800, 600));
    tabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent event) {
        JPanel selected = (JPanel) tabbedPane.getSelectedComponent();
        currentEditor = selected == null ? null :
          (DiagramEditor) selected.getClientProperty("diagramEditor");
        if (selected != null) {
          staticToolbarManager = (StaticStructureEditorToolbarManager)
            selected.getClientProperty("diagramToolbar");
          updateMenuAndToolbars(currentEditor);
          selectionStateChanged();
        } else {
          updateSelectionStatus();
        }
      }
    });
    return tabbedPane;
  }

  /**
   * Creates an editor for the specified diagram and adds it to the tabbed
   * pane.
   * @param diagram the diagram
   */
  private void createEditor(StructureDiagram diagram) {
    DiagramEditor editor = new DiagramEditor(this, diagram);
    diagramEditors.add(editor);
    editor.addEditorStateListener(this);
    editor.addSelectionListener(this);
    editor.addAppCommandListener(editorDispatcher);
    editor.addAppCommandListener(this);
    JScrollPane spane = new JScrollPane(editor);
    JPanel editorPanel = new JPanel(new BorderLayout());
    spane.getVerticalScrollBar().setUnitIncrement(10);
    spane.getHorizontalScrollBar().setUnitIncrement(10);
    StaticStructureEditorToolbarManager diagramToolbar = new StaticStructureEditorToolbarManager();
    JToolBar toolbar = diagramToolbar.getToolbar();
    diagramToolbar.addCommandListener(editorDispatcher);
    editorPanel.putClientProperty("diagramEditor", editor);
    editorPanel.putClientProperty("diagramToolbar", diagramToolbar);
    editorPanel.add(spane, BorderLayout.CENTER);
    editorPanel.add(toolbar, BorderLayout.NORTH);
    final Component comp = tabbedPane.add(diagram.getLabelText(), editorPanel);
    tabbedPane.setSelectedComponent(comp);
    updateSelectionStatus();
    diagram.addNameLabelChangeListener(new LabelChangeListener() {
      /** {@inheritDoc} */
      public void labelTextChanged(Label label) {
        tabbedPane.setTitleAt(tabbedPane.indexOfComponent(comp),
                              label.getText());
      }
    });
  }

  /**
   * Adds the tool bar.
   */
  private void installMainToolbar() {
    toolbarmanager = new MainToolbarManager();
    toolbarmanager.addCommandListener(this);
    toolbarmanager.addCommandListener(editorDispatcher);
    getContentPane().add(toolbarmanager.getToolbar(), BorderLayout.NORTH);
  }

  /**
   * Adds the menubar.
   */
  private void installMenubar() {
    menumanager = new MenuManager();
    menumanager.addCommandListener(this);
    menumanager.addCommandListener(editorDispatcher);
    setJMenuBar(menumanager.getMenuBar());
  }

  /**
   * Adds a status bar.
   */
  private void installStatusbar() {
    JPanel statusbar = new JPanel(new BorderLayout());
    countLabel.setName("diagramCounts");
    selectionLabel.setName("diagramSelection");
    JPanel summaries = new JPanel(new GridLayout(2, 1));
    summaries.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 7, 3, 7));
    summaries.add(countLabel);
    summaries.add(selectionLabel);
    statusbar.add(summaries, BorderLayout.CENTER);
    JPanel runtime = new JPanel(new BorderLayout());
    runtime.add(coordLabel, BorderLayout.WEST);
    runtime.add(memLabel, BorderLayout.EAST);
    statusbar.add(runtime, BorderLayout.SOUTH);
    getContentPane().add(statusbar, BorderLayout.SOUTH);
  }

  /**
   * Returns the specified resource as a String object.
   * @param property the property name
   * @return the property value
   */
  private String getResourceString(String property) {
    return ApplicationResources.getInstance().getString(property);
  }

  private void updateElementCount(DiagramEditor editor) {
    // posiciones: 0 = Package, 1 = Class, 2 = Component, 3 = Note
    int[] counts = new int[4];
    countElements(editor.getDiagram(), counts);
    int total = counts[0] + counts[1] + counts[2] + counts[3];
    countLabel.setText(String.format(
      "Total Items: %02d | Package: %02d | Class: %02d | Component: %02d"
      + " | Note: %02d", total, counts[0], counts[1], counts[2], counts[3]));
  }

  private void countElements(AbstractCompositeNode node, int[] counts) {
    for (DiagramElement element : node.getChildren()) {
      if (element instanceof PackageElement) counts[0]++;
      else if (element instanceof ClassElement) counts[1]++;
      else if (element instanceof ComponentElement) counts[2]++;
      else if (element instanceof NoteElement) counts[3]++;
      if (element instanceof AbstractCompositeNode) {
        countElements((AbstractCompositeNode) element, counts);
      }
    }
  }

  private void updateSelectionStatus() {
    if (currentEditor != null) updateElementCount(currentEditor);
    else countLabel.setText("Total Items: 00 | Package: 00 | Class: 00 | Component: 00 | Note: 00");
    selectionLabel.setText(currentEditor == null ? "Sin selecci\u00f3n" :
      SelectionStatus.describeSelection(currentEditor.getDiagram(), currentEditor.getSelectedElements()));
    selectionLabel.setToolTipText(selectionLabel.getText());
  }

  // ************************************************************************
  // **** Event listeners
  // *****************************************

  // ************************************************************************
  // **** EditorStateListener
  // *****************************************

  /**
   * {@inheritDoc}
   */
  public void mouseMoved(EditorMouseEvent event) {
    coordLabel.setText(String.format("(%.1f, %.1f)", event.getX(),
      event.getY()));
  }

  /**
   * {@inheritDoc}
   */
  public void stateChanged(DiagramEditor editor) {
    updateMenuAndToolbars(editor);
  }

  /**
   * {@inheritDoc}
   */
  public void elementAdded(DiagramEditor editor) {
    if (editor != currentEditor) return;
    // spring loading is implemented here
    staticToolbarManager.doClick("SELECT_MODE");
    updateMenuAndToolbars(editor);
  }

  /**
   * {@inheritDoc}
   */
  public void elementRemoved(DiagramEditor editor) {
    updateMenuAndToolbars(editor);
  }

  /**
   * Query the specified editor state and set the menu and the toolbars
   * accordingly.
   * @param editor the editor
   */
  private void updateMenuAndToolbars(DiagramEditor editor) {
    if (editor == null || editor != currentEditor) return;
    updateSelectionStatus();
    menumanager.enableMenuItem("UNDO", editor.canUndo());
    menumanager.enableMenuItem("REDO", editor.canRedo());
    toolbarmanager.enableButton("UNDO", editor.canUndo());
    toolbarmanager.enableButton("REDO", editor.canRedo());
  }

  // ************************************************************************
  // **** SelectionListener
  // *****************************************

  /**
   * {@inheritDoc}
   */
  public void selectionStateChanged() {
    updateSelectionStatus();
    boolean hasSelection = currentEditor != null &&
      !SelectionStatus.selectedItems(currentEditor.getDiagram(), currentEditor.getSelectedElements()).isEmpty();
   
    menumanager.enableMenuItem("CUT", hasSelection);
    menumanager.enableMenuItem("COPY", hasSelection);
     
    menumanager.enableMenuItem("DELETE", hasSelection);
   
    toolbarmanager.enableButton("CUT", hasSelection);
    toolbarmanager.enableButton("COPY", hasSelection);
     
    toolbarmanager.enableButton("DELETE", hasSelection);
  }

  // ************************************************************************
  // **** CommandListener
  // *****************************************

  /**
   * Initializes the selector map.
   */
  private void initSelectorMap() {
    try {
      selectorMap.put("NEW_MODEL", new MethodCall(
        getClass().getMethod("newModel")));
      selectorMap.put("OPEN_MODEL", new MethodCall(
        getClass().getMethod("openModel")));
      selectorMap.put("SAVE_AS", new MethodCall(
        getClass().getMethod("saveAs")));
      selectorMap.put("SAVE", new MethodCall(
        getClass().getMethod("save")));
      selectorMap.put("EXPORT_GFX", new MethodCall(
        getClass().getMethod("exportGfx")));
      
      selectorMap.put("CUT", new MethodCall(
    	getClass().getMethod("cut")));
      selectorMap.put("COPY", new MethodCall(
    	getClass().getMethod("copy")));
      selectorMap.put("PASTE", new MethodCall(
        getClass().getMethod("paste")));
      
      selectorMap.put("DELETE", new MethodCall(
        getClass().getMethod("delete")));
      selectorMap.put("EDIT_SETTINGS", new MethodCall(
        getClass().getMethod("editSettings")));
      selectorMap.put("QUIT", new MethodCall(
        getClass().getMethod("quitApplication")));
      selectorMap.put("ABOUT", new MethodCall(
        getClass().getMethod("about")));
      selectorMap.put("HELP_CONTENTS", new MethodCall(
        getClass().getMethod("displayHelpContents")));
    } catch (NoSuchMethodException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void handleCommand(String command) {
    MethodCall methodcall = selectorMap.get(command);
    if (methodcall != null) {
      methodcall.call(this);
    } else {
      System.out.println("not handled: " + command);
    }
  }

  /**
   * Call this method to exit this application in a clean way.
   */
  public void quitApplication() {
    if (canQuit()) {
      timer.cancel();
      timer.purge();
      dispose();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Checks if application can be quit safely.
   * @return true if can quit safely, false otherwise
   */
  private boolean canQuit() {
    if (currentEditor.canUndo()) {
      return JOptionPane.showConfirmDialog(this,
        ApplicationResources.getInstance().getString("confirm.quit.message"),
        ApplicationResources.getInstance().getString("confirm.quit.title"),
        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    return true;
  }

  /**
   * Opens the settings editor.
   */
  public void editSettings() {
    System.out.println("EDIT SETTINGS");
  }

  /**
   * Creates a new model.
   */
  public void newModel() {
    if (canCreateNewModel()) {
      umlModel = new UmlModelImpl();
      StructureDiagram diagram = new StructureDiagram(umlModel);
      umlModel.addDiagram(diagram);
      diagram.setLabelText("Class diagram 1");
      diagramEditors.clear();
      tabbedPane.removeAll();
      createEditor(diagram);
      currentFile = null;
      updateFrameTitle();
    }
  }

  /**
   * Determines if a new model can be created.
   * @return true the model can be created, false otherwise
   */
  private boolean canCreateNewModel() {
    if (currentEditor != null && currentEditor.canUndo()) {
      return JOptionPane.showConfirmDialog(this,
        ApplicationResources.getInstance().getString("confirm.new.message"),
        ApplicationResources.getInstance().getString("confirm.new.title"),
        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    return true;
  }

  /**
   * Sets up and starts the timer task.
   */
  private void scheduleMemTimer() {
    TimerTask task = new TimerTask() {
      public void run() {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            memLabel.setText(getMemString());
          }
        });
      }
    };
    // every 5 seconds
    timer.schedule(task, 2000, 5000);
  }

  /**
   * Creates the memory information string.
   * @return the memory status string
   */
  private String getMemString() {
    long free = Runtime.getRuntime().freeMemory();
    long total = Runtime.getRuntime().totalMemory();
    long used = total - free;
    used /= (1024 * 1024);
    total /= (1024 * 1024);
    return String.format("used: %dM total: %dM   ", used, total);
  }

  /**
   * Exports graphics as SVG.
   */
  public void exportGfx() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle(getResourceString("dialog.exportgfx.title"));
    FileNameExtensionFilter svgFilter = new FileNameExtensionFilter(
      "Scalable Vector Graphics file (*.svg)", "svg");
    FileNameExtensionFilter pngFilter = new FileNameExtensionFilter(
      "Portable Network Graphics file (*.png)", "png");
    fileChooser.addChoosableFileFilter(svgFilter);
    fileChooser.addChoosableFileFilter(pngFilter);
    fileChooser.setAcceptAllFileFilterUsed(false);
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      if (fileChooser.getFileFilter() == svgFilter) {
        try {
          SvgExporter exporter = new SvgExporter();
          exporter.writeSVG(getCurrentEditor(), fileChooser.getSelectedFile());
        } catch (IOException ex) {
          JOptionPane.showMessageDialog(this, ex.getMessage(),
            getResourceString("error.exportgfx.title"),
            JOptionPane.ERROR_MESSAGE);
        }
      } else if (fileChooser.getFileFilter() == pngFilter) {
        try {
          PngExporter exporter = new PngExporter();
          exporter.writePNG(getCurrentEditor(), fileChooser.getSelectedFile());
        } catch (IOException ex) {
          JOptionPane.showMessageDialog(this, ex.getMessage(),
            getResourceString("error.exportgfx.title"),
            JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  /**
   * Returns the FileFilter for the TinyUML serialized model files.
   * @return the FileFilter
   */
  private FileNameExtensionFilter createModelFileFilter() {
    return new FileNameExtensionFilter(
      "TinyUML serialized model file (*.tsm)", "tsm");
  }

  /**
   * Opens a TinyUML model.
   */
  public void openModel() {
    if (canOpen()) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle(getResourceString("dialog.openmodel.title"));
      fileChooser.addChoosableFileFilter(createModelFileFilter());
      if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        try {
          currentFile = fileChooser.getSelectedFile();
          umlModel = ModelReader.getInstance().readModel(currentFile);
          diagramEditors.clear();
          tabbedPane.removeAll();
          for (org.tinyuml.model.UmlDiagram diagram : umlModel.getDiagrams()) {
            createEditor((StructureDiagram) diagram);
          }
          if (tabbedPane.getTabCount() > 0) tabbedPane.setSelectedIndex(0);
          updateFrameTitle();
        } catch (IOException ex) {
          JOptionPane.showMessageDialog(this, ex.getMessage(),
            getResourceString("error.readfile.title"),
            JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  /**
   * Checks if application can be quit safely.
   * @return true if can quit safely, false otherwise
   */
  private boolean canOpen() {
    if (currentEditor.canUndo()) {
      return JOptionPane.showConfirmDialog(this,
        ApplicationResources.getInstance().getString("confirm.open.message"),
        ApplicationResources.getInstance().getString("confirm.open.title"),
        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    return true;
  }

  /**
   * Saves the model with a file chooser.
   */
  public void saveAs() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle(getResourceString("dialog.saveas.title"));
    fileChooser.addChoosableFileFilter(createModelFileFilter());
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      currentFile = saveModelFile(fileChooser.getSelectedFile());
      updateFrameTitle();
    }
  }

  /**
   * Saves immediately if possible.
   */
  public void save() {
    if (currentFile == null) {
      saveAs();
    } else {
      saveModelFile(currentFile);
    }
  }

  /**
   * Writes the current model file. The returned file is different if the input
   * file does not have the tsm extension.
   * @param file the file to write
   * @return the file that was written
   */
  private File saveModelFile(File file) {
    File result = null;
    try {
      result = ModelWriter.getInstance().writeModel(this, file, umlModel);
      currentEditor.clearUndoManager();
      updateMenuAndToolbars(currentEditor);
    } catch (IOException ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(this, ex.getMessage(),
        getResourceString("error.savefile.title"), JOptionPane.ERROR_MESSAGE);
    }
    return result;
  }

  /**
   * Sets the frame title according to the current working file.
   */
  private void updateFrameTitle() {
    if (currentFile != null) {
      setTitle(ApplicationResources.getInstance()
        .getString("application.title") + " [" + currentFile.getName() + "]");
    } else {
      setTitle(ApplicationResources.getInstance()
        .getString("application.title"));
    }
  }
  /**
   * cut the current selection.
   */
  public void cut(){
	  copy();
	  delete();
	  selectionStateChanged();
  }
  /**
   * Keeps track of the last copied elements (the ones selected when
   * Ctrl+C was used).
   * Creado para cumplir con la solicitud E de la tarea.
   */
  public void copy(){
	  boolean hasSelection = getCurrentEditor().getSelectedElements().size() > 0;

	  if(hasSelection)
	    lastCopiedElements = getCurrentEditor().getSelectedElements();

	  //adicionalmente, hay que habilitar el botón PASTE!
	  menumanager.enableMenuItem("PASTE", hasSelection);
	  toolbarmanager.enableButton("PASTE", hasSelection);
  }
  /**
   * paste the current selection.
   */
  public void paste(){
		getCurrentEditor().pasteElement(lastCopiedElements);
  }
  /**
   * Deletes the current selection.
   */
  public void delete() {
    getCurrentEditor().deleteSelection();
  }

  /**
   * Shows the about dialog.
   */
  public void about() {
    JOptionPane.showMessageDialog(this, getResourceString("dialog.about.text"),
      getResourceString("dialog.about.title"), JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Displays the help contents.
   */
  public void displayHelpContents() {
    try {
      URI helpUri = new URI("http://www.tinyuml.org/Wikka/UserDocs");
      Desktop.getDesktop().browse(helpUri);
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this,
        getResourceString("error.nohelp.message"),
        getResourceString("error.nohelp.title"),
        JOptionPane.ERROR_MESSAGE);
    } catch (URISyntaxException ignore) {
      ignore.printStackTrace();
    }
  }
}
