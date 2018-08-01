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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import BioData_Models.PharmGKB;

public class PharmgkbAgent extends DBagent {
	private static final long serialVersionUID = 1L;

	/* Attributes */
	String path;
	String userdir;
	String service;
	Hashtable<Info, Info> hash;
	BioData pharmdata;

	/* Constructor */
	public PharmgkbAgent() {
		super();
		slash = System.getProperty("file.separator");
		os    = System.getProperty("os.name");		
		System.out.println("OS is "+os);
		
		this.setDBname("dbpharmgkb");
		this.setInformation("pharmgkb");
		this.userdir = System.getProperty("user.dir");

		if(os.equals("Linux")){
			this.path = this.userdir+slash+"divergenomenrich"+slash+"scripts-PharmGKB"+slash;
		}else{ this.path = this.userdir+slash+"scripts-pharmgkb"+slash; }

		this.service = "search.pl";
		this.hash = new Hashtable<Info, Info>();
		this.pharmdata = new BioData("pharmgkb");
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

		public waitRequest() { MessageTemplate.MatchPerformative(ACLMessage.REQUEST); }
		
		public void action() {
			ACLMessage msg = myAgent.receive(simplerequest_template);
			if (msg != null) {
				sender = msg.getSender();
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					BioData contentdata = (BioData)msg.getContentObject();
					addBehaviour(new PharmgkbAction(sender,contentdata));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	} 

	/* Agent action: access DB -> access DB remote; send reply  */
	class PharmgkbAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
	
		public PharmgkbAction(AID pa,BioData bd){
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessPharmgkb(bd), STATE_A);
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
				msg.setContentObject(pharmdata);
				myAgent.send(msg);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	/* Agent action: parallel access to DB -> remote Access DB  */
	class accessPharmgkb extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		BioData contentdata;

		public accessPharmgkb (BioData bd) {
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
				this.addSubBehaviour(new remoteAccessPharmgkb(snp,gene));
			}
		}
		
		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}	
	
	/* Agent action: Query DB  */
	class remoteAccessPharmgkb extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private Process proc;
		private BufferedReader buffer;
		private PharmGKB ph;
		private String snpid;
		private String genesymbol;
		private String key;
		private String type;
		private String value;

		public remoteAccessPharmgkb (String s,String g) {
			this.snpid = s;
			this.genesymbol = g;
			ph = pharmdata.createPharmGKBInstance();
		}

		public Process setProc(String path, String service, String input) {
			try { return Runtime.getRuntime().exec("perl " + path + service + " " + input); }
			catch (IOException e) {
				System.out.println("Processo nulo");
				e.printStackTrace();
				return null;
			}
		}

		public void action() {
			System.out.println("Agent " + getLocalName() +" executing request for "+genesymbol+"...");
			this.proc = setProc(path, service, genesymbol);
			this.buffer = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			try {
				String line = null;
				String[] split = null;
				Info info_key = null;
				Info info_value = null;

				do {
					line = buffer.readLine();
					if(line !=null){
						if (!line.equals("") && !line.equals("No response from server")) {
							split = line.split(", ");
							type = split[1];

							if (!type.equals("Literature")) {
								if (type.equals("Gene")) {
									key = split[0];
									int aux = 0;
									while (!split[aux].contains("/gene/")) { aux++; }
									value = split[aux + 1];
								} else if (type.equals("Golden Path Position")) {
									key = split[3].split("/")[2];
									value = split[0];
								} else {
									key = split[0];
									value = split[2];
								}

								info_key = new Info(genesymbol, key);
								info_value = new Info(type, value);
								synchronized(hash){ hash.put(info_key, info_value); }
								
								if (type.equals("Drug")){ ph.setDrugItem(value); }
								if (type.equals("Disease")){ ph.setDiseaseItem(value); }
								if (type.equals("Gene")){ ph.setGenexItem(value); }
								if (type.equals("Pathway")){ ph.setPathwayItem(value); }
							}
						} else if (line.equals("No response from server")) {
							System.out.println("No response from PGKB server");
						}
					}	
				} while (buffer.ready());

				ph.setGenesymbol(genesymbol);
				ph.setPolymorphismCode(snpid);
				synchronized(pharmdata){ pharmdata.setPharmgkbList(ph); }

			} catch (IOException e) {
				System.out.println("Exception 1");
				e.printStackTrace();
			}
		}
	}
	
	/* INFO CLASS */
	class Info {
		private String inf1;
		private String inf2;

		public Info(String inf1, String inf2) {
			setInf1(inf1);
			setInf2(inf2);
		}

		public String toString() {
			return "[" + inf1 + " , " + inf2 + "]";
		}

		public String getInf1() {
			return inf1;
		}

		public void setInf1(String inf1) {
			this.inf1 = inf1;
		}

		public String getInf2() {
			return inf2;
		}

		public void setInf2(String inf2) {
			this.inf2 = inf2;
		}
	}
}