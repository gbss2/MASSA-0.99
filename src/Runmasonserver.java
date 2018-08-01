import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;

import jade.wrapper.*;

public class Runmasonserver {
	public static void main(String[] args) {
		try {
			Runtime rt = Runtime.instance();
			rt.setCloseVM(true);
			if(args.length > 0) {
				if(args[0].equalsIgnoreCase("-container")) { LaunchContainer(rt); }
			}
			LaunchMainContainer(rt);
		} catch(Exception e) { e.printStackTrace(); }
	}

	public static void LaunchMainContainer(Runtime rt){
		String setlocalhostl = "127.0.0.1";
		Profile pMain = new ProfileImpl(setlocalhostl, 8888, null);

		System.out.println("Launching a whole in-process platform..."+pMain);

		rt.createMainContainer(pMain);
		ProfileImpl pContainer = new ProfileImpl(null, 8888, null);
		System.out.println("Launching the agent container ..."+pContainer);
		ContainerController cont = rt.createAgentContainer(pContainer);

		System.out.println("Launching agents on main container ...");

		try{
			AgentController dbsnpagent = cont.createNewAgent("dbsnp","DBsnpLocalAgent", new Object[0]);
			dbsnpagent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }

		try{
			AgentController dbsnpagent2 = cont.createNewAgent("dbsnp2","DBsnpLocalAgent", new Object[0]);
			dbsnpagent2.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }

		try{
			AgentController dbsnpagent3 = cont.createNewAgent("dbsnp3","DBsnpLocalAgent", new Object[0]);
			dbsnpagent3.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }
		
		try{
			AgentController dbsnpagent4 = cont.createNewAgent("dbsnp4","DBsnpLocalAgent", new Object[0]);
			dbsnpagent4.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }

		try{
			AgentController dbsnpagent5 = cont.createNewAgent("dbsnp5","DBsnpLocalAgent", new Object[0]);
			dbsnpagent5.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }
		
		try{
			AgentController dbsnpagent = cont.createNewAgent("snptogene","SnpToGeneAgent", new Object[0]);
			dbsnpagent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }
		
		try{
			AgentController dbsnpagent = cont.createNewAgent("pharmgkb","PharmgkbAgent", new Object[0]);
			dbsnpagent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }

		try{
			AgentController goagent = cont.createNewAgent("geneontology","GOAgent", new Object[0]);
			goagent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }
		
		try{
			AgentController goagent = cont.createNewAgent("ucsc","UCSCAgent", new Object[0]);
			goagent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }

		try{
			AgentController coordinatoragent = cont.createNewAgent("coordinator","CoordinatorAgent", new Object[0]);
			coordinatoragent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }

		try{
			AgentController interfaceagent = cont.createNewAgent("interface","InterfaceAgent", new Object[0]);
			interfaceagent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }
	}

	public static void LaunchContainer(Runtime rt){
		Profile p = new ProfileImpl(false);
		System.out.println("Launching the agent container ..."+p);
	}
}