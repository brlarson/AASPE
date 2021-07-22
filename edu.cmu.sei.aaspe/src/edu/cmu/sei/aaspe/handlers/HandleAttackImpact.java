//HandleAttackImpact.java

//conversion of DoAttackImpact for OSATE 2.9.2 by brl


package edu.cmu.sei.aaspe.handlers;

import java.util.Vector;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.modelsupport.AadlConstants;
import org.osate.aadl2.modelsupport.errorreporting.AnalysisErrorReporterManager;
import org.osate.aadl2.modelsupport.errorreporting.MarkerAnalysisErrorReporter;
import org.osate.aadl2.util.OsateDebug;
import org.osate.ui.dialogs.Dialog;

import edu.cmu.sei.aaspe.export.AttackImpactCsv;
import edu.cmu.sei.aaspe.export.AttackImpactExcel;
import edu.cmu.sei.aaspe.logic.AttackImpact;
import edu.cmu.sei.aaspe.model.PropagationModel;


public class HandleAttackImpact extends AbstractAaspeHandler
  {

  private static SystemInstance si = null;

  @Override
  public IStatus runJob(Element elem, IProgressMonitor monitor) {

  long startTime = System.currentTimeMillis();

    MessageConsole console = displayConsole();
//    console.clearConsole();
    
    PropagationModel.getInstance().reset();

    monitor.beginTask("Generate Attack Impact", IProgressMonitor.UNKNOWN);

    si = getSystemInstance(elem);
    if (si == null) {
      Dialog.showError(getToolName(), "Please select a system implementation or a system instance");
      return Status.CANCEL_STATUS;
    }

    writeToConsole("Attack Impact - starting");
    AttackImpact ai = new AttackImpact(si, monitor, new AnalysisErrorReporterManager(
        new MarkerAnalysisErrorReporter.Factory(
            AadlConstants.INSTANTIATION_OBJECT_MARKER)));
    ai.defaultTraversal(si);
    OsateDebug.osateDebug("AttackImpact", "List of vulnerabilities");


//    for (Vulnerability v : ai.getVulnerabilities()) {
//      OsateDebug.osateDebug("AttackImpact", v.toString());
//    }
    long endTime = System.currentTimeMillis();

    monitor.subTask("Writing attack impact spreadsheet file");

    AttackImpactExcel report = new AttackImpactExcel (ai);
    report.export();
    AttackImpactCsv csv = new AttackImpactCsv(ai);
    csv.export();
    OsateDebug.osateDebug("Attack Impact - finished in " + ((endTime - startTime) / 1000) + " s" );
    
    monitor.done();
    refreshWorkspace();

    return Status.OK_STATUS;
  }
  
public static SystemInstance getSystemInstance()  
{ return si; }

  
//  @Override
//  public Object execute(ExecutionEvent event) throws ExecutionException
//    {
//    long startTime = System.currentTimeMillis();
//    
//    OsateDebug.osateDebug("Attack Impact - starting");
//    
//    PropagationModel.getInstance().reset();
//
////    monitor.beginTask("Generate Attack Impact", IProgressMonitor.UNKNOWN);
//    // Get the root object of the model
////    Element root = obj.getElementRoot();
//
//    // Get the system instance (if any)
//    SystemInstance si;
//    if (obj instanceof InstanceObject) {
//      si = ((InstanceObject) obj).getSystemInstance();
//    } else {
//      si = null;
//    }
//
//    if (si != null) {
//      AttackImpact ai = new AttackImpact(si, monitor, getErrorManager());
//      ai.defaultTraversal(si);
//      OsateDebug.osateDebug("AttackImpact", "List of vulnerabilities");
//
//
////      for (Vulnerability v : ai.getVulnerabilities()) {
////        OsateDebug.osateDebug("AttackImpact", v.toString());
////      }
//      long endTime = System.currentTimeMillis();
//
//      monitor.subTask("Writing attack impact spreadsheet file");
//
//      AttackImpactExcel report = new AttackImpactExcel (ai);
//      report.export();
//  AttackImpactCsv csv = new AttackImpactCsv(ai);
//  csv.export();
//      OsateDebug.osateDebug("Attack Impact - finished in " + ((endTime - startTime) / 1000) + " s" );
//      
//      monitor.done();
//    } else {
//      Dialog.showError("System instance selection", "You must select a system instance to continue");
//    }
//
//    return null;
//    }

  }
