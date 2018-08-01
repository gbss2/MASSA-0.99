import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import jade.core.ContainerID;
import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;

import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.lang.acl.ACLMessage;

public class Runmassa {
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
			LaunchContainer(rt);
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
		ProfileImpl pContainer = new ProfileImpl(null, 8888, null);
		System.out.println("Launching the agent container ..."+pContainer);
		for(ContainerID cid:getContainerIDs()){ System.out.println("------>CONTAINER: "+cid.getName()); }
	}

	protected static List<ContainerID> getContainerIDs(){
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.setProtocol(jade.domain.FIPANames.InteractionProtocol.FIPA_REQUEST);
		msg.setOntology(JADEManagementOntology.NAME);
		msg.setLanguage(jade.domain.FIPANames.ContentLanguage.FIPA_SL0);
		return null;
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