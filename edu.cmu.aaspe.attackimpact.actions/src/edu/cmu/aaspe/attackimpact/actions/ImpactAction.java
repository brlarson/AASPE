package edu.cmu.aaspe.attackimpact.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.provider.StyledString.Style.BorderStyle;
import org.eclipse.sirius.diagram.DDiagram;
import org.eclipse.sirius.diagram.DDiagramElement;
import org.eclipse.sirius.diagram.DNode;
import org.eclipse.sirius.diagram.LineStyle;
import org.eclipse.sirius.diagram.NodeStyle;
//import org.eclipse.sirius.diagram.business.internal.metamodel.spec.SquareSpec;
import org.eclipse.sirius.diagram.description.style.SquareDescription;
import org.eclipse.sirius.viewpoint.RGBValues;
import org.eclipse.sirius.viewpoint.Style;
import org.eclipse.sirius.viewpoint.description.ColorDescription;
import org.eclipse.sirius.viewpoint.description.SystemColor;
import org.eclipse.sirius.viewpoint.description.style.StyleDescription;

import edu.cmu.attackimpact.AttackImpactFactory;
import edu.cmu.attackimpact.Node;
import edu.cmu.attackimpact.Propagation;
import edu.cmu.attackimpact.Vulnerability;


public class ImpactAction implements org.eclipse.sirius.tools.api.ui.IExternalJavaAction {

	private DDiagram diagram = null;
	private List<Node> browsedNodes;
	
	public DDiagramElement findNode (Node node)
	{
		for (DDiagramElement el : diagram.getDiagramElements())
		{
			if (el.getTarget() == node)
			{
				return el;
			}
		}
		return null;
	}
	
	public void changeColor (Node node)
	{
		DNode dnode = (DNode) findNode (node);
		
		if (dnode == null)
		{
			return;
		}
		
		System.out.println ("[ImpactAction] node=" + node);
//		SquareSpec s = (SquareSpec)dnode.getOwnedStyle();
//		s.setWidth(400);
//		s.setBorderSizeComputationExpression("10");
//		s.setColor(RGBValues.create(255, 0, 0));

		if (dnode.getStyle().getCustomFeatures().contains("color") == false)
		{
			dnode.getStyle().getCustomFeatures().add( "color");
		}
//		dnode.setOwnedStyle(s);
//		dnode.getOwnedStyle().refresh();
//		dnode.refresh();
	}
	
	public void browsePropagation (Node node)
	{
		if (browsedNodes.contains(node))
		{
			return;
		}
		
		browsedNodes.add(node);
		
		changeColor(node);
		System.out.println ("[ImpactAction] dest=" + node.getName());

		for (Propagation prop : node.getPropagations())
		{
			for (Node dest : prop.getDestinations())
			{
				System.out.println ("[ImpactAction] dest=" + dest.getName());

				browsePropagation(dest);
			}
		}
	}
	
	
	@Override
	public void execute(Collection<? extends EObject> selections, Map<String, Object> parameters) {

		System.out.println("[CreateVulnerability] calling execute");
		browsedNodes = new ArrayList<Node>();
		for (EObject eo : selections) {
			EObject target = null;

			if (eo instanceof DNode)
			{
				DNode dnode = (DNode) eo;

				Utils.clearDiagram(dnode.getParentDiagram());

				this.diagram = dnode.getParentDiagram();
				
				
				if (dnode.getTarget() instanceof Node)
				{
					browsePropagation ((Node)dnode.getTarget());
				}
				
				if (dnode.getTarget() instanceof Vulnerability)
				{
					Vulnerability vulnerability = (Vulnerability) dnode.getTarget();

					/**
					 * First, we show the impact on the associated component.
					 */
					Node relatedNode = (Node) vulnerability.eContainer();
					browsePropagation (relatedNode);
										
					/**
					 * Then, we also show the associated propagations.
					 */
					for (Propagation prop : vulnerability.getPropagations())
					{
						for (Node destination : prop.getDestinations())
						{
							browsePropagation (destination);
						}
					}
					
										
					System.out.println ("[ImpactAction] vulnerability=" + vulnerability);
				}
			}

		}
	}

	@Override
	public boolean canExecute(Collection<? extends EObject> selections) {
		System.out.println("[ImpactAction] calling execute");
		for (EObject eo : selections) {
			EObject target = null;

			if (eo instanceof DNode)
			{
				DNode dnode = (DNode) eo;
				if (dnode.getTarget() instanceof Node)
				{
					return true;
				}
				
				if (dnode.getTarget() instanceof Vulnerability)
				{
					return true;
				}
			}
		}
		return false;
	}

}
