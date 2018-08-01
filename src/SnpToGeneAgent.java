import java.net.URL;
import java.net.MalformedURLException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.MessageTemplate;

public class SnpToGeneAgent extends DBagent {
	private static final long serialVersionUID = 1L;

	BioData geneidnamelist;

	public SnpToGeneAgent() {
		super();
		this.setDBname("dbsnp");
		this.setInformation("snp to gene");
		this.geneidnamelist = new BioData();
	}
	
	protected void setup() {
		System.out.println("Agent " + getLocalName() + " started.");
		this.register();
		addBehaviour(new waitRequest());
	}
	
	class waitRequest extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		private MessageTemplate simplerequest_template;
		private AID sender;

		public waitRequest() { MessageTemplate.MatchPerformative(ACLMessage.REQUEST); }
		
		public void action() {
			ACLMessage msg = myAgent.receive(simplerequest_template);
			if (msg != null) {
				sender = msg.getSender();
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+sender.getName());
				try {
					BioData contentdata = (BioData)msg.getContentObject();
					addBehaviour(new snpToGeneAction(contentdata.getSnpIdList(),sender));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	} 
	
	class snpToGeneAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public AID partneragent;
		public int[] snpidlist;
		
		public snpToGeneAction(int[] sl, AID pa){
			partneragent = pa;
			snpidlist = sl;
			
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessDBsnpEutils(snpidlist), STATE_A);
			this.registerLastState(new SendReply(partneragent,geneidnamelist), STATE_B);
			this.registerDefaultTransition(STATE_A,STATE_B);
		}
		
		public int onEnd() {
			System.out.println("Agent " + getLocalName() +" finished task.");
			return super.onEnd();
		}
	}

	class accessDBsnpEutils extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		private int[] snpidlist;

		public accessDBsnpEutils (int[] sl) {
			super(WHEN_ALL);
			this.snpidlist = sl;
		}
		
		public void onStart(){
			for (int i = 0; i < snpidlist.length; i++) {
				this.addSubBehaviour(new remoteAccessDBsnpEutils(snpidlist[i]));
			}
		}
		
		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" parallel behaviour finished.");
			return super.onEnd();
		}

		class remoteAccessDBsnpEutils extends OneShotBehaviour {
			private static final long serialVersionUID = 1L;

			private int snpid;
			GeneFilter genefilter;

			public remoteAccessDBsnpEutils(int l) {
				snpid = l;
		   	 	this.genefilter = new GeneFilter();
			}

			public void action() {
				BioData genex = new BioData();
				genefilter.setId(snpid);
				genefilter.readFLT();
				
				if(genefilter.polTable[1] != null){
					genex.gene.setSnpid(Integer.toString(snpid));
					genex.gene.setGeneSymbol(genefilter.polTable[1]);
					genex.gene.setGeneName(genefilter.polTable[1]);
					synchronized(geneidnamelist){ geneidnamelist.setGeneList(genex.gene); }
				}
			}
		}
	}			
	
	class SendReply extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private AID     msgreceiver;
		private String  msgperformative;
		private BioData msgcontent;
		private ACLMessage msg;

		public SendReply(AID p, BioData d) {
			msgreceiver     = p;
			msgcontent      = new BioData();
			msgcontent      = d;
			msgperformative = "INFORM";
		}
		
		public void action() {
			try {
				System.out.println("... sending "+msgperformative+" to agent: \""+msgreceiver.getName());
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(msgreceiver);
				msg.setLanguage("English");
				msg.setContentObject(msgcontent);
				myAgent.send(msg);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
}

class GeneFilter {
	int id;
	String[] polTable;
	
	public GeneFilter(){ polTable = new String[2]; }

	public void readFLT() {
		try {
			URL url = new URL(makeURL(id, "FLT"));
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			String line;

			do {
				line = reader.readLine();
				if (line.startsWith("LOC")) {
					String[] aux = new String[6];
					aux = line.split(" | ");
					String gene_name = aux[2];
					String locus_id = aux[4];
					int pos;
					pos = locus_id.indexOf('=');
					locus_id = locus_id.substring(pos + 1);
					polTable[0] = locus_id;
					polTable[1] = gene_name;
				}
			} while (reader.ready());
			reader.close();
		} catch (MalformedURLException e) {
			System.err.println("ERROR 1: ERROR READING FLT URL: MalformedURLException - readFLT()");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("ERROR 2: IOException - readFLT()");
			e.printStackTrace();
		}
	}
	
	public void setId(int id) {
		this.id = id;
		polTable[0] = "" + this.id;
	}

	public String makeURL(int id, String report) {
		return 
		"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=snp&id=" + id + "&report=" + report;
	}
}