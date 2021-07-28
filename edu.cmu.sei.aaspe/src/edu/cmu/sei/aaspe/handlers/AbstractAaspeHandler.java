package edu.cmu.sei.aaspe.handlers;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osate.aadl2.Element;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.ui.dialogs.Dialog;

import edu.cmu.sei.aaspe.util.SelectionHelper;

public abstract class AbstractAaspeHandler extends AbstractHandler implements IWorkbenchWindowActionDelegate
  {
  //methods for IWorkbenchWindowActionDelegate
  public void dispose() {}
  protected IWorkbenchWindow window;
  public void init(IWorkbenchWindow window) { this.window = window; }
  public void run(IAction action) {}
  public void selectionChanged(IAction action, ISelection selection)  {}
  protected abstract IStatus runJob(Element sel, IProgressMonitor monitor);

  protected String getToolName() {
    return "AASPE";
  }

  protected String getJobName() {
    return getToolName() + " job";
  }

  protected final String MARKER_TYPE = "com.multitude.bless.marker";

  protected static IResource getIResource(Resource r) {
    final URI uri = r.getURI();
    final IPath path = new Path(uri.toPlatformString(true));
    final IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
    if (resource == null) {
      throw new RuntimeException("Unable to get IResource for Resource: " + r);
    }
    return resource;
  }


  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException
    {
    Element elem = getElement(event);

    WorkspaceJob j = new WorkspaceJob(getJobName()) {
      @Override
      public IStatus runInWorkspace(final IProgressMonitor monitor) {
        return runJob(elem, monitor);
      }
    };

    j.setRule(ResourcesPlugin.getWorkspace().getRoot());
    j.schedule();
    return null;
    }


  private Element getElement(ExecutionEvent e) {
    Element root = AadlUtil.getElement(getCurrentSelection(e));

    if (root == null) {
      root = SelectionHelper.getSelectedSystemImplementation();
    }

    return root;
  }

  protected SystemInstance getSystemInstance(Element e) {
  if (e != null) {
    if (e instanceof SystemInstance) {
      return (SystemInstance) e;
    }
    if (e instanceof SystemImplementation) {
      try {
        SystemImplementation si = (SystemImplementation) e;

        writeToConsole("Generating System Instance ...");

        return InstantiateModel.buildInstanceModelFile(si);
      } catch (Exception ex) {
        Dialog.showError(getToolName(), "Could not instantiate model");
        ex.printStackTrace();
      }
    }
  }

  return null;
}

protected ComponentInstance getComponentInstance(ExecutionEvent e) {
  Element root = AadlUtil.getElement(getCurrentSelection(e));

  if (root == null) {
    root = SelectionHelper.getSelectedSystemImplementation();
  }

  if (root != null && root instanceof SystemImplementation) {
    try {
      SystemImplementation si = (SystemImplementation) root;

      root = InstantiateModel.buildInstanceModelFile(si);
    } catch (Exception ex) {
      Dialog.showError(getToolName(), "Could not instantiate model");
      return null;
    }
  }

  if (root != null && root instanceof ComponentInstance) {
    return (ComponentInstance) root;
  } else {
    return null;
  }
}

protected Object getCurrentSelection(ExecutionEvent event) {
ISelection selection = HandlerUtil.getCurrentSelection(event);
if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
  Object object = ((IStructuredSelection) selection).getFirstElement();
  return object;
} else {
  return null;
}
}

protected String getInstanceFilename(ComponentInstance root) {
return toIFile(root.eResource().getURI()).getName();
}

protected IProject getProject(ComponentInstance root) {
return toIFile(root.eResource().getURI()).getProject();
}

protected IPath getProjectPath(ComponentInstance e) {
return getProject(e).getLocation();
}

protected IPath getInstanceFilePath(ExecutionEvent e) {
Element root = getComponentInstance(e);
Resource res = root.eResource();
URI uri = res.getURI();
IPath path = toIFile(uri).getFullPath();
return path;
}

protected String writeGeneratedFile(ExecutionEvent e, String type, String content) {
IPath path = getInstanceFilePath(e);
path = path.removeFileExtension();
String filename = path.lastSegment() + "__" + type;
path = path.removeLastSegments(1).append("/.IR/" + type + "/" + filename);
path = path.addFileExtension(type);
IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
if (file != null) {
  final InputStream input = new ByteArrayInputStream(content.getBytes());
  try {
    if (file.exists()) {
      file.setContents(input, true, true, null);
    } else {
      AadlUtil.makeSureFoldersExist(path);
      file.create(input, true, null);
    }
  } catch (final CoreException excp) {
  }
  return file.getLocation().toString();
}
return null;
}

protected void writeFile(File out, String str) {
writeFile(out, str, true);
}

protected void writeFile(File out, String str, boolean confirm) {
try {
  BufferedWriter writer = new BufferedWriter(new FileWriter(out));
  writer.write(str);
  writer.close();
  if (confirm) {
    Dialog.showInfo(getToolName(), "Wrote: " + out.getAbsolutePath());
  }
} catch (Exception ee) {
  Dialog.showError(getToolName(),
      "Error encountered while trying to save file: " + out.getAbsolutePath() + "\n\n" + ee.getMessage());
}
}

private MessageConsole getConsole(String name) {
ConsolePlugin plugin = ConsolePlugin.getDefault();
IConsoleManager conMan = plugin.getConsoleManager();
IConsole[] existing = conMan.getConsoles();
for (int i = 0; i < existing.length; i++) {
  if (name.equals(existing[i].getName())) {
    return (MessageConsole) existing[i];
  }
}
// no console found, so create a new one
MessageConsole mc = new MessageConsole(name, null);
conMan.addConsoles(new IConsole[] { mc });

return mc;
}

protected MessageConsole displayConsole() {
return displayConsole(getToolName());
}

protected MessageConsole displayConsole(String name) {
MessageConsole ms = getConsole(name);
Display.getDefault().syncExec(() -> {
  try {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    IConsoleView view;
    view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
    view.display(ms);
  } catch (PartInitException e) {
    e.printStackTrace();
  }
});
return ms;
}

protected boolean writeToConsole(String text) {
return writeToConsole(text, false);
}

protected boolean writeToConsole(String text, boolean clearConsole) {
MessageConsole ms = displayConsole(getToolName());
if (clearConsole) {
  ms.clearConsole();
}
return writeToConsole(ms, text);
}

protected boolean writeToConsole(MessageConsole m, String text) {
boolean isWritten = false;
if (m != null) {
  MessageConsoleStream out = m.newMessageStream();
  out.println(text);
  isWritten = true;
  try {
    out.flush();
    out.close();
  } catch (IOException e) {
    e.printStackTrace();
  }
}
return isWritten;
}

protected Shell getShell() {
return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
}

protected void refreshWorkspace() {
try {
  ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
} catch (CoreException e) {
  e.printStackTrace();
}
}

public static IFile toIFile(URI resourceURI) {
/*
 * Ideally we'd just call OsateResourceUtil.toIFile however that is not
 * available in OSATE 2.4.x (which the CASE FM-IDE is based on). Workaround
 * is to just replicate the current behavior of that method, refer to
 * <a href=
 * "https://github.com/osate/osate2/blob/bed18dd95fe3f3bf54d657911cd5e5da1ff2718b/core/org.osate.aadl2.modelsupport/src/org/osate/aadl2/modelsupport/resources/OsateResourceUtil.java#L62"
 * >this</a>
 */

// return OsateResourceUtil.toIFile(resourceURI);

if (resourceURI.isPlatform()) {
  return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(resourceURI.toPlatformString(true)));
} else {
  return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(resourceURI.toFileString()));
}
}


  }
