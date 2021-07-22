package edu.cmu.aaspe.attackimpact.actions;

import java.util.Collection;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.diagram.DDiagram;
import org.eclipse.sirius.diagram.DNode;


public class ClearImpactAction implements org.eclipse.sirius.tools.api.ui.IExternalJavaAction {

//	private DDiagram diagram = null;
	
	
	
	@Override
	public void execute(Collection<? extends EObject> selections, Map<String, Object> parameters) {

		System.out.println("[ClearImpactAction] calling execute");
		for (EObject eo : selections) {
			System.out.println("[ClearImpactAction] eo=" + eo);

			if (eo instanceof DDiagram)
			{
				Utils.clearDiagram ((DDiagram)eo);
			}
			
			if (eo instanceof DNode)
			{
				DNode dnode = (DNode) eo;
				
				Utils.clearDiagram(dnode.getParentDiagram());
			}

		}
	}

	@Override
	public boolean canExecute(Collection<? extends EObject> selections) {
		return true;
	}

}
