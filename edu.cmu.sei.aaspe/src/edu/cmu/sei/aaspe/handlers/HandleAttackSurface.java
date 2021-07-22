package edu.cmu.sei.aaspe.handlers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.modelsupport.AadlConstants;
import org.osate.aadl2.modelsupport.errorreporting.AnalysisErrorReporterManager;
import org.osate.aadl2.modelsupport.errorreporting.MarkerAnalysisErrorReporter;
import org.osate.aadl2.util.OsateDebug;
import org.osate.ui.dialogs.Dialog;

import edu.cmu.sei.aaspe.logic.AttackSurface;

public class HandleAttackSurface extends AbstractAaspeHandler
  {

  private static SystemInstance si = null;

  @Override
  protected IStatus runJob(Element sel, IProgressMonitor monitor)
    {
    /*
     * Doesn't make sense to set the number of work units, because the whole
     * point of this action is count the number of elements. To set the work
     * units we would effectively have to count everything twice.
     */
    monitor.beginTask("Generate Attack Surface", IProgressMonitor.UNKNOWN);
    // Get the root object of the model
//    Element root = sel.getElementRoot();

    // Get the system instance (if any)
    si = getSystemInstance(sel);
    if (si == null) {
      Dialog.showError(getToolName(), "Please select a system implementation or a system instance");
      return Status.CANCEL_STATUS;
    }

    if (si != null) {
      AttackSurface as = new AttackSurface(monitor,  new AnalysisErrorReporterManager(
          new MarkerAnalysisErrorReporter.Factory(
              AadlConstants.INSTANTIATION_OBJECT_MARKER)));
      as.defaultTraversal(si);
      OsateDebug.osateDebug("DoAttackSurface", "done");
    } else {
      Dialog.showError("System instance selection", "You must select a system instance to continue");
    }

    monitor.done();

    return null;
    }

  }
