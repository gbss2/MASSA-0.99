import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;

import jade.wrapper.*;

public class Runmas {
	public static String slash;
	public static String os;
	public static String configpath;
	public static String userdir;
	public static String configfile;

	private static int analysistype;
	private static int pquery;
	
	public static void main(String[] args) {
		slash = System.getProperty("file.separator");
		os    = System.getProperty("os.name");
		userdir = System.getProperty("user.dir");
		configpath = userdir+slash+"config"+slash;
		configfile = configpath+"masconfig.txt";
		
		System.out.println("-------------------------------------------------");
		System.out.println("MAS Annotation System (c) LDGH and EPIGEN Project");
		System.out.println("-------------------------------------------------");
		
		readConfigFile();
		
		try {
			Runtime rt = Runtime.instance();
			rt.setCloseVM(true);
			if(args.length > 0) {
				if(args[0].equalsIgnoreCase("-container")) { LaunchContainer(rt); }
			}
			LaunchMainContainer(rt);
		} catch(Exception e) { e.printStackTrace(); }
	}

	public static void readConfigFile() {
		BufferedReader inputStream;
		String[] values;

		System.out.println("System is reading "+configfile+"...");

		try {
			inputStream = new BufferedReader(new FileReader(configfile));
			String l;
			while ((l = inputStream.readLine()) != null) {
				values = l.split("=");
				if(values[0].equals("analysis")){ setAnalysistype(Integer.parseInt(values[1])); }
				if(values[0].equals("pquery")){ setPquery(Integer.parseInt(values[1])); }
			}
		} catch (IOException e) { e.printStackTrace(); }

		System.out.println("Analysis type    = "+getAnalysistype()+".");	
		System.out.println("Parallel queries = "+getPquery()+".");
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

		String dbsnpagent_name;
		String goagent_name;
		String pharmagent_name;
		String ucscagent_name;
		String omim_name;
		String hugo_name;
		String gwas_name;
		String polyphen_name;
		String provean_name;
		String reactome_name;
		
		if (getAnalysistype() == 2){
			for (int i = 1; i <= (getPquery()+2); i++) {
				try{
					dbsnpagent_name = "dbsnp"+Integer.toString(i);
					AgentController dbsnpagent = cont.createNewAgent(dbsnpagent_name,"DBsnpAgent", new Object[0]);
					dbsnpagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
			}
		}

		if (getAnalysistype() == 4){
			for (int i = 1; i <= (getPquery()+2); i++) {
				try{
					dbsnpagent_name = "dbsnp"+Integer.toString(i);
					AgentController dbsnpagent = cont.createNewAgent(dbsnpagent_name,"DBsnpAgent", new Object[0]);
					dbsnpagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }

				try{
					goagent_name = "geneontology"+Integer.toString(i);
					AgentController goagent = cont.createNewAgent(goagent_name,"GOAgent", new Object[0]);
					goagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
				
				try{
					pharmagent_name = "pharmgkb"+Integer.toString(i);
					AgentController pharmgkbagent = cont.createNewAgent(pharmagent_name,"PharmgkbAgent", new Object[0]);
					pharmgkbagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
				
				try{
					ucscagent_name = "ucsc"+Integer.toString(i);
					AgentController ucscagent = cont.createNewAgent(ucscagent_name,"UCSCAgent", new Object[0]);
					ucscagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }

				try{
					omim_name = "omim"+Integer.toString(i);
					AgentController omimagent = cont.createNewAgent(omim_name,"omimAgent", new Object[0]);
					omimagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
				
				try{
					hugo_name = "hugo"+Integer.toString(i);
					AgentController hugoagent = cont.createNewAgent(hugo_name,"HGNCAgent", new Object[0]);
					hugoagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
			}
		}
		
		if (getAnalysistype() == 1){
			for (int i = 1; i <= (getPquery()+2); i++) {
				try{
					dbsnpagent_name = "dbsnp"+Integer.toString(i);
					AgentController dbsnpagent = cont.createNewAgent(dbsnpagent_name,"DBsnpLocalAgent", new Object[0]);
					dbsnpagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
			}
		}
		
		if (getAnalysistype() == 3){
			for (int i = 1; i <= (getPquery()+2); i++) {
				try{
					dbsnpagent_name = "dbsnp"+Integer.toString(i);
					AgentController dbsnpagent = cont.createNewAgent(dbsnpagent_name,"DBsnpLocalAgent", new Object[0]);
					dbsnpagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
				
				try{
					goagent_name = "geneontology"+Integer.toString(i);
					AgentController goagent = cont.createNewAgent(goagent_name,"GOLocalAgent", new Object[0]);
					goagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
				
				try{
					pharmagent_name = "pharmgkb"+Integer.toString(i);
					AgentController pharmagent = cont.createNewAgent(pharmagent_name,"PharmgkbLocalAgent", new Object[0]);
					pharmagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
				
				try{
					omim_name = "omim"+Integer.toString(i);
					AgentController omimagent = cont.createNewAgent(omim_name,"omimLocalAgent", new Object[0]);
					omimagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }

				try{
					ucscagent_name = "ucsc"+Integer.toString(i);
					AgentController ucscagent = cont.createNewAgent(ucscagent_name,"UCSCLocalAgent", new Object[0]);
					ucscagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
				
				try{
					hugo_name = "hugo"+Integer.toString(i);
					AgentController hugoagent = cont.createNewAgent(hugo_name,"HGNCLocalAgent", new Object[0]);
					hugoagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
				
				try{
					gwas_name = "gwas"+Integer.toString(i);
					AgentController gwasagent = cont.createNewAgent(gwas_name,"GWAScatalogLocalAgent", new Object[0]);
					gwasagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }

				try{
					polyphen_name = "polyphen"+Integer.toString(i);
					AgentController polyphenagent = cont.createNewAgent(polyphen_name,"PolyphenLocalAgent", new Object[0]);
					polyphenagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }

				try{
					provean_name = "provean"+Integer.toString(i);
					AgentController proveanagent = cont.createNewAgent(provean_name,"ProveanLocalAgent", new Object[0]);
					proveanagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }

				try{
					reactome_name = "reactome"+Integer.toString(i);
					AgentController reactomeagent = cont.createNewAgent(reactome_name,"reactomeLocalAgent", new Object[0]);
					reactomeagent.start();
				} catch(StaleProxyException ie) { ie.printStackTrace(); }
			}
		}

		try{
			AgentController dbsnpagent = cont.createNewAgent("snptogene","SnpToGeneAgent", new Object[0]);
			dbsnpagent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }
		
		try{
			AgentController coordinatoragent = cont.createNewAgent("coordinator","CoordinatorAgent", new Object[0]);
			coordinatoragent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }

		try{
			AgentController interfaceagent = cont.createNewAgent("interface","InterfaceAgent", new Object[0]);
			interfaceagent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }
		
		try{
			AgentController enrichAgent = cont.createNewAgent("EnrichAgent","EnrichAgent", new Object[0]);
			enrichAgent.start();
		} catch(StaleProxyException ie) { ie.printStackTrace(); }
	}

	public static void LaunchContainer(Runtime rt){
		Profile p = new ProfileImpl(false);
		System.out.println("Launching the agent container ..."+p);
	}

	public static int getAnalysistype() {
		return analysistype;
	}

	public static void setAnalysistype(int at) {
		analysistype = at;
	}

	public static int getPquery() {
		return pquery;
	}

	public static void setPquery(int pq) {
		pquery = pq;
	}
}