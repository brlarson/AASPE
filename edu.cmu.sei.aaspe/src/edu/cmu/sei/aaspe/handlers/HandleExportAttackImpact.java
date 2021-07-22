package edu.cmu.sei.aaspe.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.ui.business.api.dialect.DialectUIManager;
import org.eclipse.sirius.viewpoint.DRepresentation;
import org.eclipse.sirius.viewpoint.description.RepresentationDescription;
import org.eclipse.sirius.viewpoint.description.Viewpoint;
import org.eclipse.xtext.ui.util.ResourceUtil;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.modelsupport.AadlConstants;
import org.osate.aadl2.modelsupport.errorreporting.AnalysisErrorReporterManager;
import org.osate.aadl2.modelsupport.errorreporting.MarkerAnalysisErrorReporter;
import org.osate.aadl2.util.OsateDebug;
import org.osate.ui.dialogs.Dialog;

import edu.cmu.attackimpact.Model;
import edu.cmu.sei.aaspe.export.AttackImpactModel;
import edu.cmu.sei.aaspe.logic.AttackImpact;
import edu.cmu.sei.aaspe.model.PropagationModel;
import edu.cmu.sei.aaspe.utils.SiriusUtil;

public class HandleExportAttackImpact extends AbstractAaspeHandler
  {


  private static SystemInstance si = null;

  @Override
  protected IStatus runJob(Element sel, IProgressMonitor monitor)
    {
    monitor.beginTask("Export to Attack Impact Model", IProgressMonitor.UNKNOWN);
    si = getSystemInstance(sel);
    if (si == null) {
      Dialog.showError(getToolName(), "Please select a system implementation or a system instance");
      return Status.CANCEL_STATUS;
    }

    long startTime = System.currentTimeMillis();

    OsateDebug.osateDebug("Export Attack Impact - starting");
    PropagationModel.getInstance().reset();
    AttackImpact ai = new AttackImpact(si, monitor,  new AnalysisErrorReporterManager(
        new MarkerAnalysisErrorReporter.Factory(
            AadlConstants.INSTANTIATION_OBJECT_MARKER)));
    ai.defaultTraversal(si);
    AttackImpactModel export = new AttackImpactModel(ai);
    Model attackImpactModel = export.getAttackImpactModel(true);
    IFile ifile = ResourceUtil.getFile(si.eResource());
    String filename = ifile.getFullPath().removeFirstSegments(1).toString();

//    String filename = ifile.getName();
    filename = filename.replace("aaxl2", "attackimpact");
//    OsateDebug.osateDebug("Filename=" + filename);
//    OsateDebug.osateDebug("s=" + s);

    monitor.subTask("Writing attack model file");

    URI newURI = EcoreUtil.getURI(si).trimFragment().trimSegments(2).appendSegment("attackimpact")
        .appendSegment("ai" + ".attackimpact");
    final IProject currentProject = ResourceUtil.getFile(si.eResource()).getProject();
    final URI modelURI = serializeAttackImpactModel(attackImpactModel, newURI, currentProject);
    autoOpenAttackImpactModel(modelURI, currentProject);
//    createAndOpenAttackImpact(currentProject, modelURI, monitor);
    long endTime = System.currentTimeMillis();
    OsateDebug.osateDebug("Export Attack Impact - finished in " + ((endTime - startTime) / 1000) + " s");

    return Status.OK_STATUS;
    }

  public void autoOpenAttackImpactModel(final URI newURI, final IProject activeProject) {

  try {

    Job attackImpactCreationJob = new Job("Creation of Attack Impact Graph") {

      @Override
      protected IStatus run(IProgressMonitor monitor) {

        monitor.beginTask("Creation of Attack Impact Graph", 100);

        createAndOpenAttackImpact(activeProject, newURI, monitor);
        try {
          activeProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        } catch (CoreException e) {
          // Error while refreshing the project
        }
        monitor.done();

        return Status.OK_STATUS;
      }
    };
    attackImpactCreationJob.setUser(true);
    attackImpactCreationJob.schedule();

  } catch (Exception e) {
    e.printStackTrace();
  }

}

private void createAndOpenAttackImpact(final IProject project, final URI attackImpactURI,
    IProgressMonitor monitor) {
  SiriusUtil util = SiriusUtil.INSTANCE;
  URI attackImpactViewpointURI = URI.createURI("viewpoint:/attackimpact.design/AttackImpact");

  URI semanticResourceURI = URI.createPlatformResourceURI(attackImpactURI.toPlatformString(true), true);
  Session existingSession = util.getSessionForProjectAndResource(project, semanticResourceURI, monitor);
  if (existingSession == null) {
    // give it a second try. null was returned the first time due to a class cast exception at the end of
    // setting the Modeling perspective.
    existingSession = util.getSessionForProjectAndResource(project, semanticResourceURI, monitor);
  }
  if (existingSession != null) {
    util.saveSession(existingSession, monitor);
    ResourceSetImpl resset = new ResourceSetImpl();
    Model model = getAttackImpactModelFromSession(existingSession, semanticResourceURI);
    // XXX this next piece of code tries to compensate for a bug in Sirius where it cannot find the Model
    // It should be there since the getSessionForProjectandResource would have put it there.
    if (model == null) {
      OsateDebug.osateDebug("Could not find semantic resource Attack Impact in session for URI "
          + semanticResourceURI.path());
      EObject res = resset.getEObject(attackImpactURI, true);
      if (res instanceof Model) {
        model = (Model) res;
      }
    }
    if (model == null) {
      OsateDebug.osateDebug("Could not find Attack Impact for URI " + attackImpactURI.path());
      return;
    }
    final Viewpoint attackImpactVP = util.getViewpoint(existingSession, attackImpactViewpointURI, monitor);
    final RepresentationDescription description = util.getRepresentationDescription(attackImpactVP,
        "AttackImpactDiagram");
    String representationName = model.getName() + " Graph";
    final DRepresentation rep = util.findRepresentation(existingSession, attackImpactVP, description,
        representationName);
    if (rep != null) {
      DialectUIManager.INSTANCE.openEditor(existingSession, rep, new NullProgressMonitor());
    } else {
      try {
        util.createAndOpenRepresentation(existingSession, attackImpactVP, description, representationName,
            model, monitor);
      } catch (Exception e) {
        OsateDebug.osateDebug("Could not create and open Attack Impact Model " + model.getName());
        return;
      }
    }

  }
}

private Model getAttackImpactModelFromSession(Session session, URI uri) {
  Resource resource = SiriusUtil.INSTANCE.getResourceFromSession(session, uri);
  if (resource != null) {
    for (EObject object : resource.getContents()) {
      if (object instanceof Model) {
        return (Model) object;
      }
    }
  }
  return null;
}

private static URI serializeAttackImpactModel(Model attackImpactModel, final URI newURI, IProject activeProject) {

  try {

    ResourceSet set = new ResourceSetImpl();
    Resource res = set.createResource(newURI);

    res.getContents().add(attackImpactModel);

//    FileOutputStream fos = new FileOutputStream(newFile.getRawLocation().toFile());
    res.save(null);
    OsateDebug.osateDebug("[AttackImpactModel]", "activeproject=" + activeProject.getName());
    activeProject.refreshLocal(IResource.DEPTH_INFINITE, null);
    return EcoreUtil.getURI(attackImpactModel);
  } catch (Exception e) {
    e.printStackTrace();
  }
  return newURI;

}

  }
