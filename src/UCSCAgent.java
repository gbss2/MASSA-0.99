import java.io.IOException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import BioData_Models.UCSC;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.MessageTemplate;

public class UCSCAgent extends DBagent{
	private static final long serialVersionUID = 1L;

	/* Attributes */
	public String sn = "genome-mysql.cse.ucsc.edu";
	public String md = "hg19";
	public String ucscUser = "genome";
	public String ucscKey = "";
	BioData ucscdata;
	
	/* Constructor */
	public UCSCAgent() {
		this.setDBname("ucsc");
		this.setInformation("ucsc");
		this.dbConnect(sn, md, ucscUser, ucscKey);
		this.ucscdata = new BioData("ucsc");
    }

	/* Agent setup */
	protected void setup() {
		System.out.println("Agent "+getLocalName()+" started.");
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
					addBehaviour(new UCSCAction(sender,contentdata.getSnpRsList()));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	}

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */	
	class UCSCAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";

		public UCSCAction(AID pa, String[] rslist){
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessUCSC(rslist), STATE_A);
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
				msg.setContentObject(ucscdata);
				myAgent.send(msg);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}	

	/* Agent action: parallel access to DB -> mysqlRemoteUCSCqueryThree  */
	class accessUCSC extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;
		private String[] snprslist;

		public accessUCSC (String[] rsl) {
			super(WHEN_ALL);
			snprslist = rsl;
		}

		public void onStart(){
			for (int i = 0; i < snprslist.length; i++) {
				this.addSubBehaviour(new mysqlRemoteUCSCquery(snprslist[i]));
			}
		}

		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			dbDisconnect();
			return super.onEnd();
		}
	}

	/* Agent action: Query DB  */
	class mysqlRemoteUCSCquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private String snpid;
		private UCSC ucscobject;

		public mysqlRemoteUCSCquery(String s) { snpid = s; }

		public void action() {
			ucscobject = QueryUCSC1(snpid);
			ucscobject.setPolymorphismCode(snpid.substring(2));
			synchronized(ucscdata){ ucscdata.setUCSCList(ucscobject); }
		}
	}


	/* LIST OF QUERIES */
	
	/* Agent action: Query SNP  */
	public UCSC QueryUCSC1(String snpid) {
		BioData	bioobjects = new BioData("ucsc");
		UCSC ucscobject = bioobjects.createUCSCInstance();
		
		String strsql = "SELECT snp135.strand, " +
						"snp135.refUCSC, " +
						"snp135.observed, " +
						"snp135.class, " +
						"snp135.func, " +
						"snp135CodingDbSnp.codons, " +
						"snp135CodingDbSnp.peptides " +
						"FROM snp135 " +
						"LEFT JOIN snp135CodingDbSnp ON snp135.name = snp135CodingDbSnp.name " +
						"WHERE " +
						"snp135.name = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,snpid);
			ResultSet rs = pstmt.executeQuery();	
		    
			while (rs.next()) {
				String strand = rs.getString("strand");
				ucscobject.setStrand(strand);
				String refUCSC = rs.getString("refUCSC");
				ucscobject.setRefUCSC(refUCSC);
				String obsGen = rs.getString("observed");
				ucscobject.setObsGen(obsGen);
				String ucscClass = rs.getString("class");
				ucscobject.setUcscClass(ucscClass);
				String ucscFunc = rs.getString("func");
				ucscobject.setUcscFunc(ucscFunc);
			}
			
			rs.close();
			pstmt.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}

		return ucscobject;
	}
}