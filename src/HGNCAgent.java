import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Process;
import java.util.Iterator;
import java.util.Set;

import BioData_Models.HUGO;

public class HGNCAgent extends DBagent{
	private static final long serialVersionUID = 1L;

	/* Attributes */
	String path;
	String userdir;
	String service;
	BioData hugodata;

	/* Constructor */
	public HGNCAgent() {
		super();
		slash = System.getProperty("file.separator");
		os    = System.getProperty("os.name");		
		System.out.println("OS is "+os);
		
		this.setDBname("hugo");
		this.setInformation("hugo");
		this.userdir = System.getProperty("user.dir");
		if(os.equals("Linux")){
			this.path = this.userdir+slash+"divergenomenrich"+slash+"scripts-PharmGKB"+slash;
		}else{ this.path = this.userdir+slash+"scripts-pharmgkb"+slash; }

		this.service = "hugoPerl.pl";
		this.hugodata = new BioData("hugo");
	}

	/* Agent setup */
	protected void setup() {
		System.out.println("Agent " + getLocalName() + " started.");
		this.register();
		addBehaviour(new waitRequest());
	}

	/* Agent shutdown */
	
	
	/* BEHAVIOURS */
	
	/* Agent communication: wait for messages */
	class waitRequest extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		private MessageTemplate simplerequest_template;
		private AID sender;

		public waitRequest() {
			 MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
		}
		
		public void action() {
			ACLMessage msg = myAgent.receive(simplerequest_template);
			if (msg != null) {
				sender = msg.getSender();
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					BioData contentdata = (BioData)msg.getContentObject();
					addBehaviour(new hugoAction(sender,contentdata));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	} 

	/* Agent action: access DB -> access DB remote; send reply  */	
	class hugoAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public hugoAction(AID pa,BioData bd){
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessHUGO(bd), STATE_A);
			this.registerLastState(new SendReply(pa), STATE_B);
			this.registerDefaultTransition(STATE_A,STATE_B);
		}
		
		public int onEnd() {
			System.out.println("Agent " + getLocalName() +" finished task.");
			return super.onEnd();
		}
	}
	
	/* Agent reply: send message to Coordinator on task end */
	class SendReply extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private AID     msgreceiver;
		private String  msgperformative;
		private ACLMessage msg;

		public SendReply(AID p) {
			msgreceiver     = p;
			msgperformative = "INFORM";
		}
		
		public void action() {
			try {
				System.out.println("... sending "+msgperformative+" to agent: \""+msgreceiver.getName());
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(msgreceiver);
				msg.setLanguage("English");
				msg.setContentObject(hugodata);
				myAgent.send(msg);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}

	/* Agent action: parallel access to DB -> remote Access DB  */	
	class accessHUGO extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		BioData contentdata;

		public accessHUGO (BioData bd) {
			super(WHEN_ALL);
			contentdata = bd;
		}
		
		public void onStart(){
			Set<String> keyset;
			keyset = contentdata.snp_gene.keySet();
			Iterator<String> itr = keyset.iterator();
			
			while (itr.hasNext()) {
				String snp  = itr.next();
				String gene = contentdata.snp_gene.get(snp);
				this.addSubBehaviour(new remoteAccessHUGO(snp,gene));
			}
		}
		
		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}	
	
	/* Agent action: Query DB  */
	class remoteAccessHUGO extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private Process proc;
		private BufferedReader buffer;
		private HUGO hugo;
		private String snpid;
		private String geneSymbol;

		public remoteAccessHUGO (String s,String g) {
			this.snpid = s;
			this.geneSymbol = g;
			hugo = hugodata.createHUGOInstance();
		}

		public Process setProc(String path, String service, String input) {
			try {
				return Runtime.getRuntime().exec("perl " + path + service + " " + input);
			} catch (IOException e) {
				System.out.println("Null process");
				e.printStackTrace();
				return null;
			}
		}

		public void action() {
			System.out.println("Agent " + getLocalName() +" executing request for "+geneSymbol+"...");
			this.proc = setProc(path, service, geneSymbol);
			this.buffer = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			try {
				String line = null;
				String[] split = null;

				do {
					line = buffer.readLine();
					
					if(line !=null && !line.equals("")){
						split = line.split("\\t");
						hugo.setHgncId(split[0]);
						hugo.setGeneSymbol(split[1]);
						hugo.setHgGeneName(split[2]);
						hugo.setGeneSynonyms(split[8]);
						hugo.setLocusType(split[4]);
						hugo.setLocusGroup(split[5]);
						if(split[24].isEmpty()){ hugo.setGeneFamilyTag(null); }
						else{ hugo.setGeneFamilyTag(split[24]); };
						hugo.setGeneFamily(split[25]);
						
						hugo.setSpecialistDBLinks(split[20]);
						hugo.setLocusSpecDB(split[31]);
							
						try { hugo.setEnzymeId(split[16]); }
						catch(Exception e){ hugo.setEnzymeId("null"); }
								
						try { hugo.setEntrezId(split[17]); }
						catch(Exception e){ hugo.setEntrezId("null"); }

						try { hugo.setEnsemblId(split[18]); }
						catch(Exception e){ hugo.setEnsemblId("null"); }
								
						try { hugo.setPubMedIds(split[22]); }
						catch(Exception e){ hugo.setPubMedIds("null"); }
								
						try { hugo.setRefSeqIds(split[23]); }
						catch(Exception e){ hugo.setRefSeqIds("null"); }
								
						try { hugo.setCCDSIds(split[29]); }
						catch(Exception e){ hugo.setCCDSIds("null"); }
								
						try { hugo.setVegaIds(split[30]); }
						catch(Exception e){ hugo.setVegaIds("null"); }
								
						try { hugo.setOmimId(split[33]); }
						catch(Exception e){ hugo.setOmimId("null"); }
								
						try { hugo.setUniProtId(split[35]); } 
						catch(Exception e){ hugo.setUniProtId("null"); }
								
						try { hugo.setUcscId(split[37]); }
						catch(Exception e){ hugo.setUcscId("null"); }
							
						try { hugo.setMouseGdbId(split[38]); }
						catch(Exception e){ hugo.setMouseGdbId("null"); }
							
						try { hugo.setRatGdbId(split[39]); }
						catch(Exception e){ hugo.setRatGdbId("null"); }
							
						hugo.setGeneSymbol(geneSymbol);
						hugo.setPolymorphismCode(snpid);


						synchronized(hugodata){
							hugodata.setHUGOList(hugo);
						}
					} else { System.out.println("No response from HGNC server"); }
				} while (buffer.ready());
			} catch (IOException e) {
				System.out.println("Exception 1");
				e.printStackTrace();
			}
		}
	}
}